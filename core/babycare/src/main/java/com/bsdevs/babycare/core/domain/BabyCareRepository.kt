package com.bsdevs.babycare.core.domain

import com.bsdevs.network.dto.DailyLogDto
import com.bsdevs.network.dto.BabyEvent
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.YearMonth

data class RepositoryFetchResult(
    val nextAnchorMonth: YearMonth?,
    val hasMoreData: Boolean
)

interface BabyCareRepository {
    val cachedDays: StateFlow<List<DailyLogDto>>
    val measurements: StateFlow<List<BabyEvent.Measurement>>
    val vaccinations: StateFlow<List<BabyEvent.Vaccination>>

    suspend fun loadInitialData(userId: String, pageSize: Int, forceRefresh: Boolean = false): RepositoryFetchResult
    suspend fun refreshData(userId: String, pageSize: Int): RepositoryFetchResult
    suspend fun loadMoreData(userId: String, pageSize: Int): RepositoryFetchResult
    suspend fun saveActivityEvent(userId: String, date: String, event: BabyEvent)
    suspend fun getFeedingEventById(userId: String, activityId: String): BabyEvent.Feeding?
    suspend fun getNappyEventById(userId: String, activityId: String): BabyEvent.Nappy?
    suspend fun getTemperatureEventById(userId: String, activityId: String): BabyEvent.Temperature?
    suspend fun getMeasurementEventById(userId: String, activityId: String): BabyEvent.Measurement?
    suspend fun getVaccinationEventById(userId: String, activityId: String): BabyEvent.Vaccination?
    suspend fun updateActivityEvent(userId: String, date: String, eventId: String, updatedEvent: BabyEvent)
    suspend fun deleteActivityEvent(userId: String, date: String, eventId: String)
    fun getCurrentDate(): LocalDate
    fun clearCache()
}
