package com.bsdevs.babycare.presentation.graph

data class FeedingGraphUiState(
    val hourlyCounts: List<HourlyFeedingCount> = emptyList(),
    val totalFeedsInCache: Int = 0,
    val analysisResult: FeedingAnalysisResult? = null,
    val dailyAverageGaps: List<DailyAverageGap> = emptyList(),
)

data class FeedingAnalysisResult(
    val bucketGaps: List<FeedingBucketData> = emptyList()
)

data class FeedingBucketData(
    val rangeLabel: String,     // e.g., "10-20 min"
    val averageGapMinutes: Int, // Average resting gap following this feed length
    val totalCount: Int         // Number of instances found in history
)

data class HourlyFeedingCount(
    val hour: Int,         // 0 to 23
    val displayLabel: String, // e.g., "02:00"
    val count: Int
)

data class DailyAverageGap(
    val dateString: String,      // e.g., "2026-08-16" for the X-axis label
    val averageGapMinutes: Int,   // Y-axis value
    val rolling14DayAverageMinutes: Int?,
)
