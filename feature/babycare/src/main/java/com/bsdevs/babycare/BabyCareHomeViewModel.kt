package com.bsdevs.babycare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.network.FeedingDto
import com.bsdevs.babycare.network.NappyChangeDto
import com.bsdevs.common.result.Result
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    private val accountService: AccountService
) : ViewModel() {

    private val _viewData = MutableStateFlow<Result<BabyCareHomeViewData>>(Result.Loading)
    val viewData: StateFlow<Result<BabyCareHomeViewData>> = _viewData
        .onStart {
            loadData()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    private var lastNappyDoc: DocumentSnapshot? = null
    private var lastFeedingDoc: DocumentSnapshot? = null
    private val pageSize = 15L
    private var cachedNappies = listOf<NappyChangeDto>()
    private var cachedFeedings = listOf<FeedingDto>()

    fun loadData() {
        viewModelScope.launch {
            _viewData.update { Result.Loading }
            lastNappyDoc = null
            lastFeedingDoc = null
            try {
                val userId = accountService.currentUserId

                val nappyTask = async { fetchNappyChangesBatch(userId, null) }
                val feedingTask = async { fetchFeedingsBatch(userId, null) }

                val nappyResult = nappyTask.await()
                val feedingResult = feedingTask.await()

                lastNappyDoc = nappyResult.second
                lastFeedingDoc = feedingResult.second

                // 🗃️ 1. Seed your cache memory properties with the raw initial datasets
                cachedNappies = nappyResult.first
                cachedFeedings = feedingResult.first

                _viewData.update {
                    Result.Success(
                        BabyCareHomeViewData(
                            lastNappyChange = nappyResult.first.firstOrNull()
                                ?.let { formatNappyChange(it) },
                            lastFeeding = feedingResult.first.firstOrNull()
                                ?.let { formatFeeding(it) },
                            activityFeed = getFilteredAndSortedFeed(ActivityFilter.NONE),
                            canLoadMore = nappyResult.first.size >= pageSize || feedingResult.first.size >= pageSize,
                            currentFilter = ActivityFilter.NONE
                        )
                    )
                }
            } catch (e: Exception) {
                _viewData.update { Result.Error(e) }
            }
        }
    }

    fun refreshData() {
        val currentResult = _viewData.value
        if (currentResult !is Result.Success || currentResult.data.isRefreshing || currentResult.data.isLoadingMore) return

        viewModelScope.launch {
            _viewData.update { Result.Success(currentResult.data.copy(isRefreshing = true)) }
            lastNappyDoc = null
            lastFeedingDoc = null

            try {
                val userId = accountService.currentUserId
                val nappyTask = async { fetchNappyChangesBatch(userId, null) }
                val feedingTask = async { fetchFeedingsBatch(userId, null) }

                val nappyResult = nappyTask.await()
                val feedingResult = feedingTask.await()

                lastNappyDoc = nappyResult.second
                lastFeedingDoc = feedingResult.second

                cachedNappies = nappyResult.first
                cachedFeedings = feedingResult.first

                val activeFilter = currentResult.data.currentFilter

                _viewData.update {
                    Result.Success(
                        BabyCareHomeViewData(
                            lastNappyChange = nappyResult.first.firstOrNull()
                                ?.let { formatNappyChange(it) },
                            lastFeeding = feedingResult.first.firstOrNull()
                                ?.let { formatFeeding(it) },
                            // 🔄 Regenerates your grouped items list using the active filter type
                            activityFeed = getFilteredAndSortedFeed(activeFilter),
                            canLoadMore = nappyResult.first.size >= pageSize || feedingResult.first.size >= pageSize,
                            isRefreshing = false,
                            currentFilter = activeFilter
                        )
                    )
                }
            } catch (e: Exception) {
                _viewData.update { Result.Success(currentResult.data.copy(isRefreshing = false)) }
            }
        }
    }

    fun loadMore() {
        val currentResult = _viewData.value
        if (currentResult !is Result.Success || currentResult.data.isLoadingMore || !currentResult.data.canLoadMore) return

        viewModelScope.launch {
            _viewData.update { Result.Success(currentResult.data.copy(isLoadingMore = true)) }
            try {
                val userId = accountService.currentUserId

                val nappyTask = async { fetchNappyChangesBatch(userId, lastNappyDoc) }
                val feedingTask = async { fetchFeedingsBatch(userId, lastFeedingDoc) }

                val nappyResult = nappyTask.await()
                val feedingResult = feedingTask.await()

                lastNappyDoc = nappyResult.second
                lastFeedingDoc = feedingResult.second

                cachedNappies = cachedNappies + nappyResult.first
                cachedFeedings = cachedFeedings + feedingResult.first

                val activeFilter = currentResult.data.currentFilter

                _viewData.update {
                    Result.Success(
                        currentResult.data.copy(
                            // 🔄 Re-evaluates the complete cached history with the new appended entries
                            activityFeed = getFilteredAndSortedFeed(activeFilter),
                            canLoadMore = nappyResult.first.size >= pageSize || feedingResult.first.size >= pageSize,
                            isLoadingMore = false
                        )
                    )
                }
            } catch (e: Exception) {
                _viewData.update { Result.Success(currentResult.data.copy(isLoadingMore = false)) }
            }
        }
    }

    private fun combineAndSort(
        nappies: List<NappyChangeDto>,
        feedings: List<FeedingDto>,
        collapsedHeaders: Set<String> // ➕ Add parameter parameter here
    ): List<HomeFeedItem> {
        val rawActivities = nappies.map { BabyActivity.Nappy(it) } + feedings.map { BabyActivity.Feeding(it) }
        val sortedActivities = rawActivities.sortedByDescending { it.dateTimeString }

        val groupedByDate = sortedActivities.groupBy { item ->
            val pureDateString = item.date!!.split(" ").firstOrNull() ?: item.date!!
            formatHeaderDate(pureDateString)
        }

        val finalizedFeed = mutableListOf<HomeFeedItem>()

        groupedByDate.forEach { (dateHeader, activitiesInDay) ->
            val feedingCount = activitiesInDay.count { it is BabyActivity.Feeding }
            val nappyCount = activitiesInDay.count { it is BabyActivity.Nappy }

            // Always include the Header block regardless of state layout rules
            finalizedFeed.add(HomeFeedItem.Header(dateHeader, feedingCount, nappyCount))

            // 🚨 CRUCIAL CHECK: Skip adding rows if this day header title is collapsed!
            if (!collapsedHeaders.contains(dateHeader)) {
                activitiesInDay.forEach { activity ->
                    finalizedFeed.add(HomeFeedItem.ActivityRow(activity))
                }
            }
        }

        return finalizedFeed
    }


    private suspend fun fetchNappyChangesBatch(
        userId: String,
        startAfter: DocumentSnapshot?
    ): Pair<List<NappyChangeDto>, DocumentSnapshot?> {
        var query =
            Firebase.firestore.collection("nappyChanges").document(userId).collection("changes")
                .orderBy("dateTime", Query.Direction.DESCENDING)
                .limit(pageSize)

        if (startAfter != null) {
            query = query.startAfter(startAfter)
        }

        val snapshot = query.get().await()
        val changes = snapshot.toObjects(NappyChangeDto::class.java)
        val lastDoc = snapshot.documents.lastOrNull()

        return Pair(changes, lastDoc)
    }

    private suspend fun fetchFeedingsBatch(
        userId: String,
        startAfter: DocumentSnapshot?
    ): Pair<List<FeedingDto>, DocumentSnapshot?> {
        var query = Firebase.firestore.collection("feedings").document(userId).collection("records")
            .orderBy("dateTime", Query.Direction.DESCENDING)
            .limit(pageSize)

        if (startAfter != null) {
            query = query.startAfter(startAfter)
        }

        val snapshot = query.get().await()
        val feedings = snapshot.toObjects(FeedingDto::class.java)
        val lastDoc = snapshot.documents.lastOrNull()

        return Pair(feedings, lastDoc)
    }

    private fun formatNappyChange(dto: NappyChangeDto): String? {
        val dateStr = dto.date ?: return null
        val timeStr = dto.time ?: ""
        return formatDateTime(dateStr, timeStr)
    }

    private fun formatFeeding(dto: FeedingDto): String? {
        val dateStr = dto.date ?: return null
        val timeStr = dto.startTime ?: ""
        val formatted = formatDateTime(dateStr, timeStr) ?: return null

        val sideIndicator = when (dto.mainFeedingSide) {
            "Left" -> " (L)"
            "Right" -> " (R)"
            "Bottle" -> " (B)"
            else -> ""
        }

        return "$formatted$sideIndicator"
    }

    private fun formatDateTime(dateStr: String, timeStr: String): String? {
        val date = try {
            LocalDate.parse(dateStr)
        } catch (_: Exception) {
            return null
        }
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        return when {
            date == today -> "today $timeStr"
            date == yesterday -> "yesterday $timeStr"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("d'th' MMMM", Locale.ENGLISH)
                date.format(formatter)
            }
        }
    }

    private fun getFilteredAndSortedFeed(
        filter: ActivityFilter,
        collapsed: Set<String> = (_viewData.value as? Result.Success)?.data?.collapsedHeaders ?: emptySet()
    ): List<HomeFeedItem> {
        val filteredNappies = if (filter == ActivityFilter.FEEDING) emptyList() else cachedNappies
        val filteredFeedings = if (filter == ActivityFilter.NAPPY) emptyList() else cachedFeedings

        // Pass the collapsed set directly to your mapping builder block layout
        return combineAndSort(filteredNappies, filteredFeedings, collapsed)
    }

    fun toggleActivityFilter(filter: ActivityFilter) {
        val currentResult = _viewData.value
        if (currentResult !is Result.Success) return

        // If clicking an already active filter, clear it back to NONE
        val newFilterSetting = if (currentResult.data.currentFilter == filter) {
            ActivityFilter.NONE
        } else {
            filter
        }

        _viewData.update {
            Result.Success(
                currentResult.data.copy(
                    activityFeed = getFilteredAndSortedFeed(newFilterSetting), // 🔄 Re-computes list
                    currentFilter = newFilterSetting
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

    // 🛠️ 1. Main interaction endpoint called when tapping a sticky header row
    fun toggleHeaderCollapse(headerTitle: String) {
        val currentResult = _viewData.value
        if (currentResult !is Result.Success) return

        val currentCollapsed = currentResult.data.collapsedHeaders
        val updatedCollapsed = if (currentCollapsed.contains(headerTitle)) {
            currentCollapsed - headerTitle // Expand
        } else {
            currentCollapsed + headerTitle // Collapse
        }

        _viewData.update {
            Result.Success(
                currentResult.data.copy(
                    collapsedHeaders = updatedCollapsed,
                    // Re-evaluate list compilation passing the updated state rules configuration
                    activityFeed = getFilteredAndSortedFeed(currentResult.data.currentFilter, updatedCollapsed)
                )
            )
        }
    }
}
