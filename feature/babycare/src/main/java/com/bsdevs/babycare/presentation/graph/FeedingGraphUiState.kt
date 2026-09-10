package com.bsdevs.babycare.presentation.graph

import java.time.LocalDate

data class FeedingGraphUiState(
    val hourlyCounts: List<HourlyFeedingCount> = emptyList(),
    val totalFeedsInCache: Int = 0,
    val analysisResult: FeedingAnalysisResult? = null,
    val dailyAverageGaps: List<DailyAverageGap> = emptyList(),
    val dateFilter: DateFilter = DateFilter.LastNDays(7),
    val availableDates: Set<LocalDate> = emptySet(),
    val isLoading: Boolean = false,
    val showDatePicker: Boolean = false,
    val isGapChartFullScreen: Boolean = false,
    val selectedGapIndex: Int? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)

sealed class DateFilter {
    data class LastNDays(val days: Int) : DateFilter()
    data class CustomRange(val start: LocalDate, val end: LocalDate) : DateFilter()
    object AllTime : DateFilter()
}

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
    val date: LocalDate? = null
)
