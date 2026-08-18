package com.bsdevs.babycare

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.navigation.FeedingRoute
import com.bsdevs.babycare.network.FeedingDto
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class FeedingUiState(
    val id: String? = null,
    val originalDocId: String? = null,
    val date: String = LocalDate.now().toString(),
    val startTime: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val leftDuration: Long = 0,
    val rightDuration: Long = 0,
    val bottleAmountMl: Int? = null,
    val isLeftRunning: Boolean = false,
    val isRightRunning: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class FeedingSide { LEFT, RIGHT }

sealed class FeedingEvent {
    object SaveSuccess : FeedingEvent()
    data class SaveError(val message: String) : FeedingEvent()
}

@HiltViewModel
class FeedingViewModel @Inject constructor(
    private val accountService: AccountService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<FeedingRoute>()

    private val _uiState = MutableStateFlow(FeedingUiState())
    val uiState: StateFlow<FeedingUiState> = _uiState.asStateFlow()

    private val _events = Channel<FeedingEvent>()
    val events = _events.receiveAsFlow()

    init {
        route.activityId?.let { id ->
            // 🔄 ONLY load from network if we haven't started tracking or loaded yet
            if (_uiState.value.id == null && !_uiState.value.isLoading) {
                loadFeeding(id)
            }
        }
    }

    private fun loadFeeding(id: String) {
        val userId = accountService.currentUserId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val snapshot = FirebaseFirestore.getInstance().collection("feedings")
                    .document(userId).collection("records")
                    .whereEqualTo("id", id)
                    .get()
                    .await()

                val doc = snapshot.documents.firstOrNull()
                val feeding = doc?.toObject(FeedingDto::class.java)
                if (feeding != null) {
                    _uiState.update {
                        it.copy(
                            id = feeding.id,
                            originalDocId = doc.id,
                            date = feeding.date ?: it.date,
                            startTime = feeding.startTime ?: it.startTime,
                            bottleAmountMl = feeding.bottleAmountMl,
                            leftDuration = feeding.leftDuration,
                            rightDuration = feeding.rightDuration,
                            isLoading = false
                        )
                    }
                    // Seed your commonised base duration maps cleanly
                    baseDurations[FeedingSide.LEFT] = feeding.leftDuration
                    baseDurations[FeedingSide.RIGHT] = feeding.rightDuration
                } else {
                    // If it fails to find the doc, turn off the loading flag safely
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    // 🗃️ Store tracking details in an array or map indexed by the side
    private val timerJobs = mutableMapOf<FeedingSide, Job?>()
    private val baseDurations = mutableMapOf<FeedingSide, Long>().withDefault { 0L }

    fun toggleTimer(side: FeedingSide) {
        val isRunning = when (side) {
            FeedingSide.LEFT -> _uiState.value.isLeftRunning
            FeedingSide.RIGHT -> _uiState.value.isRightRunning
        }

        if (isRunning) {
            pauseTimer(side)
        } else {
            startTimer(side)
        }
    }

    private fun startTimer(side: FeedingSide) {
        val sessionStartTime = SystemClock.elapsedRealtime()
        val previousAccumulatedDuration = baseDurations.getValue(side)

        // Update the running state flag dynamically
        _uiState.update { state ->
            when (side) {
                FeedingSide.LEFT -> state.copy(isLeftRunning = true)
                FeedingSide.RIGHT -> state.copy(isRightRunning = true)
            }
        }

        // Launch a single unified timer loop
        timerJobs[side] = viewModelScope.launch {
            while (true) {
                val actualSecondsElapsed = (SystemClock.elapsedRealtime() - sessionStartTime) / 1000
                val totalDuration = previousAccumulatedDuration + actualSecondsElapsed

                _uiState.update { state ->
                    when (side) {
                        FeedingSide.LEFT -> state.copy(leftDuration = totalDuration)
                        FeedingSide.RIGHT -> state.copy(rightDuration = totalDuration)
                    }
                }
                delay(1000L)
            }
        }
    }

    private fun pauseTimer(side: FeedingSide) {
        timerJobs[side]?.cancel()
        timerJobs[side] = null

        // Lock in the precise duration up to this millisecond
        val finalDuration = when (side) {
            FeedingSide.LEFT -> _uiState.value.leftDuration
            FeedingSide.RIGHT -> _uiState.value.rightDuration
        }
        baseDurations[side] = finalDuration

        // Flip the running state flag off
        _uiState.update { state ->
            when (side) {
                FeedingSide.LEFT -> state.copy(isLeftRunning = false)
                FeedingSide.RIGHT -> state.copy(isRightRunning = false)
            }
        }
    }

    // 📥 Make sure to update your existing duration setter endpoints too:
    fun onLeftDurationChanged(duration: Long) {
        baseDurations[FeedingSide.LEFT] = duration
        _uiState.update { it.copy(leftDuration = duration) }
    }

    fun onRightDurationChanged(duration: Long) {
        baseDurations[FeedingSide.RIGHT] = duration
        _uiState.update { it.copy(rightDuration = duration) }
    }

    private var leftTimerJob: Job? = null
    private var rightTimerJob: Job? = null

    private var leftStartTime: Long? = null
    private var rightStartTime: Long? = null
    private var leftBaseDuration: Long = 0
    private var rightBaseDuration: Long = 0

    fun onStartTimeSelected(hour: Int, minute: Int) {
        val formattedTime = String.format("%02d:%02d", hour, minute)
        _uiState.update { it.copy(startTime = formattedTime) }
    }

    fun toggleLeftTimer() {
// ...
        if (_uiState.value.isLeftRunning) {
            pauseLeftTimer()
        } else {
            startLeftTimer()
        }
    }

    fun toggleRightTimer() {
        if (_uiState.value.isRightRunning) {
            pauseRightTimer()
        } else {
            startRightTimer()
        }
    }

    private fun startLeftTimer() {
        leftStartTime = System.currentTimeMillis()
        _uiState.update { it.copy(isLeftRunning = true) }
        leftTimerJob = viewModelScope.launch {
            while (true) {
                val elapsed = (System.currentTimeMillis() - (leftStartTime ?: 0L)) / 1000
                _uiState.update { it.copy(leftDuration = leftBaseDuration + elapsed) }
                delay(1000L)
            }
        }
    }

    private fun pauseLeftTimer() {
        leftTimerJob?.cancel()
        leftBaseDuration = _uiState.value.leftDuration
        leftStartTime = null
        _uiState.update { it.copy(isLeftRunning = false) }
    }

    private fun startRightTimer() {
        rightStartTime = System.currentTimeMillis()
        _uiState.update { it.copy(isRightRunning = true) }
        rightTimerJob = viewModelScope.launch {
            while (true) {
                val elapsed = (System.currentTimeMillis() - (rightStartTime ?: 0L)) / 1000
                _uiState.update { it.copy(rightDuration = rightBaseDuration + elapsed) }
                delay(1000L)
            }
        }
    }

    private fun pauseRightTimer() {
        rightTimerJob?.cancel()
        rightBaseDuration = _uiState.value.rightDuration
        rightStartTime = null
        _uiState.update { it.copy(isRightRunning = false) }
    }

    fun updateBottleAmount(amount: Int?) {
        _uiState.update { it.copy(bottleAmountMl = amount) }
    }

    fun submitFeeding() {
        val currentState = _uiState.value
        val userId = accountService.currentUserId
        
        val mainFeedingSide = when {
            currentState.bottleAmountMl != null -> "Bottle"
            currentState.leftDuration > currentState.rightDuration -> "Left"
            currentState.rightDuration > currentState.leftDuration -> "Right"
            currentState.leftDuration > 0 -> "Both"
            else -> null
        }

        // Ensure we have a valid UUID. If the current ID is null or a legacy timestamp string, generate a new UUID.
        val isCurrentIdUuid = try {
            currentState.id?.let { UUID.fromString(it) } != null
        } catch (e: Exception) {
            false
        }
        
        val feedingId = if (isCurrentIdUuid) currentState.id!! else UUID.randomUUID().toString()

        val feeding = FeedingDto(
            id = feedingId,
            userId = userId,
            date = currentState.date,
            startTime = currentState.startTime,
            leftDuration = currentState.leftDuration,
            rightDuration = currentState.rightDuration,
            totalDuration = currentState.leftDuration + currentState.rightDuration,
            mainFeedingSide = mainFeedingSide,
            bottleAmountMl = currentState.bottleAmountMl
        )
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val db = FirebaseFirestore.getInstance()
                val collection = db.collection("feedings")
                    .document(userId).collection("records")
                
                // MIGRATION LOGIC:
                // If we are editing an existing record (originalDocId exists) 
                // AND that document was stored under a different ID (like the old timestamp string),
                // we delete the old document to prevent duplicates.
                if (currentState.originalDocId != null && currentState.originalDocId != feedingId) {
                    collection.document(currentState.originalDocId).delete().await()
                }

                // Save the record using the UUID as the document ID
                collection.document(feedingId)
                    .set(feeding)
                    .await()
                
                _events.send(FeedingEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                _events.send(FeedingEvent.SaveError(e.message ?: "Unknown error"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
