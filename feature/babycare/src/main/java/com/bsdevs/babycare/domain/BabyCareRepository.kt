package com.bsdevs.babycare.domain

import com.bsdevs.babycare.network.DailyLogDto
import com.bsdevs.babycare.network.UnifiedEventDto
import kotlinx.coroutines.flow.StateFlow
import java.time.YearMonth

data class RepositoryFetchResult(
    val nextAnchorMonth: YearMonth?,
    val hasMoreData: Boolean
)

interface BabyCareRepository {
    val cachedDays: StateFlow<List<DailyLogDto>>
    val measurements: StateFlow<List<UnifiedEventDto>>

    suspend fun loadInitialData(userId: String, pageSize: Int, forceRefresh: Boolean = false): RepositoryFetchResult
    suspend fun refreshData(userId: String, pageSize: Int): RepositoryFetchResult
    suspend fun loadMoreData(userId: String, pageSize: Int): RepositoryFetchResult
    suspend fun saveActivityEvent(userId: String, date: String, event: UnifiedEventDto)
    suspend fun getFeedingEventById(userId: String, activityId: String): UnifiedEventDto?
    suspend fun getNappyEventById(userId: String, activityId: String): UnifiedEventDto?
    suspend fun getMeasurementEventById(userId: String, activityId: String): UnifiedEventDto?
    suspend fun updateActivityEvent(userId: String, date: String, eventId: String, updatedEvent: UnifiedEventDto)
    suspend fun deleteActivityEvent(userId: String, date: String, eventId: String)
    fun clearCache()
}
