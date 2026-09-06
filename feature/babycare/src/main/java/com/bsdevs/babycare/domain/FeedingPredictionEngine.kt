package com.bsdevs.babycare.domain

import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.babycare.presentation.measurement.WhoGrowthData
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

data class PredictionResult(
    val predictedTime: LocalDateTime,
    val confidenceRangeMinutes: Int
)

@Singleton
class FeedingPredictionEngine @Inject constructor() {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /**
     * Predicts the next feeding time based on historical data and baby context.
     * Returns null if not enough data is available (needs at least 2 events).
     */
    fun predictNextFeeding(
        events: List<UnifiedEventDto>,
        context: BabyContext = BabyContext()
    ): PredictionResult? {
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
        val timeOfDayAdjustment = calculateTimeOfDayBias(feedingEvents, lastFeeding)

        // 4. Biological adjustments
        val ageAdjustment = calculateAgeAdjustment(context)
        val growthSpurtAdjustment = calculateGrowthSpurtAdjustment(context)
        val whoAdjustment = calculateWhoCentileAdjustment(context)
        val metabolicAdjustment = calculateMetabolicAdjustment(context)

        val totalAdjustment = trendAdjustment + timeOfDayAdjustment + ageAdjustment + growthSpurtAdjustment + whoAdjustment + metabolicAdjustment
        
        val predictedGapMinutes = (weightedAverageGapMinutes + totalAdjustment)
            .toLong()
            .coerceAtLeast(30) // Minimum 30 mins between feeds

        val predictedTime = lastFeeding.plusMinutes(predictedGapMinutes)
        
        // 5. Consistency / Confidence Index
        val confidenceRange = calculateConfidenceRange(recentGaps)

        return PredictionResult(predictedTime, confidenceRange)
    }

    private fun calculateConfidenceRange(gaps: List<Long>): Int {
        if (gaps.size < 3) return 30 // Default 30 min window for low data
        
        val mean = gaps.average()
        val standardDeviation = sqrt(gaps.map { (it - mean).pow(2.0) }.average())
        
        // Window is 1.5 * Standard Deviation, capped between 15 and 60 mins
        return (standardDeviation * 1.5).toInt().coerceIn(15, 60)
    }

    private fun calculateWhoCentileAdjustment(context: BabyContext): Double {
        val birthDateStr = context.birthDate ?: return 0.0
        val lastWeight = context.measurements
            .filter { it.type == "MEASUREMENT" && it.weight != null }
            .maxByOrNull { it.dateTimeString }?.weight ?: return 0.0
        
        return try {
            val birthDate = LocalDate.parse(birthDateStr)
            val ageMonths = ChronoUnit.MONTHS.between(birthDate, LocalDate.now()).toInt()
            val isFemale = context.gender?.lowercase() == "female"
            
            val whoData = if (isFemale) WhoGrowthData.weightForAgeGirls else WhoGrowthData.weightForAgeBoys
            val centilePoint = whoData.lastOrNull { it.month <= ageMonths } ?: whoData.first()
            
            // centilePoint.values has percentiles: 0.4, 2, 9, 25, 50, 75, 91, 98, 99.6
            val p50 = centilePoint.values[4]
            val p91 = centilePoint.values[6]
            
            when {
                lastWeight >= p91 -> -15.0 // High centile babies often eat more frequently
                lastWeight >= p50 -> -5.0
                else -> 0.0
            }
        } catch (_: Exception) {
            0.0
        }
    }

    private fun calculateMetabolicAdjustment(context: BabyContext): Double {
        val nappies = context.nappyEvents
            .filter { it.dateTimeString.isNotEmpty() }
            .mapNotNull { parseDateTime(it.dateTimeString) }
            .sorted()
        
        if (nappies.size < 5) return 0.0
        
        // Calculate average nappy frequency in the last 24 hours vs last 7 days
        val now = LocalDateTime.now()
        val last24hCount = nappies.count { it.isAfter(now.minusDays(1)) }
        val last7dCount = nappies.count { it.isAfter(now.minusDays(7)) }
        val avgNappiesPerDay = last7dCount / 7.0
        
        return if (last24hCount > avgNappiesPerDay * 1.2) {
            // 20% increase in output = higher metabolic demand
            -10.0
        } else {
            0.0
        }
    }

    private fun calculateAgeAdjustment(context: BabyContext): Double {
        val birthDateStr = context.birthDate ?: return 0.0
        return try {
            val birthDate = LocalDate.parse(birthDateStr)
            val ageInWeeks = ChronoUnit.WEEKS.between(birthDate, LocalDate.now())
            
            // Heuristic: Add 5 minutes for every month of age, up to 1 hour max.
            val ageInMonths = ageInWeeks / 4.0
            (ageInMonths * 5.0).coerceAtMost(60.0)
        } catch (_: Exception) {
            0.0
        }
    }

    private fun calculateGrowthSpurtAdjustment(context: BabyContext): Double {
        val weights = context.measurements
            .filter { it.type == "MEASUREMENT" && it.weight != null }
            .sortedBy { it.dateTimeString }
        
        if (weights.size < 2) return 0.0
        
        val lastWeight = weights.last()
        val prevWeight = weights[weights.size - 2]
        
        return try {
            val lastDate = LocalDate.parse(lastWeight.dateTimeString.split(" ").first())
            val prevDate = LocalDate.parse(prevWeight.dateTimeString.split(" ").first())
            
            val daysBetween = ChronoUnit.DAYS.between(prevDate, lastDate)
            if (daysBetween <= 0) return 0.0
            
            val weightGain = lastWeight.weight!! - prevWeight.weight!!
            val gainPerDay = weightGain / daysBetween
            
            // Heuristic: If gain is > 40g/day, baby is likely in a growth spurt.
            // Reduce feeding intervals by 15-30 minutes.
            if (gainPerDay > 0.040) {
                -20.0
            } else {
                0.0
            }
        } catch (_: Exception) {
            0.0
        }
    }

    private fun parseDateTime(dateTimeString: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(dateTimeString, dateTimeFormatter)
        } catch (_: Exception) {
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
        if (gaps.size < 2) return 0.0
        
        // Calculate the difference between each consecutive gap
        val differences = mutableListOf<Long>()
        for (i in 1 until gaps.size) {
            differences.add(gaps[i] - gaps[i - 1])
        }
        
        // The adjustment is the average "velocity" of the gap change
        // Projected forward to the next feeding.
        return differences.average()
    }

    private fun calculateTimeOfDayBias(
        allEvents: List<LocalDateTime>,
        lastFeeding: LocalDateTime
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
