package com.bsdevs.babycare.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.DailyLogDto
import com.bsdevs.babycare.network.FeedingDto
import com.bsdevs.babycare.network.NappyChangeDto
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

sealed class HomeFeedItem {
    data class Header(val title: String, val feedingCount: Int, val nappyCount: Int) :
        HomeFeedItem()

    data class ActivityRow(val activity: BabyActivity) : HomeFeedItem()
}

data class BabyCareHomeViewData(
    val lastNappyChange: String? = null,
    val lastFeeding: String? = null,
    val activityFeed: List<HomeFeedItem> = emptyList(),
    val canLoadMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val currentFilter: ActivityFilter = ActivityFilter.NONE,
    val collapsedHeaders: Set<String> = emptySet(),
)

enum class ActivityFilter { NONE, NAPPY, FEEDING }

@HiltViewModel
class BabyCareHomeViewModel @Inject constructor(
    private val repository: BabyCareRepository,
    private val accountService: AccountService
) : ViewModel() {

    private val pageSize = 20
    private val _viewData = MutableStateFlow<Result<BabyCareHomeViewData>>(Result.Loading)

    // Internal trackers for configuration states
    private val _currentFilter = MutableStateFlow(ActivityFilter.NONE)
    private val _collapsedHeaders = MutableStateFlow<Set<String>>(emptySet())

    private val _isInitialLoading = MutableStateFlow(false)

    val viewData: StateFlow<Result<BabyCareHomeViewData>> = combine(
        repository.cachedDays,
        _currentFilter,
        _collapsedHeaders,
        _isInitialLoading
    ) { dailyLogsList, filter, collapsed, isLoading ->
        if (isLoading && dailyLogsList.isEmpty()) {
            Result.Loading
        } else {
            val processed = processFeed(dailyLogsList, filter, collapsed)
            Result.Success(processed)
        }
    }.catch { e ->
        emit(Result.Error(e))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Result.Loading
    )

    init {
        loadData()
    }

    fun loadData() {
        // Prevent redundant network fetches if cached data is already present
        viewModelScope.launch {
            _isInitialLoading.value = true
            try {
                repository.loadInitialData(accountService.currentUserId, pageSize)
            } catch (e: Exception) {
                // Handle initial fetch failure
            } finally {
                _isInitialLoading.value = false
            }
        }
    }

    fun refreshData() {
        val current = (_viewData.value as? Result.Success)?.data ?: return
        if (current.isRefreshing || current.isLoadingMore) return

        viewModelScope.launch {
            setRefreshingState(true)
            try {
                repository.refreshData(accountService.currentUserId, pageSize)
            } finally {
                setRefreshingState(false)
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
                    type = correctedNappyType ?: "Wet"
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
                    bottleAmountMl = event.bottleAmountMl
                )
            )
        }
    }

    fun toggleActivityFilter(filter: ActivityFilter) {
        _currentFilter.update { current -> if (current == filter) ActivityFilter.NONE else filter }
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
        _collapsedHeaders.update { current -> if (current.contains(title)) current - title else current + title }
    }

    private fun setLoadingMoreState(value: Boolean) {
        val currentResult = _viewData.value
        if (currentResult is Result.Success) {
            _viewData.value = Result.Success(
                currentResult.data.copy(isLoadingMore = value)
            )
        }
    }

    private fun setRefreshingState(value: Boolean) {
        val currentResult = _viewData.value
        if (currentResult is Result.Success) {
            _viewData.value = Result.Success(
                currentResult.data.copy(isRefreshing = value)
            )
        }
    }
}
