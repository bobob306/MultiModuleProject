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

data class BabyCareHomeViewData(
    val lastNappyChange: String? = null,
    val lastFeeding: String? = null,
    val activityFeed: List<BabyActivity> = emptyList(),
    val canLoadMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val currentFilter: ActivityFilter = ActivityFilter.NONE
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

                val combined = combineAndSort(nappyResult.first, feedingResult.first)

                _viewData.update {
                    Result.Success(
                        BabyCareHomeViewData(
                            lastNappyChange = nappyResult.first.firstOrNull()?.let { formatNappyChange(it) },
                            lastFeeding = feedingResult.first.firstOrNull()?.let { formatFeeding(it) },
                            activityFeed = combined,
                            canLoadMore = nappyResult.first.size >= pageSize || feedingResult.first.size >= pageSize,
                            currentFilter = ActivityFilter.NONE // 🌟 Explicitly default to un-filtered on clean boot
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
                            lastNappyChange = nappyResult.first.firstOrNull()?.let { formatNappyChange(it) },
                            lastFeeding = feedingResult.first.firstOrNull()?.let { formatFeeding(it) },
                            activityFeed = getFilteredAndSortedFeed(activeFilter), // 🔄 Commonised
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
                            activityFeed = getFilteredAndSortedFeed(activeFilter), // 🔄 Commonised
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

    private fun combineAndSort(nappies: List<NappyChangeDto>, feedings: List<FeedingDto>): List<BabyActivity> {
        val activities = nappies.map { BabyActivity.Nappy(it) } + feedings.map { BabyActivity.Feeding(it) }
        return activities.sortedByDescending { it.dateTimeString }
    }

    private suspend fun fetchNappyChangesBatch(userId: String, startAfter: DocumentSnapshot?): Pair<List<NappyChangeDto>, DocumentSnapshot?> {
        var query = Firebase.firestore.collection("nappyChanges").document(userId).collection("changes")
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

    private suspend fun fetchFeedingsBatch(userId: String, startAfter: DocumentSnapshot?): Pair<List<FeedingDto>, DocumentSnapshot?> {
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
        val date = try { LocalDate.parse(dateStr) } catch (_: Exception) { return null }
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

    private fun getFilteredAndSortedFeed(filter: ActivityFilter): List<BabyActivity> {
        val filteredNappies = if (filter == ActivityFilter.FEEDING) emptyList() else cachedNappies
        val filteredFeedings = if (filter == ActivityFilter.NAPPY) emptyList() else cachedFeedings
        return combineAndSort(filteredNappies, filteredFeedings)
    }


    // 🗃️ Cache memory fields to keep raw items safe during filtering shifts
    private var cachedNappies = listOf<NappyChangeDto>()
    private var cachedFeedings = listOf<FeedingDto>()

    // 🛠️ 1. Main filter toggle entry point called by clicking a Home UI card icon
    fun toggleActivityFilter(filter: ActivityFilter) {
        val currentResult = _viewData.value
        if (currentResult !is Result.Success) return

        val newFilter = if (currentResult.data.currentFilter == filter) ActivityFilter.NONE else filter

        _viewData.update {
            Result.Success(
                currentResult.data.copy(
                    activityFeed = getFilteredAndSortedFeed(newFilter), // 🔄 Commonised
                    currentFilter = newFilter
                )
            )
        }
    }

    // 🛠️ 2. Core filtering engine that combines items without hitting the network
    private fun updateUiFeedWithFilter(filter: ActivityFilter, currentData: BabyCareHomeViewData) {
        // Drop the collection components completely if the opposing filter option is selected
        val filteredNappies = if (filter == ActivityFilter.FEEDING) emptyList() else cachedNappies
        val filteredFeedings = if (filter == ActivityFilter.NAPPY) emptyList() else cachedFeedings

        val newlySortedFeed = combineAndSort(filteredNappies, filteredFeedings)

        _viewData.update {
            Result.Success(
                currentData.copy(
                    activityFeed = newlySortedFeed,
                    currentFilter = filter // Save the active filter flag inside your state layout
                )
            )
        }
    }
}
