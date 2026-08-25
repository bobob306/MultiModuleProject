package com.bsdevs.babycare.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.DailyLogDto
import com.bsdevs.babycare.network.FeedingDto
import com.bsdevs.babycare.network.NappyChangeDto
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.babycare.presentation.common.BabyActivity
import com.bsdevs.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BabyCareHomeViewModel @Inject constructor(
    private val repository: BabyCareRepository,
    private val accountService: AccountService
) : ViewModel() {

    private val pageSize = 20

    // Internal trackers for configuration states
    private val _currentFilter = MutableStateFlow(ActivityFilter.NONE)
    private val _collapsedHeaders = MutableStateFlow<Set<String>>(emptySet())

    private val _viewData = MutableStateFlow<Result<BabyCareHomeViewData>>(Result.Loading)
    val viewData: StateFlow<Result<BabyCareHomeViewData>> = _viewData.asStateFlow()

    init {
        // 🌟 2. Observe the repository cache in the background to handle instant updates
        // without letting empty states lock up our initialization pipeline
        viewModelScope.launch {
            repository.cachedDays.collect { dailyLogs ->
                // Only map to Success if we aren't currently waiting on a full initial pull
                if (_viewData.value !is Result.Loading || dailyLogs.isNotEmpty()) {
                    updateDisplayFeed(dailyLogs)
                }
            }
        }

        // Trigger initial data load immediately on launch
        initialLoad()
    }

    private fun initialLoad() {
        viewModelScope.launch {
            try {
                // Fetch the current month document from Firestore
                val fetchResult = repository.loadInitialData(accountService.currentUserId, pageSize)

                // Force switch the state to success immediately, even if the month is brand new/empty
                updateDisplayFeed(repository.cachedDays.value, fetchResult.hasMoreData)
            } catch (e: Exception) {
                Log.e("HOME_INIT_ERROR", "Failed initial data block fetch", e)
                _viewData.value = Result.Error(e)
            }
        }
    }
    private fun updateDisplayFeed(dailyLogs: List<DailyLogDto>, canLoadMore: Boolean = true) {
        val processedFeed = processFeed(
            dailyLogs = dailyLogs,
            filter = _currentFilter.value,
            collapsed = _collapsedHeaders.value
        )
        _viewData.value = Result.Success(
            processedFeed.copy(
                isRefreshing = false,
                isLoadingMore = false,
                canLoadMore = canLoadMore
            )
        )
    }

    fun refreshData() {
        val current = when (val currentState = _viewData.value) {
            is Result.Success -> currentState.data
            else -> BabyCareHomeViewData(isRefreshing = true)
        }

        if (current.isLoadingMore) return

        viewModelScope.launch {
            // Turn on pull-to-refresh spinner indicator
            _viewData.value = Result.Success(current.copy(isRefreshing = true))

            try {
                val refreshResult = repository.loadInitialData(accountService.currentUserId, pageSize)
                updateDisplayFeed(repository.cachedDays.value, refreshResult.hasMoreData)
            } catch (e: Exception) {
                Log.e("REFRESH_ERROR", "Failed to clear refresh cycle", e)
                _viewData.value = Result.Success(current.copy(isRefreshing = false))
            }
        }
    }

    fun loadMore() {
        val currentResult = _viewData.value
        // 🛡️ Safeguard: check that we are in a stable Success state first
        if (currentResult !is Result.Success || currentResult.data.isLoadingMore || !currentResult.data.canLoadMore) return

        val visibleRowsCount =
            currentResult.data.activityFeed.count { it is HomeFeedItem.ActivityRow }

        // If the user has collapsed everything, stop automatic background network triggers
        if (visibleRowsCount == 0) return

        viewModelScope.launch {
            setLoadingMoreState(true)
            try {
                // 🔄 Trigger your clean day-block repository pagination method
                val result = repository.loadMoreData(accountService.currentUserId, pageSize)

                // Update the loading flags based on the returned repository signals
                _viewData.update { current ->
                    if (current is Result.Success) {
                        Result.Success(
                            current.data.copy(
                                canLoadMore = result.hasMoreData,
                                isLoadingMore = false
                            )
                        )
                    } else {
                        current
                    }
                }
            } catch (e: Exception) {
                setLoadingMoreState(false)
            }
        }
    }

    private fun processFeed(
        dailyLogs: List<DailyLogDto>,
        filter: ActivityFilter,
        collapsed: Set<String>
    ): BabyCareHomeViewData {
        val finalizedFeed = mutableListOf<HomeFeedItem>()

        var absoluteLastNappy: String? = null
        var absoluteLastFeeding: String? = null

        dailyLogs.forEachIndexed { index, dayLog ->

            // 🌟 FIXED: Force all nested events for this calendar day to sort by time (newest first)
            val sortedDayEvents = dayLog.events.sortedByDescending { it.dateTimeString }

            // 🔄 Apply active filter rules onto the cleanly sorted list array instead of the raw one
            val visibleEvents = sortedDayEvents.filter { event ->
                when (filter) {
                    ActivityFilter.NONE -> true
                    ActivityFilter.NAPPY -> event.type == "NAPPY"
                    ActivityFilter.FEEDING -> event.type == "FEEDING"
                }
            }

            val feedingCount = visibleEvents.count { it.type == "FEEDING" }
            val nappyCount = visibleEvents.count { it.type == "NAPPY" }
            val displayHeaderTitle = formatHeaderDate(dayLog.date)

            finalizedFeed.add(HomeFeedItem.Header(displayHeaderTitle, feedingCount, nappyCount))

            // Capture your summary tiles from the top sorted item of the most recent day block
            if (index == 0) {
                absoluteLastNappy = sortedDayEvents.firstOrNull { it.type == "NAPPY" }
                    ?.let { "Last nappy: ${it.time}" }
                absoluteLastFeeding = sortedDayEvents.firstOrNull { it.type == "FEEDING" }
                    ?.let { "Last feed: ${it.time}" }
            }

            if (!collapsed.contains(displayHeaderTitle)) {
                visibleEvents.forEach { unifiedEvent ->
                    val babyActivityModel = mapToBabyActivity(unifiedEvent, dayLog.date)
                    finalizedFeed.add(HomeFeedItem.ActivityRow(babyActivityModel))
                }
            }
        }

        return BabyCareHomeViewData(
            lastNappyChange = absoluteLastNappy,
            lastFeeding = absoluteLastFeeding,
            activityFeed = finalizedFeed,
            currentFilter = filter,
            collapsedHeaders = collapsed
        )
    }


    private fun mapToBabyActivity(event: UnifiedEventDto, parentDate: String): BabyActivity {
        // 🔄 Fix 1: Extract the "HH:mm" time segment dynamically from the dateTimeString if the time field is blank
        val extractedTime = if (!event.time.isNullOrEmpty()) {
            event.time
        } else {
            event.dateTimeString.split(" ").getOrNull(1) ?: ""
        }

        // 🔄 Fix 2: If the type field was corrupted (e.g., set to "Wet"), recognize it as a nappy activity
        val isNappy =
            event.type == "NAPPY" || event.type == "Wet" || event.type == "Dirty" || event.type == "Both"

        return if (isNappy) {
            // Fallback: If nappyType is missing because it was saved under 'type', recover it here
            val correctedNappyType =
                if (!event.nappyType.isNullOrEmpty()) event.nappyType else event.type

            BabyActivity.Nappy(
                NappyChangeDto(
                    id = event.id,
                    date = parentDate,
                    time = extractedTime, // ✨ Time is now safely populated
                    dateTime = event.dateTimeString,
                    type = correctedNappyType ?: "Wet",
                    comment = event.comment,
                )
            )
        } else {
            BabyActivity.Feeding(
                FeedingDto(
                    id = event.id,
                    date = parentDate,
                    startTime = extractedTime,
                    dateTime = event.dateTimeString,
                    mainFeedingSide = event.mainFeedingSide,
                    leftDuration = event.leftDuration,
                    rightDuration = event.rightDuration,
                    totalDuration = event.totalDuration,
                    bottleAmountMl = event.bottleAmountMl,
                    comment = event.comment
                )
            )
        }
    }

    private fun formatHeaderDate(dateString: String): String {
        return try {
            // Safe check: extract just the YYYY-MM-DD segment if it contains time info
            val cleanDateStr = dateString.split(" ").firstOrNull() ?: dateString
            val targetDate = LocalDate.parse(cleanDateStr)
            val today = LocalDate.now()

            when (targetDate) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> {
                    val day = targetDate.dayOfMonth
                    val suffix = when {
                        day in 11..13 -> "th"
                        day % 10 == 1 -> "st"
                        day % 10 == 2 -> "nd"
                        day % 10 == 3 -> "rd"
                        else -> "th"
                    }
                    val monthFormatter = DateTimeFormatter.ofPattern(" MMMM", Locale.ENGLISH)
                    "$day$suffix${targetDate.format(monthFormatter)}"
                }
            }
        } catch (e: Exception) {
            dateString // Fallback safety representation if parsing strings fails
        }
    }

    fun toggleHeaderCollapse(title: String) {
        _collapsedHeaders.update { current ->
            if (current.contains(title)) current - title else current + title
        }
        // 🌟 FORCE UI REFRESH: Immediately push the recalculated state to the screen
        updateDisplayFeed(repository.cachedDays.value)
    }

    fun toggleActivityFilter(filter: ActivityFilter) {
        _currentFilter.update { current ->
            if (current == filter) ActivityFilter.NONE else filter
        }
        // 🌟 FORCE UI REFRESH: Immediately push the filtered state to the screen
        updateDisplayFeed(repository.cachedDays.value)
    }

    private fun setLoadingMoreState(value: Boolean) {
        val currentResult = _viewData.value
        if (currentResult is Result.Success) {
            _viewData.value = Result.Success(
                currentResult.data.copy(isLoadingMore = value)
            )
        }
    }
}
