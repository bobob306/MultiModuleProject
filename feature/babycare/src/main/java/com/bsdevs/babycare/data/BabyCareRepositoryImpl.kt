package com.bsdevs.babycare.data.repository

import android.util.Log
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.domain.RepositoryFetchResult
import com.bsdevs.babycare.network.DailyLogDto
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.babycare.network.BabyCareFirestoreService
import com.bsdevs.common.DispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BabyCareRepositoryImpl @Inject constructor(
    private val apiService: BabyCareFirestoreService,
    private val dispatchers: DispatcherProvider,
) : BabyCareRepository {

    private val _cachedDays = MutableStateFlow<List<DailyLogDto>>(emptyList())
    override val cachedDays: StateFlow<List<DailyLogDto>> = _cachedDays.asStateFlow()

    private var currentAnchorMonth: YearMonth? = null

    override suspend fun loadInitialData(userId: String, pageSize: Int): RepositoryFetchResult = withContext(dispatchers.io) {
        // Optimization: If we already have data in memory, don't hit the network unless force refreshed
        if (_cachedDays.value.isNotEmpty()) {
            return@withContext RepositoryFetchResult(
                nextAnchorMonth = currentAnchorMonth,
                hasMoreData = currentAnchorMonth != null
            )
        }
        
        try {
            val latestMonthId = apiService.getLatestMonthId(userId)

            if (latestMonthId == null) {
                _cachedDays.value = emptyList()
                currentAnchorMonth = null
                return@withContext RepositoryFetchResult(nextAnchorMonth = null, hasMoreData = false)
            }

            val monthlyDays = fetchMonthFromService(userId, latestMonthId)
            _cachedDays.value = monthlyDays.sortedByDescending { it.date }

            val nextMonthId = apiService.getMonthIdBefore(userId, latestMonthId)
            currentAnchorMonth = nextMonthId?.let { parseYearMonth(it) }

            RepositoryFetchResult(
                nextAnchorMonth = currentAnchorMonth,
                hasMoreData = currentAnchorMonth != null
            )
        } catch (e: Exception) {
            Log.e("BABYCARE_REPO", "Error loading initial data", e)
            throw e // Rethrow so ViewModel can catch it
        }
    }

    private suspend fun fetchMonthFromService(userId: String, monthId: String): List<DailyLogDto> {
        val data = apiService.fetchMonthDocument(userId, monthId) ?: return emptyList()
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
        return UnifiedEventDto(
            id = eventMap["id"] as? String ?: "",
            type = eventMap["type"] as? String ?: "",
            time = eventMap["time"] as? String ?: "",
            dateTimeString = eventMap["dateTimeString"] as? String ?: "",
            comment = eventMap["comment"] as? String,
            nappyType = eventMap["nappyType"] as? String,
            mainFeedingSide = eventMap["mainFeedingSide"] as? String,
            leftDuration = (eventMap["leftDuration"] as? Number)?.toLong() ?: 0L,
            rightDuration = (eventMap["rightDuration"] as? Number)?.toLong() ?: 0L,
            totalDuration = (eventMap["totalDuration"] as? Number)?.toLong() ?: 0L,
            bottleAmountMl = (eventMap["bottleAmountMl"] as? Number)?.toInt(),
            temperature = (eventMap["temperature"] as? Number)?.toDouble()
        )
    }

    override suspend fun refreshData(userId: String, pageSize: Int): RepositoryFetchResult = withContext(dispatchers.io) {
        _cachedDays.value = emptyList()
        currentAnchorMonth = null
        loadInitialData(userId, pageSize)
    }

    override suspend fun loadMoreData(userId: String, pageSize: Int): RepositoryFetchResult = withContext(dispatchers.io) {
        val anchor = currentAnchorMonth ?: return@withContext RepositoryFetchResult(null, false)
        val monthId = formatYearMonth(anchor)

        val newMonthlyDays = fetchMonthFromService(userId, monthId)
        _cachedDays.value = (_cachedDays.value + newMonthlyDays).sortedByDescending { it.date }

        val nextMonthId = apiService.getMonthIdBefore(userId, monthId)
        currentAnchorMonth = nextMonthId?.let { parseYearMonth(it) }

        RepositoryFetchResult(currentAnchorMonth, currentAnchorMonth != null)
    }

    override suspend fun saveActivityEvent(userId: String, date: String, event: UnifiedEventDto) = withContext(dispatchers.io) {
        val monthId = extractMonthString(date)
        apiService.saveEvent(userId, monthId, date, toMap(event))
        updateLocalCacheWithNewEvent(date, userId, event)
    }

    override suspend fun updateActivityEvent(userId: String, date: String, eventId: String, updatedEvent: UnifiedEventDto) = withContext(dispatchers.io) {
        val monthId = extractMonthString(date)
        apiService.updateEvent(userId, monthId, date, eventId, toMap(updatedEvent))
        updateLocalCacheWithModifiedEvent(date, eventId, updatedEvent)
    }

    override suspend fun deleteActivityEvent(userId: String, date: String, eventId: String) = withContext(dispatchers.io) {
        val monthId = extractMonthString(date)
        apiService.deleteEvent(userId, monthId, date, eventId)
        updateLocalCacheWithDeletedEvent(date, eventId)
    }

    override suspend fun getFeedingEventById(userId: String, activityId: String): UnifiedEventDto? = withContext(dispatchers.io) {
        val cached = _cachedDays.value.flatMap { it.events }.firstOrNull { it.id == activityId }
        if (cached != null) return@withContext cached
        
        // 📡 Network Fallback: Scan all months for the event ID
        apiService.getAllMonthIds(userId).firstNotNullOfOrNull { monthId ->
            fetchMonthFromService(userId, monthId).flatMap { it.events }.firstOrNull { it.id == activityId }
        }
    }

    override suspend fun getNappyEventById(userId: String, activityId: String) = getFeedingEventById(userId, activityId)

    private fun formatYearMonth(ym: YearMonth) = String.format(java.util.Locale.ROOT, "%04d-%02d", ym.year, ym.monthValue)
    private fun extractMonthString(date: String) = date.substring(0, 7)

    private fun toMap(e: UnifiedEventDto) = mapOf(
        "id" to e.id, "type" to e.type, "time" to e.time, "dateTimeString" to e.dateTimeString,
        "comment" to e.comment, "nappyType" to e.nappyType, "mainFeedingSide" to e.mainFeedingSide,
        "leftDuration" to e.leftDuration, "rightDuration" to e.rightDuration, "totalDuration" to e.totalDuration,
        "bottleAmountMl" to e.bottleAmountMl, "temperature" to e.temperature
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

    private fun updateLocalCacheWithModifiedEvent(date: String, eventId: String, updated: UnifiedEventDto) {
        val list = _cachedDays.value.toMutableList()
        val index = list.indexOfFirst { it.date == date }
        if (index != -1) {
            list[index] = list[index].copy(events = list[index].events.map { if (it.id == eventId) updated else it })
            _cachedDays.value = list
        }
    }

    private fun updateLocalCacheWithDeletedEvent(date: String, eventId: String) {
        val list = _cachedDays.value.toMutableList()
        val index = list.indexOfFirst { it.date == date }
        if (index != -1) {
            list[index] = list[index].copy(events = list[index].events.filterNot { it.id == eventId })
            _cachedDays.value = list
        }
    }
}
