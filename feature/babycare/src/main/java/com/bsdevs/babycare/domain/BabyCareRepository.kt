package com.bsdevs.babycare.domain

import com.bsdevs.babycare.network.DailyLogDto
import com.bsdevs.babycare.network.FeedingDto
import com.bsdevs.babycare.network.NappyChangeDto
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.data.NetworkScreenData
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

data class RepositoryFetchResult(
    val nextAnchorDate: LocalDate?, // 📅 Tracks where the next 5-day block should start
    val hasMoreData: Boolean
)

interface BabyCareRepository {
    val cachedDays: StateFlow<List<DailyLogDto>>

    suspend fun loadInitialData(userId: String, pageSize: Int): RepositoryFetchResult
    suspend fun refreshData(userId: String, pageSize: Int): RepositoryFetchResult
    suspend fun loadMoreData(userId: String, pageSize: Int): RepositoryFetchResult
    suspend fun saveActivityEvent(userId: String, date: String, event: UnifiedEventDto)
    suspend fun getFeedingEventById(userId: String, activityId: String): UnifiedEventDto?
    suspend fun getNappyEventById(userId: String, activityId: String): UnifiedEventDto?
    suspend fun updateActivityEvent(userId: String, date: String, eventId: String, updatedEvent: UnifiedEventDto)
    suspend fun deleteActivityEvent(userId: String, date: String, eventId: String)
    suspend fun fetchScreenLayout(screenName: String): List<NetworkScreenData>
}