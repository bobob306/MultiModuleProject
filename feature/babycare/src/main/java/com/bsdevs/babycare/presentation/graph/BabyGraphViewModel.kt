package com.bsdevs.babycare.presentation.graph

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.common.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BabyGraphViewModel @Inject constructor(
    private val repository: BabyCareRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    val uiState: StateFlow<FeedingGraphUiState> = repository.cachedDays
        .map { dailyLogs ->
            withContext(dispatchers.default) {
                val allEvents = dailyLogs.flatMap { it.events }
                val feedingEvents = allEvents.filter { it.type == "FEEDING" }

                val countsByHour = feedingEvents.groupBy { event ->
                    extractHourFromTime(event.time)
                }.mapValues { it.value.size }

                val hourlyGraphData = (0..23).map { hour ->
                    HourlyFeedingCount(
                        hour = hour,
                        displayLabel = String.format(Locale.getDefault(), "%02d:00", hour),
                        count = countsByHour[hour] ?: 0
                    )
                }

                val analysis = calculateFeedingGaps(feedingEvents)

                // 🌟 1. Compute the daily average gaps for the new chart
                val dailyGapsData = calculateDailyAverageGaps(feedingEvents)

                FeedingGraphUiState(
                    hourlyCounts = hourlyGraphData,
                    totalFeedsInCache = feedingEvents.size,
                    analysisResult = analysis,
                    dailyAverageGaps = dailyGapsData // 🌟 2. Assign to view state
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeedingGraphUiState()
        )

    private fun calculateDailyAverageGaps(events: List<UnifiedEventDto>): List<DailyAverageGap> {
        val onlyFeedings = events.filter { it.type == "FEEDING" && it.dateTimeString.isNotEmpty() }
        if (onlyFeedings.size < 2) return emptyList()

        val sortedFeeds = onlyFeedings.sortedBy { it.dateTimeString }

        data class DatedGap(val date: String, val gapMinutes: Long)
        val gapMeasurements = mutableListOf<DatedGap>()

        for (i in 0 until sortedFeeds.size - 1) {
            val currentFeed = sortedFeeds[i]
            val nextFeed = sortedFeeds[i + 1]

            val currentMinutes = parseToTotalMinutes(currentFeed.dateTimeString)
            val nextMinutes = parseToTotalMinutes(nextFeed.dateTimeString)

            if (currentMinutes == -1L || nextMinutes == -1L) continue
            val gapMinutes = nextMinutes - currentMinutes

            if (gapMinutes in 15..720) {
                val targetDate = nextFeed.dateTimeString.split(" ").getOrNull(0) ?: continue
                gapMeasurements.add(DatedGap(targetDate, gapMinutes))
            }
        }

        // Step A: First compute standard single-day raw averages chronologically
        val rawDailyAverages = gapMeasurements
            .groupBy { it.date }
            .map { (date, gapsForDay) ->
                date to gapsForDay.map { it.gapMinutes }.average().toInt()
            }
            .sortedBy { it.first } // Ensure historical sequence

        // Step B: Loop over sorted items to calculate rolling windows
        return rawDailyAverages.mapIndexed { index, (dateStr, dayAvg) ->
            // To build a true 14-point window, look backwards up to 13 steps + current index step
            val startIdx = (index - 13).coerceAtLeast(0)
            val windowItems = rawDailyAverages.subList(startIdx, index + 1)

            // Optional boundary: Only calculate if we have a robust history sample
            // If you prefer to show a line immediately from Day 1, remove this `if` restriction
            val rollingAvg = if (windowItems.size >= 3) {
                windowItems.map { it.second }.average().toInt()
            } else {
                null // Not enough history depth yet
            }

            DailyAverageGap(
                dateString = dateStr,
                averageGapMinutes = dayAvg,
                rolling14DayAverageMinutes = rollingAvg
            )
        }
    }

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

    private fun calculateFeedingGaps(events: List<UnifiedEventDto>): FeedingAnalysisResult? {
        // 1. ISOLATE: Filter out everything that isn't a feeding event FIRST
        val onlyFeedings = events.filter { it.type == "FEEDING" && it.dateTimeString.isNotEmpty() }

        // Safety check: We need at least two feeding events total to analyze intervals
        if (onlyFeedings.size < 2) {
            Log.w("ANALYSIS_DEBUG", "⚠️ Not enough feeds to compute gaps. Total found: ${onlyFeedings.size}")
            return null
        }

        // 2. SORT: Chronologically order from OLDEST to NEWEST (Ascending text sort fits YYYY-MM-DD flawlessly)
        val sortedFeeds = onlyFeedings.sortedBy { it.dateTimeString }
        Log.d("ANALYSIS_DEBUG", "🚀 Processing ${sortedFeeds.size} chronological feeds for interval gaps")

        data class FeedGapPair(val feedDurationMinutes: Long, val gapMinutes: Long)
        val pairs = mutableListOf<FeedGapPair>()

        // 3. MEASURE: Loop strictly through consecutive feeding events
        for (i in 0 until sortedFeeds.size - 1) {
            val currentFeed = sortedFeeds[i]
            val nextFeed = sortedFeeds[i + 1]

            val currentMinutes = parseToTotalMinutes(currentFeed.dateTimeString)
            val nextMinutes = parseToTotalMinutes(nextFeed.dateTimeString)

            // Skip if either date fails to parse cleanly into absolute minutes
            if (currentMinutes == -1L || nextMinutes == -1L) continue

            val gapMinutes = nextMinutes - currentMinutes

            // Filter out bad calculations (negative) or giant gaps across unlogged days (over 12 hours)
            if (gapMinutes in 15..720) {
                // Determine the current feed's length. Fall back to 15 mins (900s) for standard bottle inputs
                val duration = if (currentFeed.totalDuration > 0) currentFeed.totalDuration else 900L
                pairs.add(FeedGapPair(duration, gapMinutes))
            }
        }

        if (pairs.isEmpty()) {
            Log.w("ANALYSIS_DEBUG", "⚠️ Valid gaps list empty after timeframe filtering threshold boundaries.")
            return null
        }

        // 4. BUCKET: Group the computed pairs into your requested time blocks
        val buckets = listOf(
            "0-10 min" to pairs.filter {
                val mins = it.feedDurationMinutes / 60L // 🌟 Convert seconds to minutes
                mins in 0..10
            },
            "10-20 min" to pairs.filter {
                val mins = it.feedDurationMinutes / 60L
                mins in 11..20
            },
            "20-30 min" to pairs.filter {
                val mins = it.feedDurationMinutes / 60L
                mins in 21..30
            },
            "30+ min" to pairs.filter {
                val mins = it.feedDurationMinutes / 60L
                mins > 30
            }
        )

        val bucketDataList = buckets.map { (label, filteredPairs) ->
            val avgGap = if (filteredPairs.isNotEmpty()) {
                filteredPairs.map { it.gapMinutes }.average().toInt()
            } else {
                0
            }

            Log.d("ANALYSIS_DEBUG", "📦 Bucket [$label]: Found ${filteredPairs.size} matches. Avg Gap: $avgGap mins")

            FeedingBucketData(
                rangeLabel = label,
                averageGapMinutes = avgGap,
                totalCount = filteredPairs.size
            )
        }

        return FeedingAnalysisResult(bucketGaps = bucketDataList)
    }


    /**
     * Converts a standard "yyyy-MM-dd HH:mm" string into raw total minutes since a baseline
     * to easily calculate time differences without using Java 8 time libraries.
     */
    private fun parseToTotalMinutes(dateTimeString: String): Long {
        return try {
            // Split "2026-08-16 22:31" into ["2026-08-16", "22:31"]
            val spaceParts = dateTimeString.split(" ")
            if (spaceParts.size < 2) return -1L

            val dateParts = spaceParts[0].split("-") // ["2026", "08", "16"]
            val timeParts = spaceParts[1].split(":") // ["22", "31"]

            if (dateParts.size < 3 || timeParts.size < 2) return -1L

            // Explicitly cast every integer chunk into a Long primitive immediately
            val year = dateParts[0].toLong()
            val month = dateParts[1].toLong()
            val day = dateParts[2].toLong()
            val hour = timeParts[0].toLong()
            val minute = timeParts[1].toLong()

            // Cumulative days at start of each month (non-leap year)
            val monthOffsets = longArrayOf(0, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
            val safeMonth = month.toInt().coerceIn(1..12)

            // Combine operations using uniform Long math
            val minutesInDay = (hour * 60L) + minute
            val minutesInYear = year * 365L * 24L * 60L
            val minutesInMonth = monthOffsets[safeMonth] * 24L * 60L
            val minutesInDays = (day - 1) * 24L * 60L

            minutesInYear + minutesInMonth + minutesInDays + minutesInDay
        } catch (e: Exception) {
            -1L
        }
    }
}
