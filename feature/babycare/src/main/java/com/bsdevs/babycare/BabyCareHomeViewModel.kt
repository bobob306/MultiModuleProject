package com.bsdevs.babycare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.network.NappyChangeDto
import com.bsdevs.common.result.Result
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val activityFeed: List<NappyChangeDto> = emptyList(),
    val canLoadMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false // 👈 Added parameter for top spinner control
)

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

    private var lastDocument: DocumentSnapshot? = null
    private val pageSize = 20L

    fun loadData() {
        viewModelScope.launch {
            _viewData.update { Result.Loading }
            lastDocument = null
            try {
                val userId = accountService.currentUserId
                val result = fetchNappyChangesBatch(userId, null)
                val nappyChanges = result.first
                lastDocument = result.second
                
                _viewData.update {
                    Result.Success(
                        BabyCareHomeViewData(
                            lastNappyChange = nappyChanges.firstOrNull()?.let { formatNappyChange(it) },
                            lastFeeding = null,
                            activityFeed = nappyChanges,
                            canLoadMore = nappyChanges.size >= pageSize
                        )
                    )
                }
            } catch (e: Exception) {
                _viewData.update { Result.Error(e) }
            }
        }
    }

    // 🚀 NEW: Pull-to-Refresh Background State Machine Worker
    fun refreshData() {
        val currentResult = _viewData.value
        // Guard check: Avoid refreshing if already loading, refreshing, or if there's a hard error
        if (currentResult !is Result.Success || currentResult.data.isRefreshing || currentResult.data.isLoadingMore) return

        viewModelScope.launch {
            // Turn on the refreshing wheel animation overlay while preserving old list visibility
            _viewData.update { Result.Success(currentResult.data.copy(isRefreshing = true)) }

            // Wipe pagination bookmark node index reference tracking state variables
            lastDocument = null

            try {
                val userId = accountService.currentUserId
                val result = fetchNappyChangesBatch(userId, null)
                val freshChanges = result.first
                lastDocument = result.second

                _viewData.update {
                    Result.Success(
                        BabyCareHomeViewData(
                            lastNappyChange = freshChanges.firstOrNull()?.let { formatNappyChange(it) },
                            lastFeeding = currentResult.data.lastFeeding, // Preserve state information
                            activityFeed = freshChanges, // Overwrite list with fresh database records
                            canLoadMore = freshChanges.size >= pageSize,
                            isRefreshing = false, // Stop the overlay animation
                            isLoadingMore = false
                        )
                    )
                }
                println("refreshed data successfully")

            } catch (e: Exception) {
                // Fall back gracefully to the prior persistent data array tree and turn off refresh spinner
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
                val result = fetchNappyChangesBatch(userId, lastDocument)
                val newChanges = result.first
                lastDocument = result.second
                
                _viewData.update {
                    Result.Success(
                        currentResult.data.copy(
                            activityFeed = currentResult.data.activityFeed + newChanges,
                            canLoadMore = newChanges.size >= pageSize,
                            isLoadingMore = false
                        )
                    )
                }
            } catch (e: Exception) {
                _viewData.update { Result.Success(currentResult.data.copy(isLoadingMore = false)) }
            }
        }
    }

    private suspend fun fetchNappyChangesBatch(userId: String, startAfter: DocumentSnapshot?): Pair<List<NappyChangeDto>, DocumentSnapshot?> {
        var query = Firebase.firestore.collection("nappyChanges")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .orderBy("time", Query.Direction.DESCENDING)
            .limit(pageSize)
        
        if (startAfter != null) {
            query = query.startAfter(startAfter)
        }
        
        val snapshot = query.get().await()
        val changes = snapshot.toObjects(NappyChangeDto::class.java)
        val lastDoc = snapshot.documents.lastOrNull()
        
        return Pair(changes, lastDoc)
    }

    private fun formatNappyChange(dto: NappyChangeDto): String? {
        val dateStr = dto.date ?: return null
        val timeStr = dto.time ?: ""
        
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
}
