package com.bsdevs.babycare.presentation.babyhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.DailyLogDto
import com.bsdevs.babycare.presentation.babyactivities.BabyActivity
import com.bsdevs.common.result.Result
import com.bsdevs.data.BabyDashboardTileNetwork
import com.bsdevs.data.LocationTypeData
import com.bsdevs.data.LocationTypeData.INTERNAL
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.SizeData
import com.bsdevs.data.SpacerTypeData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
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


sealed class HomeUiIntent {
    data class CollapseHeader(val dateHeader: String) : HomeUiIntent()
    data class ToggleFilter(val activityType: String) : HomeUiIntent()
    data class EditActivityRow(val id: String, val activityType: String) : HomeUiIntent()
}

data class BabyCareHomeViewData(
    val lastNappyChange: String? = null,
    val lastFeeding: String? = null,
    val activityFeed: List<NetworkScreenData> = emptyList(),
    val canLoadMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val currentFilter: ActivityFilter = ActivityFilter.NONE,
    val collapsedHeaders: Set<String> = emptySet(),
)

enum class ActivityFilter { NONE, NAPPY, FEEDING }

@HiltViewModel
class BabyCareHomeViewModel @Inject constructor(
    private val repository: BabyCareRepository, private val accountService: AccountService
) : ViewModel() {

    private val pageSize = 20
    private val _viewData = MutableStateFlow<Result<BabyCareHomeViewData>>(Result.Loading)
    val viewData: StateFlow<Result<BabyCareHomeViewData>> = _viewData.asStateFlow()

    private val _navigationEvents = Channel<BabyCareHomeNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    // Internal trackers for configuration states
    private val _currentFilter = MutableStateFlow(ActivityFilter.NONE)
    private val _collapsedHeaders = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            combine(
                repository.cachedDays, // 🔄 Collect your modern unified days data stream
                _currentFilter, _collapsedHeaders
            ) { dailyLogsList, filter, collapsed ->
                processFeed(dailyLogsList, filter, collapsed)
            }.collect { updatedViewData ->
                _viewData.value = Result.Success(updatedViewData)
            }
        }
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _viewData.update { Result.Loading }
            try {
                repository.loadInitialData(accountService.currentUserId, pageSize)
            } catch (e: Exception) {
                _viewData.value = Result.Error(e)
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

        // 🔄 FIXED: Check for your modern Server-Driven Row Node type instead of the old sealed class
        val visibleRowsCount = currentResult.data.activityFeed.count {
            it is NetworkScreenData.BabyFeedRowNetwork
        }

        // If the user has collapsed all days, stop automatic background network triggers
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
                                canLoadMore = result.hasMoreData, isLoadingMore = false
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

    fun onUiIntent(intent: HomeUiIntent) {
        val currentResult = _viewData.value as? Result.Success
        if (currentResult !is Result.Success) return

        when (intent) {
            // 📌 1. Handle Collapse logic entirely in the ViewModel
            is HomeUiIntent.CollapseHeader -> {
                onCollapseHeader(currentResult, intent)
            }

            // 🕒 2. Handle Filter logic entirely in the ViewModel
            is HomeUiIntent.ToggleFilter -> {
                onToggleFilter(currentResult, intent)
            }

            // 🎬 3. Handle Edit checks inside the ViewModel and fire it down your navigation channel
            is HomeUiIntent.EditActivityRow -> {
                onEditActivityRow(currentResult, intent)
            }
        }
    }

    private fun processFeed(
        dailyLogs: List<DailyLogDto>, filter: ActivityFilter, collapsed: Set<String>
    ): BabyCareHomeViewData {
        // 🧱 1. Create a flat list of server-driven screen models
        val finalizedSduiFeed = mutableListOf<NetworkScreenData>()
        var indexCounter = 0

        // ➕ 2. Build and inject your independent Sub-Type Tiles right at the top of the stream
        finalizedSduiFeed.add(
            NetworkScreenData.BabyDashboardTilesNetwork(
                index = indexCounter++, content = buildTileContent(dailyLogs)
            )
        )

        // ➕ 3. Inject a small decorative layout spacer or label under your top tiles block
        finalizedSduiFeed.add(
            NetworkScreenData.SpacerDataNetwork(
                index = indexCounter++, size = SizeData(
                    type = SpacerTypeData.HEIGHT, height = 16, weight = null
                )
            )
        )

        // 🔄 4. Process your chronological database days into server-driven elements
        dailyLogs.forEachIndexed { dayIndex, dayLog ->

            // Ensure all nested events for this calendar day are sorted by time (newest first)
            val sortedDayEvents = dayLog.events.sortedByDescending { it.dateTimeString }

            // Filter events matching active filter visibility constraints
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

            // ➕ 5. Inject a Day Header node
            finalizedSduiFeed.add(
                NetworkScreenData.BabyFeedHeaderNetwork(
                    index = indexCounter++,
                    title = displayHeaderTitle,
                    feedingCount = feedingCount,
                    nappyCount = nappyCount
                )
            )

            // ➕ 6. Append Row nodes directly underneath unless the day is actively collapsed
            if (!collapsed.contains(displayHeaderTitle)) {
                visibleEvents.forEach { unifiedEvent ->

                    // Formulate descriptive subtitle content contexts dynamically based on type properties
                    val structuralSubtitle = if (unifiedEvent.type == "NAPPY") {
                        "Nappy Change: ${unifiedEvent.nappyType ?: "Wet"}"
                    } else {
                        if (unifiedEvent.mainFeedingSide == "Bottle") {
                            "Bottle Feeding (${unifiedEvent.bottleAmountMl ?: 0}ml)"
                        } else {
                            "Breast Feeding: ${unifiedEvent.mainFeedingSide ?: "Both"}"
                        }
                    }

                    finalizedSduiFeed.add(
                        NetworkScreenData.BabyFeedRowNetwork(
                            index = indexCounter++,
                            id = unifiedEvent.id,
                            activityType = unifiedEvent.type,
                            title = if (unifiedEvent.type == "NAPPY") "Nappy Change" else "Feeding Session",
                            subtitle = structuralSubtitle,
                            time = unifiedEvent.time,
                            rawDate = dayLog.date
                        )
                    )
                }
            }
        }

        // 🧱 7. Return your fully rendered server-driven view state model configuration
        return BabyCareHomeViewData(
            activityFeed = finalizedSduiFeed, // 🌟 Pushes unified List<NetworkScreenData> directly down
            currentFilter = filter, collapsedHeaders = collapsed
        )
    }

    private fun buildTileContent(dailyLogs: List<DailyLogDto>): List<BabyDashboardTileNetwork> {

        // 💥 1. Flatten all nested day events into a single linear collection list array
        val allEventsFlatList = dailyLogs.flatMap { it.events }

        // 🔄 2. Sort all combined events chronologically (Newest items at index 0)
        val sortedEvents = allEventsFlatList.sortedByDescending { it.dateTimeString }

        // 🍼 3. Extract the absolute first occurrence flagged as a FEEDING
        val mostRecentFeeding = sortedEvents.firstOrNull { it.type == "FEEDING" }

        // 🧷 4. Extract the absolute first occurrence flagged as a NAPPY
        val mostRecentNappyChange = sortedEvents.firstOrNull { it.type == "NAPPY" }

        val nappyTile = BabyDashboardTileNetwork(
            lastNappyChange = mostRecentNappyChange?.time,
            destination = "babycare://nappy",
            locationTypeData = INTERNAL,
            label = "Feeding nav",
            lastFeeding = null,
        )
        val feedingTile = BabyDashboardTileNetwork(
            lastNappyChange = null,
            destination = "babycare://feeding",
            locationTypeData = INTERNAL,
            label = "Feeding nav",
            lastFeeding = mostRecentFeeding?.time,
        )
        return listOf(nappyTile, feedingTile)
    }

    private fun onCollapseHeader(
        currentResult: Result.Success<BabyCareHomeViewData>, intent: HomeUiIntent.CollapseHeader
    ) {
        val currentCollapsed = currentResult.data.collapsedHeaders
        val updatedCollapsed = if (currentCollapsed.contains(intent.dateHeader)) {
            currentCollapsed - intent.dateHeader
        } else {
            currentCollapsed + intent.dateHeader
        }

        val latestCachedDays = repository.cachedDays.value

        _viewData.value = Result.Success(
            currentResult.data.copy(
                collapsedHeaders = updatedCollapsed,
                // 🔄 FIXED: Append .activityFeed to pull the clean List<NetworkScreenData> out of the wrapper!
                activityFeed = processFeed(
                    dailyLogs = latestCachedDays,
                    filter = currentResult.data.currentFilter,
                    collapsed = updatedCollapsed
                ).activityFeed
            )
        )
    }

    private fun onToggleFilter(
        currentResult: Result.Success<BabyCareHomeViewData>, intent: HomeUiIntent.ToggleFilter
    ) {
        val targetFilter =
            if (intent.activityType == "NAPPY") ActivityFilter.NAPPY else ActivityFilter.FEEDING
        val activeFilter =
            if (currentResult.data.currentFilter == targetFilter) ActivityFilter.NONE else targetFilter

        _currentFilter.value = activeFilter // Sync back to your initialization combine blocks
    }

    fun onEditActivityRow(
        currentResult: Result.Success<BabyCareHomeViewData>,
        intent: HomeUiIntent.EditActivityRow
    ) {
        viewModelScope.launch {
            val destinationUrl = if (intent.activityType == "NAPPY") {
                "babycare://edit_nappy?id=${intent.id}"
            } else {
                "babycare://edit_feeding?id=${intent.id}"
            }
            println(destinationUrl)
            // Emits a navigation side effect back up to your core NavHost file safely!
            _navigationEvents.send(
                BabyCareHomeNavigationEvent.NavigateToDeepLink(
                    uriString = destinationUrl,
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

    fun onNavigate(destination: String, location: LocationTypeData, display: String) {
        viewModelScope.launch {
            if (location == INTERNAL) {
                // Emits the safe action token down into your screen view listener pipe
                _navigationEvents.send(BabyCareHomeNavigationEvent.NavigateToDeepLink(destination))
            } else {
                // Optional: Handle EXTERNAL browser redirection URLs here down the road
            }
        }
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

sealed class BabyCareHomeNavigationEvent {
    data class NavigateToDeepLink(val uriString: String) : BabyCareHomeNavigationEvent()
}