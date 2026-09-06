package com.bsdevs.babycare.domain

import com.bsdevs.babycare.network.UnifiedEventDto
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedingPredictionEngine @Inject constructor() {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /**
     * Predicts the next feeding time based on historical data.
     * Returns null if not enough data is available (needs at least 2 events).
     */
    fun predictNextFeeding(events: List<UnifiedEventDto>, now: LocalDateTime): LocalDateTime? {
        val feedingEvents = events
            .filter { it.type == "FEEDING" }
            .mapNotNull { parseDateTime(it.dateTimeString) }
            .sorted()

        if (feedingEvents.size < 2) return null

        val lastFeeding = feedingEvents.last()
        
        // 1. Calculate weighted average of recent gaps
        val recentGaps = calculateRecentGaps(feedingEvents)
        if (recentGaps.isEmpty()) return null
        
        val weightedAverageGapMinutes = calculateWeightedAverage(recentGaps)

        // 2. Trend analysis (recency bias for growing/shrinking intervals)
        val trendAdjustment = calculateTrendAdjustment(recentGaps)
        
        // 3. Time-of-day bias
        val timeOfDayAdjustment = calculateTimeOfDayBias(feedingEvents, lastFeeding, now)

        val predictedGapMinutes = (weightedAverageGapMinutes + trendAdjustment + timeOfDayAdjustment).toLong()
            .coerceAtLeast(30) // Minimum 30 mins between feeds

        return lastFeeding.plusMinutes(predictedGapMinutes)
    }

    private fun parseDateTime(dateTimeString: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(dateTimeString, dateTimeFormatter)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateRecentGaps(events: List<LocalDateTime>): List<Long> {
        val gaps = mutableListOf<Long>()
        for (i in 1 until events.size) {
            val gap = Duration.between(events[i - 1], events[i]).toMinutes()
            // Ignore gaps larger than 12 hours as they are likely outliers (e.g., missed logging)
            if (gap in 30..720) {
                gaps.add(gap)
            }
        }
        // Take last 5 gaps for recency
        return gaps.takeLast(5)
    }

    private fun calculateWeightedAverage(gaps: List<Long>): Double {
        var totalWeight = 0
        var weightedSum = 0.0
        
        gaps.forEachIndexed { index, gap ->
            val weight = index + 1 // Higher index = more recent = more weight
            weightedSum += gap * weight
            totalWeight += weight
        }
        
        return weightedSum / totalWeight
    }

    private fun calculateTrendAdjustment(gaps: List<Long>): Double {
        if (gaps.size < 3) return 0.0
        
        // Check if gaps are consistently increasing or decreasing
        val lastThree = gaps.takeLast(3)
        val isIncreasing = lastThree[0] < lastThree[1] && lastThree[1] < lastThree[2]
        val isDecreasing = lastThree[0] > lastThree[1] && lastThree[1] > lastThree[2]
        
        return when {
            isIncreasing -> (lastThree[2] - lastThree[0]) / 2.0
            isDecreasing -> (lastThree[2] - lastThree[0]) / 2.0
            else -> 0.0
        }
    }

    private fun calculateTimeOfDayBias(
        allEvents: List<LocalDateTime>,
        lastFeeding: LocalDateTime,
        now: LocalDateTime
    ): Double {
        val lastTime = lastFeeding.toLocalTime()
        
        // Find historical feeds that happened around the same time of day (within 1 hour)
        val similarTimeFeeds = allEvents.filter { 
            val time = it.toLocalTime()
            val diff = Math.abs(Duration.between(time, lastTime).toMinutes())
            diff <= 60 && it != lastFeeding
        }

        if (similarTimeFeeds.isEmpty()) return 0.0

        // Calculate the average gap after those similar-time feeds
        val subsequentGaps = mutableListOf<Long>()
        similarTimeFeeds.forEach { historicalFeed ->
            val index = allEvents.indexOf(historicalFeed)
            if (index != -1 && index < allEvents.size - 1) {
                val gap = Duration.between(allEvents[index], allEvents[index + 1]).toMinutes()
                if (gap in 30..720) {
                    subsequentGaps.add(gap)
                }
            }
        }

        if (subsequentGaps.isEmpty()) return 0.0
        
        val avgHistoricalGap = subsequentGaps.average()
        
        // We give this historical pattern a slight influence (20%)
        return (avgHistoricalGap - calculateWeightedAverage(calculateRecentGaps(allEvents))) * 0.2
    }
}
