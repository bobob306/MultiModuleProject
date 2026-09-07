package com.bsdevs.babycare.data.repository

import android.util.Log
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.domain.RepositoryFetchResult
import com.bsdevs.network.dto.DailyLogDto
import com.bsdevs.network.dto.UnifiedEventDto
import com.bsdevs.babycare.network.BabyCareFirestoreService
import com.bsdevs.babycare.presentation.common.TimeProvider
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.SyncManager
import com.bsdevs.data.Syncable
import com.bsdevs.data.local.dao.BabyEventDao
import com.bsdevs.data.local.entities.BabyEventEntity
import com.bsdevs.data.repository.Clearable
import com.bsdevs.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BabyCareRepositoryImpl @Inject constructor(
    private val apiService: BabyCareFirestoreService,
    private val userRepository: UserRepository,
    private val dispatchers: DispatcherProvider,
    private val timeProvider: TimeProvider,
    private val babyEventDao: BabyEventDao,
    private val syncManager: SyncManager
) : BabyCareRepository, Clearable, Syncable {

    private val repositoryScope = CoroutineScope(dispatchers.io + SupervisorJob())

    init {
        userRepository.registerClearable(this)
        syncManager.registerSyncable(this)
    }

    private val _cachedDays = MutableStateFlow<List<DailyLogDto>>(emptyList())
    override val cachedDays: StateFlow<List<DailyLogDto>> = _cachedDays.asStateFlow()

    private val _measurements = MutableStateFlow<List<UnifiedEventDto>>(emptyList())
    override val measurements: StateFlow<List<UnifiedEventDto>> = _measurements.asStateFlow()

    private val _vaccinations = MutableStateFlow<List<UnifiedEventDto>>(emptyList())
    override val vaccinations: StateFlow<List<UnifiedEventDto>> = _vaccinations.asStateFlow()

    private var currentAnchorMonth: YearMonth? = null

    override suspend fun sync() {
        val userId = userRepository.userProfile.value?.id ?: return
        val pending = babyEventDao.getPendingSync()
        if (pending.isEmpty()) return
        
        Log.d("BABYCARE_REPO", "Syncing ${pending.size} pending events")
        for (entity in pending) {
            try {
                if (entity.isDeleted) {
                    deleteActivityEvent(userId, entity.date, entity.id)
                } else {
                    saveActivityEvent(userId, entity.date, entity.event)
                }
                babyEventDao.insertEvents(listOf(entity.copy(isPendingSync = false)))
            } catch (e: Exception) {
                Log.e("BABYCARE_REPO", "Sync failed for event ${entity.id}", e)
            }
        }
    }

    override suspend fun loadInitialData(userId: String, pageSize: Int, forceRefresh: Boolean): RepositoryFetchResult = withContext(dispatchers.io) {
        val babyId = getAuthorizedBabyId(userId)
        
        // Load from DB first
        if (!forceRefresh && _cachedDays.value.isEmpty() && babyId != null) {
            val localEvents = babyEventDao.getEvents(babyId).first()
            if (localEvents.isNotEmpty()) {
                val dailyLogs = localEvents.groupBy { it.date }.map { (date, events) ->
                    DailyLogDto(date, userId, events.map { it.event })
                }.sortedByDescending { it.date }
                _cachedDays.value = dailyLogs
            }
        }

        // Optimization: If we already have data in memory, don't hit the network unless force refreshed
        if (!forceRefresh && _cachedDays.value.isNotEmpty()) {
            return@withContext RepositoryFetchResult(
                nextAnchorMonth = currentAnchorMonth,
                hasMoreData = currentAnchorMonth != null
            )
        }
        
        try {
            val latestMonthId = apiService.getLatestMonthId(userId, forceRefresh)

            // 1. Fetch all measurements once (Separate collection)
            val measurementList = apiService.fetchAllMeasurements(userId).map { parseUnifiedEvent(it) }
            _measurements.value = measurementList

            // 2. Fetch all vaccinations once (Separate collection)
            val vaccinationList = apiService.fetchAllVaccinations(userId).map { parseUnifiedEvent(it) }
            _vaccinations.value = vaccinationList

            if (latestMonthId == null) {
                _cachedDays.value = emptyList()
                mergeAndSortCachedDays(emptyList(), measurementList, vaccinationList, userId)
                currentAnchorMonth = null
                return@withContext RepositoryFetchResult(nextAnchorMonth = null, hasMoreData = false)
            }

            val today = timeProvider.currentLocalDate()
            val currentMonthId = formatYearMonth(YearMonth.from(today))
            val monthsToFetch = mutableListOf(latestMonthId)

            // 🚀 Special Case: In the first 8 days of a month, if we have data for the current month,
            // also pull the previous month to avoid an empty-looking screen.
            if (today.dayOfMonth <= 8 && latestMonthId == currentMonthId) {
                apiService.getMonthIdBefore(userId, latestMonthId)?.let { prevMonthId ->
                    monthsToFetch.add(prevMonthId)
                }
            }

            val allMonthlyDays = monthsToFetch.flatMap { mId ->
                fetchMonthFromService(userId, mId, forceRefresh)
            }

            // Check if we have enough "Primary" activity (Feeds/Nappies) to fill a screen
            val primaryEventCount = allMonthlyDays.sumOf { day ->
                day.events.count {
                    it.type == "FEEDING" || it.type == "NAPPY" || it.type == "Both" || it.type == "Wet" || it.type == "Dirty"
                }
            }

            var finalMonthlyDays = allMonthlyDays
            var lastFetchedId = monthsToFetch.last()

            // 🚀 Special Case: In the first 8 days of a month, OR if the current month is very sparse,
            // also pull the previous month to avoid an empty-looking screen.
            if ((today.dayOfMonth <= 8 || primaryEventCount < 5) && 
                latestMonthId == currentMonthId && 
                monthsToFetch.size == 1
            ) {
                apiService.getMonthIdBefore(userId, latestMonthId)?.let { prevMonthId ->
                    val prevMonthDays = fetchMonthFromService(userId, prevMonthId, forceRefresh)
                    finalMonthlyDays = allMonthlyDays + prevMonthDays
                    lastFetchedId = prevMonthId
                }
            }

            mergeAndSortCachedDays(finalMonthlyDays, measurementList, vaccinationList, userId)
            
            val nextMonthId = try { 
                apiService.getMonthIdBefore(userId, lastFetchedId) 
            } catch (e: Exception) { 
                null 
            }
            currentAnchorMonth = nextMonthId?.let { parseYearMonth(it) }

            RepositoryFetchResult(
                nextAnchorMonth = currentAnchorMonth,
                hasMoreData = currentAnchorMonth != null
            )
        } catch (e: Exception) {
            Log.e("BABYCARE_REPO", "Error loading initial data", e)
            if (forceRefresh) {
                // Return current state instead of throwing if we are just refreshing
                RepositoryFetchResult(
                    nextAnchorMonth = currentAnchorMonth,
                    hasMoreData = currentAnchorMonth != null
                )
            } else {
                throw e
            }
        }
    }

    private suspend fun fetchMonthFromService(userId: String, monthId: String, forceRefresh: Boolean = false): List<DailyLogDto> {
        val data = apiService.fetchMonthDocument(userId, monthId, forceRefresh) ?: return emptyList()
        val daysMap = data["days"] as? Map<*, *> ?: emptyMap<Any?, Any?>()

        return daysMap.map { (dateString, eventsArray) ->
            DailyLogDto(
                date = dateString as String,
                userId = userId,
                events = (eventsArray as List<*>).map { parseUnifiedEvent(it as Map<String, Any?>) }
            )
        }
    }

    private fun parseYearMonth(monthId: String): YearMonth {
        val parts = monthId.split("-")
        return YearMonth.of(parts[0].toInt(), parts[1].toInt())
    }

    private fun parseUnifiedEvent(eventMap: Map<String, Any?>): UnifiedEventDto {
        val rawDateTime = eventMap["dateTimeString"] as? String ?: ""
        // Normalize: replace "YYYY-MM-DD HH:mm" with "YYYY-MM-DDTHH:mm" for better string sorting
        val dateTimeString = if (rawDateTime.contains(" ") && !rawDateTime.contains("T")) {
            rawDateTime.replace(" ", "T")
        } else {
            rawDateTime
        }

        val time = eventMap["time"] as? String ?: run {
            try {
                // Try parsing as UTC ISO 8601 first
                java.time.OffsetDateTime.parse(dateTimeString)
                    .atZoneSameInstant(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            } catch (_: Exception) {
                // Fallback to legacy extraction logic (handles "YYYY-MM-DD HH:mm" or ISO)
                dateTimeString.substringAfter("T", dateTimeString.substringAfter(" ", "")).take(5)
            }
        }
        return UnifiedEventDto(
            id = eventMap["id"] as? String ?: "",
            type = eventMap["type"] as? String ?: "",
            time = time,
            dateTimeString = dateTimeString,
            comment = eventMap["comment"] as? String,
            nappyType = eventMap["nappyType"] as? String,
            mainFeedingSide = eventMap["mainFeedingSide"] as? String,
            leftDuration = (eventMap["leftDuration"] as? Number)?.toLong() ?: 0L,
            rightDuration = (eventMap["rightDuration"] as? Number)?.toLong() ?: 0L,
            totalDuration = (eventMap["totalDuration"] as? Number)?.toLong() ?: 0L,
            bottleAmountMl = (eventMap["bottleAmountMl"] as? Number)?.toInt(),
            temperature = (eventMap["temperature"] as? Number)?.toDouble(),
            height = (eventMap["height"] as? Number)?.toDouble(),
            weight = (eventMap["weight"] as? Number)?.toDouble(),
            headCircumference = (eventMap["headCircumference"] as? Number)?.toDouble() ?: (eventMap["head_circumference"] as? Number)?.toDouble(),
            isMedical = eventMap["isMedical"] as? Boolean,
            vaccinationNames = (eventMap["vaccinationNames"] as? List<*>)?.filterIsInstance<String>(),
            location = eventMap["location"] as? String,
            seriesId = eventMap["seriesId"] as? String,
            hasVitaminD = eventMap["hasVitaminD"] as? Boolean,
            predictionGapMinutes = (eventMap["predictionGapMinutes"] as? Number)?.toLong()
        )
    }

    private suspend fun getAuthorizedBabyId(id: String): String? {
        val user = userRepository.userProfile.value ?: userRepository.getUser(id)
        val authorizedIds = (user?.babyIds ?: emptyList()) + listOfNotNull(user?.babyId)

        return when {
            authorizedIds.contains(id) -> id
            id == user?.id -> authorizedIds.firstOrNull()
            else -> null
        }
    }

    override suspend fun refreshData(userId: String, pageSize: Int): RepositoryFetchResult = withContext(dispatchers.io) {
        currentAnchorMonth = null
        loadInitialData(userId, pageSize, forceRefresh = true)
    }

    override suspend fun loadMoreData(userId: String, pageSize: Int): RepositoryFetchResult = withContext(dispatchers.io) {
        val anchor = currentAnchorMonth ?: return@withContext RepositoryFetchResult(null, false)
        val monthId = formatYearMonth(anchor)

        val newMonthlyDays = fetchMonthFromService(userId, monthId)
        val currentDays = _cachedDays.value
        val measurementsOnly = _measurements.value
        val vaccinationsOnly = _vaccinations.value
        
        val allMonthlyDays = (currentDays.map { day -> 
            day.copy(events = day.events.filter { it.type != "MEASUREMENT" && it.type != "VACCINATION" }) 
        } + newMonthlyDays).groupBy { it.date }.map { (date, logs) ->
            DailyLogDto(date, userId, logs.flatMap { it.events })
        }

        mergeAndSortCachedDays(allMonthlyDays, measurementsOnly, vaccinationsOnly, userId)

        val nextMonthId = apiService.getMonthIdBefore(userId, monthId)
        currentAnchorMonth = nextMonthId?.let { parseYearMonth(it) }

        RepositoryFetchResult(currentAnchorMonth, currentAnchorMonth != null)
    }

    override suspend fun saveActivityEvent(userId: String, date: String, event: UnifiedEventDto) = withContext(dispatchers.io) {
        val babyId = getAuthorizedBabyId(userId)
        if (babyId != null) {
            babyEventDao.insertEvents(listOf(BabyEventEntity(event.id, babyId, date, event, isPendingSync = true)))
        }

        try {
            if (event.type == "MEASUREMENT") {
                apiService.saveMeasurement(userId, event.id, toMap(event))
                _measurements.value = (_measurements.value + event).sortedByDescending { it.dateTimeString }
            } else if (event.type == "VACCINATION") {
                apiService.saveVaccination(userId, event.id, toMap(event))
                _vaccinations.value = (_vaccinations.value + event).sortedByDescending { it.dateTimeString }
            } else {
                val monthId = extractMonthString(date)
                apiService.saveEvent(userId, monthId, date, toMap(event))
            }
            if (babyId != null) {
                babyEventDao.insertEvents(listOf(BabyEventEntity(event.id, babyId, date, event, isPendingSync = false)))
            }
        } catch (e: Exception) {
            Log.e("BABYCARE_REPO", "Failed to sync saved event", e)
        }
        updateLocalCacheWithNewEvent(date, userId, event)
    }

    override suspend fun updateActivityEvent(userId: String, date: String, eventId: String, updatedEvent: UnifiedEventDto) = withContext(dispatchers.io) {
        val babyId = getAuthorizedBabyId(userId)
        if (babyId != null) {
            babyEventDao.insertEvents(listOf(BabyEventEntity(eventId, babyId, date, updatedEvent, isPendingSync = true)))
        }

        try {
            val specializedTarget = when (updatedEvent.type) {
                "MEASUREMENT" -> "MEASUREMENT"
                "VACCINATION" -> "VACCINATION"
                else -> null
            }

            if (specializedTarget != null) {
                if (specializedTarget == "MEASUREMENT") {
                    apiService.updateMeasurement(userId, eventId, toMap(updatedEvent))
                    _measurements.value = _measurements.value.map { if (it.id == eventId) updatedEvent else it }
                } else {
                    apiService.updateVaccination(userId, eventId, toMap(updatedEvent))
                    _vaccinations.value = _vaccinations.value.map { if (it.id == eventId) updatedEvent else it }
                }
            } else {
                val monthId = extractMonthString(date)
                apiService.updateEvent(userId, monthId, date, eventId, toMap(updatedEvent))
            }

            if (babyId != null) {
                babyEventDao.insertEvents(listOf(BabyEventEntity(eventId, babyId, date, updatedEvent, isPendingSync = false)))
            }
        } catch (e: Exception) {
            Log.e("BABYCARE_REPO", "Failed to sync updated event", e)
        }
        updateLocalCacheWithModifiedEvent(date, userId, eventId, updatedEvent)
    }

    override suspend fun deleteActivityEvent(userId: String, date: String, eventId: String) = withContext(dispatchers.io) {
        babyEventDao.markDeleted(eventId)

        try {
            val cachedEvent = _cachedDays.value.flatMap { it.events }.firstOrNull { it.id == eventId }
                ?: _measurements.value.firstOrNull { it.id == eventId }
                ?: _vaccinations.value.firstOrNull { it.id == eventId }

            if (cachedEvent?.type == "MEASUREMENT") {
                apiService.deleteMeasurement(userId, eventId)
                _measurements.value = _measurements.value.filterNot { it.id == eventId }
            } else if (cachedEvent?.type == "VACCINATION") {
                apiService.deleteVaccination(userId, eventId)
                _vaccinations.value = _vaccinations.value.filterNot { it.id == eventId }
            } else {
                val monthId = extractMonthString(date)
                apiService.deleteEvent(userId, monthId, date, eventId)
            }
            babyEventDao.deleteById(eventId)
        } catch (e: Exception) {
            Log.e("BABYCARE_REPO", "Failed to sync deleted event", e)
        }
        updateLocalCacheWithDeletedEvent(date, eventId)
    }

    override fun getCurrentDate(): LocalDate = timeProvider.currentLocalDate()

    override suspend fun getFeedingEventById(userId: String, activityId: String): UnifiedEventDto? = withContext(dispatchers.io) {
        val cached = _cachedDays.value.flatMap { it.events }.firstOrNull { it.id == activityId }
        if (cached != null) return@withContext cached
        
        apiService.getAllMonthIds(userId).firstNotNullOfOrNull { monthId ->
            fetchMonthFromService(userId, monthId).flatMap { it.events }.firstOrNull { it.id == activityId }
        }
    }

    override suspend fun getNappyEventById(userId: String, activityId: String) = getFeedingEventById(userId, activityId)

    override suspend fun getTemperatureEventById(userId: String, activityId: String) = getFeedingEventById(userId, activityId)

    override suspend fun getMeasurementEventById(userId: String, activityId: String): UnifiedEventDto? = withContext(dispatchers.io) {
        val cached = _measurements.value.firstOrNull { it.id == activityId }
        if (cached != null) return@withContext cached

        apiService.fetchAllMeasurements(userId).map { parseUnifiedEvent(it) }
            .firstOrNull { it.id == activityId } ?: getFeedingEventById(userId, activityId)
    }

    override suspend fun getVaccinationEventById(userId: String, activityId: String): UnifiedEventDto? = withContext(dispatchers.io) {
        val cached = _vaccinations.value.firstOrNull { it.id == activityId }
        if (cached != null) return@withContext cached
        
        apiService.fetchAllVaccinations(userId).map { parseUnifiedEvent(it) }
            .firstOrNull { it.id == activityId }
    }

    private suspend fun mergeAndSortCachedDays(
        monthlyDays: List<DailyLogDto>,
        measurements: List<UnifiedEventDto>,
        vaccinations: List<UnifiedEventDto>,
        userId: String
    ) {
        val extraEventsByDate = (measurements + vaccinations).groupBy { it.dateTimeString.substringBefore("T").substringBefore(" ") }
        val mergedDays = monthlyDays.toMutableList()
        
        extraEventsByDate.forEach { (date, extraEvents) ->
            val existingDayIndex = mergedDays.indexOfFirst { it.date == date }
            if (existingDayIndex != -1) {
                val existingDay = mergedDays[existingDayIndex]
                val cleanEvents = existingDay.events.filter { it.type != "MEASUREMENT" && it.type != "VACCINATION" }
                mergedDays[existingDayIndex] = existingDay.copy(events = cleanEvents + extraEvents)
            } else {
                mergedDays.add(DailyLogDto(date, userId, extraEvents))
            }
        }

        val sortedDays = mergedDays.sortedByDescending { it.date }
        _cachedDays.value = sortedDays
        
        val babyId = getAuthorizedBabyId(userId)
        if (babyId != null) {
            val entities = sortedDays.flatMap { day ->
                day.events.map { event ->
                    BabyEventEntity(event.id, babyId, day.date, event, isPendingSync = false)
                }
            }
            babyEventDao.insertEvents(entities)
        }
    }

    private fun formatYearMonth(ym: YearMonth) = String.format(Locale.ROOT, "%04d-%02d", ym.year, ym.monthValue)
    private fun extractMonthString(date: String) = date.substring(0, 7)

    private fun toMap(e: UnifiedEventDto) = mapOf(
        "id" to e.id, "type" to e.type, "time" to e.time, "dateTimeString" to e.dateTimeString,
        "comment" to e.comment, "nappyType" to e.nappyType, "mainFeedingSide" to e.mainFeedingSide,
        "leftDuration" to e.leftDuration, "rightDuration" to e.rightDuration, "totalDuration" to e.totalDuration,
        "bottleAmountMl" to e.bottleAmountMl, "temperature" to e.temperature,
        "height" to e.height, "weight" to e.weight, "headCircumference" to e.headCircumference, "isMedical" to e.isMedical,
        "vaccinationNames" to e.vaccinationNames, "location" to e.location, "seriesId" to e.seriesId,
        "hasVitaminD" to e.hasVitaminD, "predictionGapMinutes" to e.predictionGapMinutes
    )

    private fun updateLocalCacheWithNewEvent(date: String, userId: String, event: UnifiedEventDto) {
        val list = _cachedDays.value.toMutableList()
        val index = list.indexOfFirst { it.date == date }
        if (index != -1) {
            list[index] = list[index].copy(events = list[index].events + event)
        } else {
            list.add(DailyLogDto(date, userId, listOf(event)))
        }
        _cachedDays.value = list.sortedByDescending { it.date }
    }

    private fun updateLocalCacheWithModifiedEvent(date: String, userId: String, eventId: String, updated: UnifiedEventDto) {
        val list = _cachedDays.value.toMutableList()
        val index = list.indexOfFirst { it.date == date }
        if (index != -1) {
            list[index] = list[index].copy(events = list[index].events.map { if (it.id == eventId) updated else it })
        } else {
            list.add(DailyLogDto(date, userId, listOf(updated)))
        }
        _cachedDays.value = list.sortedByDescending { it.date }
    }

    private fun updateLocalCacheWithDeletedEvent(date: String, eventId: String) {
        val list = _cachedDays.value.toMutableList()
        val index = list.indexOfFirst { it.date == date }
        if (index != -1) {
            list[index] = list[index].copy(events = list[index].events.filterNot { it.id == eventId })
            _cachedDays.value = list
        }
    }

    override fun clearCache() {
        _cachedDays.value = emptyList()
        _measurements.value = emptyList()
        _vaccinations.value = emptyList()
        currentAnchorMonth = null
    }
}

private fun formatYearMonth(ym: YearMonth) = String.format(Locale.ROOT, "%04d-%02d", ym.year, ym.monthValue)
private fun extractMonthString(date: String) = date.substring(0, 7)

private fun toMap(e: UnifiedEventDto) = mapOf(
    "id" to e.id, "type" to e.type, "time" to e.time, "dateTimeString" to e.dateTimeString,
    "comment" to e.comment, "nappyType" to e.nappyType, "mainFeedingSide" to e.mainFeedingSide,
    "leftDuration" to e.leftDuration, "rightDuration" to e.rightDuration, "totalDuration" to e.totalDuration,
    "bottleAmountMl" to e.bottleAmountMl, "temperature" to e.temperature,
    "height" to e.height, "weight" to e.weight, "headCircumference" to e.headCircumference, "isMedical" to e.isMedical,
    "vaccinationNames" to e.vaccinationNames, "location" to e.location, "seriesId" to e.seriesId,
    "hasVitaminD" to e.hasVitaminD, "predictionGapMinutes" to e.predictionGapMinutes
)

