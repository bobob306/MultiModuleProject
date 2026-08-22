package com.bsdevs.babycare.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.babycare.domain.BabyCareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FeedingGraphUiState(
    val hourlyCounts: List<HourlyFeedingCount> = emptyList(),
    val totalFeedsInCache: Int = 0
)

data class HourlyFeedingCount(
    val hour: Int,         // 0 to 23
    val displayLabel: String, // e.g., "02:00"
    val count: Int
)

@HiltViewModel
class BabyGraphViewModel @Inject constructor(
    private val repository: BabyCareRepository,
) : ViewModel() {

    val uiState: StateFlow<FeedingGraphUiState> = repository.cachedDays
        .map { dailyLogs ->
            // 1. Flatten all events across all cached days into a single list
            val allEvents = dailyLogs.flatMap { it.events }

            // 2. Filter out anything that isn't a feeding event
            val feedingEvents = allEvents.filter { it.type == "FEEDING" }

            // 3. Group the feedings by their extracted hour of the day
            val countsByHour = feedingEvents.groupBy { event ->
                extractHourFromTime(event.time)
            }.mapValues { it.value.size }

            // 4. Generate a comprehensive 24-element list ensuring empty hours still show 0 counts
            val hourlyGraphData = (0..23).map { hour ->
                HourlyFeedingCount(
                    hour = hour,
                    // Simple string formatting to avoid Java Time API compilation issues
                    displayLabel = String.format("%02d:00", hour),
                    count = countsByHour[hour] ?: 0
                )
            }

            FeedingGraphUiState(
                hourlyCounts = hourlyGraphData,
                totalFeedsInCache = feedingEvents.size
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeedingGraphUiState()
        )

    /**
     * Extracts the hour directly from standard "HH:mm" strings cleanly without relying on LocalTime
     */
    private fun extractHourFromTime(timeString: String): Int {
        return try {
            // Split "22:31" into ["22", "31"] and grab the first element
            val parts = timeString.split(":")
            if (parts.isNotEmpty()) {
                parts[0].toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            Log.e("GRAPH_ERROR", "Failed parsing time string: $timeString", e)
            0
        }
    }
}
