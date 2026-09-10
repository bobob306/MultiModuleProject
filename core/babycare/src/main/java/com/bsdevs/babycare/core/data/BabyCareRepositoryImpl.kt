package com.bsdevs.babycare.core.data

import android.util.Log
import com.bsdevs.babycare.core.domain.BabyCareRepository
import com.bsdevs.babycare.core.domain.RepositoryFetchResult
import com.bsdevs.network.dto.DailyLogDto
import com.bsdevs.network.dto.BabyEvent
import com.bsdevs.babycare.core.network.BabyCareFirestoreService
import com.bsdevs.common.TimeProvider
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.SyncManager
import com.bsdevs.data.Syncable
import com.bsdevs.data.local.dao.BabyEventDao
import com.bsdevs.data.local.entities.BabyEventEntity
import com.bsdevs.data.repository.Clearable
import com.bsdevs.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    syncManager: SyncManager
) : BabyCareRepository, Clearable, Syncable {

    init {
        userRepository.registerClearable(this)
        syncManager.registerSyncable(this)
    }

    private val _cachedDays = MutableStateFlow<List<DailyLogDto>>(emptyList())
    override val cachedDays: StateFlow<List<DailyLogDto>> = _cachedDays.asStateFlow()

    private val _measurements = MutableStateFlow<List<BabyEvent.Measurement>>(emptyList())
    override val measurements: StateFlow<List<BabyEvent.Measurement>> = _measurements.asStateFlow()

    private val _vaccinations = MutableStateFlow<List<BabyEvent.Vaccination>>(emptyList())
    override val vaccinations: StateFlow<List<BabyEvent.Vaccination>> = _vaccinations.asStateFlow()

    private var currentAnchorMonth: YearMonth? = null

    override suspend fun sync() {
        val userId = userRepository.userProfile.value?.id ?: return
        val pending = babyEventDao.getPendingSync()
        if (pending.isEmpty()) return
        
        Log.d("BABYCARE_REPO", "Syncing ${pending.size} pending events")
        for (entity in pending) {
            try {
                if (entity.isDeleted) {
                    syncDelete(userId, entity.date, entity.id, entity.event.type)
                } else {
                    syncSave(userId, entity.date, entity.event)
                }
            } catch (e: Exception) {
                Log.e("BABYCARE_REPO", "Sync failed for event ${entity.id}", e)
            }
        }
    }

    private suspend fun syncSave(userId: String, date: String, event: BabyEvent) {
        when (event) {
            is BabyEvent.Measurement -> apiService.saveMeasurement(userId, event.id, toMap(event))
            is BabyEvent.Vaccination -> apiService.saveVaccination(userId, event.id, toMap(event))
            else -> {
                val monthId = extractMonthString(date)
                apiService.saveEvent(userId, monthId, date, toMap(event))
            }
        }
        val babyId = getAuthorizedBabyId(userId) ?: return
        babyEventDao.insertEvents(listOf(BabyEventEntity(event.id, babyId, date, event, isPendingSync = false)))
        
        // 🧠 Update memory to reflect synced status
        val syncedEvent = event.withPendingSync(false)
        when (syncedEvent) {
            is BabyEvent.Measurement -> {
                _measurements.value = _measurements.value.map { if (it.id == event.id) syncedEvent else it }
            }
            is BabyEvent.Vaccination -> {
                _vaccinations.value = _vaccinations.value.map { if (it.id == event.id) syncedEvent else it }
            }
            else -> {
                updateLocalCacheWithModifiedEvent(date, userId, event.id, syncedEvent)
            }
        }
    }

    private suspend fun syncDelete(userId: String, date: String, eventId: String, type: String) {
        when (type) {
            "MEASUREMENT" -> apiService.deleteMeasurement(userId, eventId)
            "VACCINATION" -> apiService.deleteVaccination(userId, eventId)
            else -> {
                val monthId = extractMonthString(date)
                apiService.deleteEvent(userId, monthId, date, eventId)
            }
        }
        babyEventDao.deleteById(eventId)
        
        // 🧠 Ensure memory is clean (case: sync() retrying a deletion)
        if (type == "MEASUREMENT") {
            _measurements.value = _measurements.value.filterNot { it.id == eventId }
        } else if (type == "VACCINATION") {
            _vaccinations.value = _vaccinations.value.filterNot { it.id == eventId }
        } else {
            updateLocalCacheWithDeletedEvent(date, eventId)
        }
    }

    override suspend fun loadInitialData(userId: String, pageSize: Int, forceRefresh: Boolean): RepositoryFetchResult = withContext(dispatchers.io) {
        val babyId = getAuthorizedBabyId(userId)
        
        // 1. 📂 Offline-first: Load from Room immediately if memory is empty
        if (_cachedDays.value.isEmpty()) {
            babyId?.let { id ->
                val localEvents = babyEventDao.getEvents(id).first()
                localEvents.takeIf { it.isNotEmpty() }?.let { events ->
                    val dailyLogs = events.groupBy { it.date }.asSequence().map { (date, eventsForDate) ->
                        DailyLogDto(date, userId, eventsForDate.map { it.event.withPendingSync(it.isPendingSync) })
                    }.sortedByDescending { it.date }.toList()
                    _cachedDays.value = dailyLogs
                }
            }
        }

        // 2. 🛡️ Optimization: If we already have data in memory AND we have already performed a network fetch 
        // in this session (anchor is set), don't hit the network unless force refreshed.
        if (!forceRefresh && _cachedDays.value.isNotEmpty() && currentAnchorMonth != null) {
            return@withContext RepositoryFetchResult(
                nextAnchorMonth = currentAnchorMonth,
                hasMoreData = true
            )
        }
        
        try {
            // 3. 🌐 Network Sync: Fetch the latest pointers from Firestore
            val latestMonthId = apiService.getLatestMonthId(userId, forceRefresh)

            // 1. Fetch all measurements once (Separate collection)
            val measurementList = apiService.fetchAllMeasurements(userId).map { parseBabyEvent(it) as BabyEvent.Measurement }
            _measurements.value = measurementList

            // 2. Fetch all vaccinations once (Separate collection)
            val vaccinationList = apiService.fetchAllVaccinations(userId).map { parseBabyEvent(it) as BabyEvent.Vaccination }
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
            if ((today.dayOfMonth <= 8) && (latestMonthId == currentMonthId)) {
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
                    it is BabyEvent.Feeding || it is BabyEvent.Nappy
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
                hasMoreData = currentAnchorMonth != null,
            )
        } catch (_: Exception) {
            Log.e("BABYCARE_REPO", "Error loading initial data")
            if (forceRefresh) {
                // Return current state instead of throwing if we are just refreshing
                RepositoryFetchResult(
                    nextAnchorMonth = currentAnchorMonth,
                    hasMoreData = currentAnchorMonth != null,
                )
            } else {
                throw Exception("Error loading initial data")
            }
        }
    }

    private suspend fun fetchMonthFromService(userId: String, monthId: String, forceRefresh: Boolean = false): List<DailyLogDto> {
        val data = apiService.fetchMonthDocument(userId, monthId, forceRefresh) ?: return emptyList()
        val daysMap = (data["days"] as? Map<*, *>) ?: emptyMap<Any?, Any?>()

        return daysMap.map { (dateString, eventsArray) ->
            DailyLogDto(
                date = dateString as String,
                userId = userId,
                events = (eventsArray as List<*>).map { item ->
                    @Suppress("UNCHECKED_CAST")
                    parseBabyEvent(item as Map<String, Any?>)
                }
            )
        }
    }

    private fun parseYearMonth(monthId: String): YearMonth {
        val parts = monthId.split("-")
        return YearMonth.of(parts[0].toInt(), parts[1].toInt())
    }

    private fun parseBabyEvent(eventMap: Map<String, Any?>): BabyEvent {
        val type = eventMap["type"] as? String ?: ""
        val rawDateTime = eventMap["dateTimeString"] as? String ?: ""
        // Normalize: replace "YYYY-MM-DD HH:mm" with "YYYY-MM-DDTHH:mm" for better string sorting
        val dateTimeString = if (rawDateTime.contains(" ") && !rawDateTime.contains("T")) {
            rawDateTime.replace(" ", "T")
        } else {
            rawDateTime
        }

        val time = (eventMap["time"] as? String) ?: run {
            try {
                // Try parsing as UTC ISO 8601 first
                OffsetDateTime.parse(dateTimeString)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
            } catch (_: Exception) {
                // Fallback to legacy extraction logic (handles "YYYY-MM-DD HH:mm" or ISO)
                dateTimeString.substringAfter("T", dateTimeString.substringAfter(" ", "")).take(5)
            }
        }
        
        val id = eventMap["id"] as? String ?: ""
        val comment = eventMap["comment"] as? String
        val isPendingSync = eventMap["isPendingSync"] as? Boolean ?: false

        val isNappyLegacy = type == "Wet" || type == "Dirty" || type == "Both"

        return when {
            type == "FEEDING" -> BabyEvent.Feeding(
                id = id, time = time, dateTimeString = dateTimeString, comment = comment, isPendingSync = isPendingSync,
                mainFeedingSide = eventMap["mainFeedingSide"] as? String,
                leftDuration = (eventMap["leftDuration"] as? Number)?.toLong() ?: 0L,
                rightDuration = (eventMap["rightDuration"] as? Number)?.toLong() ?: 0L,
                totalDuration = (eventMap["totalDuration"] as? Number)?.toLong() ?: 0L,
                bottleAmountMl = (eventMap["bottleAmountMl"] as? Number)?.toInt(),
                hasVitaminD = eventMap["hasVitaminD"] as? Boolean,
                predictionGapMinutes = (eventMap["predictionGapMinutes"] as? Number)?.toLong()
            )
            type == "NAPPY" || isNappyLegacy -> BabyEvent.Nappy(
                id = id, time = time, dateTimeString = dateTimeString, comment = comment, isPendingSync = isPendingSync,
                nappyType = if (isNappyLegacy) type else eventMap["nappyType"] as? String
            )
            type == "TEMPERATURE" -> BabyEvent.Temperature(
                id = id, time = time, dateTimeString = dateTimeString, comment = comment, isPendingSync = isPendingSync,
                temperature = (eventMap["temperature"] as? Number)?.toDouble()
            )
            type == "MEASUREMENT" -> BabyEvent.Measurement(
                id = id, time = time, dateTimeString = dateTimeString, comment = comment, isPendingSync = isPendingSync,
                height = (eventMap["height"] as? Number)?.toDouble(),
                weight = (eventMap["weight"] as? Number)?.toDouble(),
                headCircumference = (eventMap["headCircumference"] as? Number)?.toDouble() ?: (eventMap["head_circumference"] as? Number)?.toDouble(),
                isMedical = eventMap["isMedical"] as? Boolean
            )
            type == "VACCINATION" -> BabyEvent.Vaccination(
                id = id, time = time, dateTimeString = dateTimeString, comment = comment, isPendingSync = isPendingSync,
                vaccinationNames = (eventMap["vaccinationNames"] as? List<*>)?.filterIsInstance<String>(),
                location = eventMap["location"] as? String,
                seriesId = eventMap["seriesId"] as? String
            )
            type == "VITAMIN_D" -> BabyEvent.VitaminD(
                id = id, time = time, dateTimeString = dateTimeString, comment = comment, isPendingSync = isPendingSync
            )
            else -> BabyEvent.Unknown(
                id = id, time = time, dateTimeString = dateTimeString, comment = comment, isPendingSync = isPendingSync,
                unknownType = type
            )
        }
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
        val anchor = currentAnchorMonth ?: return@withContext RepositoryFetchResult(nextAnchorMonth = null, hasMoreData = false)
        val monthId = formatYearMonth(anchor)

        val newMonthlyDays = fetchMonthFromService(userId, monthId)
        val measurementsOnly = _measurements.value
        val vaccinationsOnly = _vaccinations.value
        
        mergeAndSortCachedDays(newMonthlyDays, measurementsOnly, vaccinationsOnly, userId)

        val nextMonthId = apiService.getMonthIdBefore(userId, monthId)
        currentAnchorMonth = nextMonthId?.let { parseYearMonth(it) }

        RepositoryFetchResult(currentAnchorMonth, currentAnchorMonth != null)
    }

    override suspend fun saveActivityEvent(userId: String, date: String, event: BabyEvent) = withContext(dispatchers.io) {
        val pendingEvent = event.withPendingSync(true)
        // 1. 📂 Update Local DB with pending flag
        getAuthorizedBabyId(userId)?.let { babyId ->
            babyEventDao.insertEvents(listOf(BabyEventEntity(event.id, babyId, date, pendingEvent, isPendingSync = true)))
        }

        // 2. 🧠 Update in-memory StateFlows IMMEDIATELY for UI responsiveness
        when (pendingEvent) {
            is BabyEvent.Measurement -> {
                _measurements.value = (_measurements.value + pendingEvent).sortedByDescending { it.dateTimeString }
            }
            is BabyEvent.Vaccination -> {
                _vaccinations.value = (_vaccinations.value + pendingEvent).sortedByDescending { it.dateTimeString }
            }
            else -> {
                updateLocalCacheWithNewEvent(date, userId, pendingEvent)
            }
        }

        // 3. 🌐 Non-blocking network sync
        try {
            syncSave(userId, date, event)
        } catch (e: Exception) {
            Log.e("BABYCARE_REPO", "Network sync failed for saved event, will retry later", e)
        }
        Unit
    }

    override suspend fun updateActivityEvent(userId: String, date: String, eventId: String, updatedEvent: BabyEvent) = withContext(dispatchers.io) {
        val pendingEvent = updatedEvent.withPendingSync(true)
        // 1. 📂 Update Local DB with pending flag
        getAuthorizedBabyId(userId)?.let { babyId ->
            babyEventDao.insertEvents(listOf(BabyEventEntity(eventId, babyId, date, pendingEvent, isPendingSync = true)))
        }

        // 2. 🧠 Update in-memory StateFlows IMMEDIATELY
        when (pendingEvent) {
            is BabyEvent.Measurement -> {
                _measurements.value = _measurements.value.map { if (it.id == eventId) pendingEvent else it }
            }
            is BabyEvent.Vaccination -> {
                _vaccinations.value = _vaccinations.value.map { if (it.id == eventId) pendingEvent else it }
            }
            else -> {
                updateLocalCacheWithModifiedEvent(date, userId, eventId, pendingEvent)
            }
        }

        try {
            syncSave(userId, date, updatedEvent)
        } catch (e: Exception) {
            Log.e("BABYCARE_REPO", "Network sync failed for updated event", e)
        }
        Unit
    }

    override suspend fun deleteActivityEvent(userId: String, date: String, eventId: String) = withContext(dispatchers.io) {
        // 1. 📂 Mark as deleted in Local DB
        val type = _cachedDays.value.asSequence().flatMap { it.events }.firstOrNull { it.id == eventId }?.type
            ?: _measurements.value.firstOrNull { it.id == eventId }?.type
            ?: _vaccinations.value.firstOrNull { it.id == eventId }?.type
            ?: ""

        babyEventDao.markDeleted(eventId)

        // 2. 🧠 Update in-memory StateFlows IMMEDIATELY
        updateLocalCacheWithDeletedEvent(date, eventId)

        try {
            syncDelete(userId, date, eventId, type)
        } catch (e: Exception) {
            Log.e("BABYCARE_REPO", "Network sync failed for deleted event", e)
        }
        Unit
    }

    override fun getCurrentDate(): LocalDate = timeProvider.currentLocalDate()

    override suspend fun getFeedingEventById(userId: String, activityId: String): BabyEvent.Feeding? = 
        getActivityEventById(userId, activityId) as? BabyEvent.Feeding

    override suspend fun getNappyEventById(userId: String, activityId: String) = 
        getActivityEventById(userId, activityId) as? BabyEvent.Nappy

    override suspend fun getTemperatureEventById(userId: String, activityId: String) = 
        getActivityEventById(userId, activityId) as? BabyEvent.Temperature

    private suspend fun getActivityEventById(userId: String, activityId: String): BabyEvent? = withContext(dispatchers.io) {
        val cached = _cachedDays.value.asSequence().flatMap { it.events }.firstOrNull { it.id == activityId }
        if (cached != null) return@withContext cached
        
        apiService.getAllMonthIds(userId).asSequence().firstNotNullOfOrNull { monthId ->
            fetchMonthFromService(userId, monthId).asSequence().flatMap { it.events }.firstOrNull { it.id == activityId }
        }
    }

    override suspend fun getMeasurementEventById(userId: String, activityId: String): BabyEvent.Measurement? = withContext(dispatchers.io) {
        val cached = _measurements.value.firstOrNull { it.id == activityId }
        if (cached != null) return@withContext cached

        apiService.fetchAllMeasurements(userId).map { parseBabyEvent(it) as BabyEvent.Measurement }
            .firstOrNull { it.id == activityId } ?: getActivityEventById(userId, activityId) as? BabyEvent.Measurement
    }

    override suspend fun getVaccinationEventById(userId: String, activityId: String): BabyEvent.Vaccination? = withContext(dispatchers.io) {
        val cached = _vaccinations.value.firstOrNull { it.id == activityId }
        if (cached != null) return@withContext cached
        
        apiService.fetchAllVaccinations(userId).map { parseBabyEvent(it) as BabyEvent.Vaccination }
            .firstOrNull { it.id == activityId }
    }

    private suspend fun mergeAndSortCachedDays(
        monthlyDays: List<DailyLogDto>,
        measurements: List<BabyEvent.Measurement>,
        vaccinations: List<BabyEvent.Vaccination>,
        userId: String
    ) {
        val extraEventsByDate = (measurements + vaccinations).groupBy { it.dateTimeString.substringBefore("T").substringBefore(" ") }
        
        // 🧬 Merge with existing cache to avoid clearing months that weren't part of this fetch
        val currentData = _cachedDays.value
        val monthsFetched = monthlyDays.map { extractMonthString(it.date) }.distinct().toSet()
        
        // Keep days from months we DID NOT just fetch from network
        val preservedDays = currentData.filter { day ->
            extractMonthString(day.date) !in monthsFetched
        }
        
        val mergedDays = (monthlyDays + preservedDays).toMutableList()
        
        // Merge in measurements and vaccinations (which are fetched globally)
        extraEventsByDate.forEach { (date, extraEvents) ->
            val existingDayIndex = mergedDays.indexOfFirst { it.date == date }
            if (existingDayIndex != -1) {
                val existingDay = mergedDays[existingDayIndex]
                val cleanEvents = existingDay.events.filter { it !is BabyEvent.Measurement && it !is BabyEvent.Vaccination }
                mergedDays[existingDayIndex] = existingDay.copy(events = cleanEvents + extraEvents)
            } else {
                mergedDays.add(DailyLogDto(date, userId, extraEvents))
            }
        }

        val sortedDays = mergedDays.sortedByDescending { it.date }
        _cachedDays.value = sortedDays
        
        val babyId = getAuthorizedBabyId(userId)
        if (babyId != null) {
            // Only update Room for dates that were touched in this fetch (network or global)
            // This prevents overwriting pending syncs in months we didn't fetch.
            val datesToUpdate = (monthlyDays.map { it.date } + extraEventsByDate.keys).toSet()
            
            val entities = sortedDays.filter { it.date in datesToUpdate }.flatMap { day ->
                day.events.map { event ->
                    BabyEventEntity(event.id, babyId, day.date, event, isPendingSync = event.isPendingSync)
                }
            }
            if (entities.isNotEmpty()) {
                babyEventDao.insertEvents(entities)
            }
        }
    }

    private fun formatYearMonth(ym: YearMonth) = String.format(Locale.ROOT, "%04d-%02d", ym.year, ym.monthValue)
    private fun extractMonthString(date: String) = date.substring(0, 7)

    private fun toMap(e: BabyEvent): Map<String, Any?> {
        val base = mutableMapOf<String, Any?>(
            "id" to e.id,
            "type" to e.type,
            "time" to e.time,
            "dateTimeString" to e.dateTimeString,
            "comment" to e.comment
        )
        when (e) {
            is BabyEvent.Feeding -> {
                base.putAll(mapOf(
                    "mainFeedingSide" to e.mainFeedingSide,
                    "leftDuration" to e.leftDuration.takeIf { it > 0 },
                    "rightDuration" to e.rightDuration.takeIf { it > 0 },
                    "totalDuration" to e.totalDuration.takeIf { it > 0 },
                    "bottleAmountMl" to e.bottleAmountMl,
                    "hasVitaminD" to e.hasVitaminD,
                    "predictionGapMinutes" to e.predictionGapMinutes
                ))
            }
            is BabyEvent.Nappy -> base["nappyType"] = e.nappyType
            is BabyEvent.Temperature -> base["temperature"] = e.temperature
            is BabyEvent.Measurement -> {
                base.putAll(mapOf(
                    "height" to e.height,
                    "weight" to e.weight,
                    "headCircumference" to e.headCircumference,
                    "isMedical" to e.isMedical
                ))
            }
            is BabyEvent.Vaccination -> {
                base.putAll(mapOf(
                    "vaccinationNames" to e.vaccinationNames,
                    "location" to e.location,
                    "seriesId" to e.seriesId
                ))
            }
            is BabyEvent.VitaminD -> {}
            is BabyEvent.Unknown -> base["type"] = e.unknownType
        }
        return base.filterValues { it != null }
    }

    private fun updateLocalCacheWithNewEvent(date: String, userId: String, event: BabyEvent) {
        val list = _cachedDays.value.toMutableList()
        val index = list.indexOfFirst { it.date == date }
        if (index != -1) {
            list[index] = list[index].copy(events = list[index].events + event)
        } else {
            list.add(DailyLogDto(date, userId, listOf(event)))
        }
        _cachedDays.value = list.sortedByDescending { it.date }
    }

    private fun updateLocalCacheWithModifiedEvent(date: String, userId: String, eventId: String, updated: BabyEvent) {
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
