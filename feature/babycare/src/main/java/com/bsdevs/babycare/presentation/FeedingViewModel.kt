package com.bsdevs.babycare.presentation

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.presentation.navigation.FeedingRoute
import com.bsdevs.babycare.network.FeedingDto
import com.bsdevs.babycare.network.UnifiedEventDto
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
    private val repository: BabyCareRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<FeedingRoute>()

    private val _uiState = MutableStateFlow(FeedingUiState())
    val uiState: StateFlow<FeedingUiState> = _uiState.asStateFlow()

    private val _events = Channel<FeedingEvent>()
    val events = _events.receiveAsFlow()

    // 🗃️ Store tracking details in an array or map indexed by the side
    private val timerJobs = mutableMapOf<FeedingSide, Job?>()
    private val baseDurations = mutableMapOf<FeedingSide, Long>().withDefault { 0L }

    init {
        route.activityId?.let { id ->
            // 🔄 ONLY load from network if we haven't started tracking or loaded yet
            if (_uiState.value.id == null && !_uiState.value.isLoading) {
                loadFeeding(id)
            }
        }
        
        route.startSide?.let { sideStr ->
            val side = when (sideStr.lowercase()) {
                "left" -> FeedingSide.LEFT
                "right" -> FeedingSide.RIGHT
                else -> null
            }
            side?.let { startTimer(it) }
        }
    }

    private fun loadFeeding(id: String) {
        val userId = accountService.currentUserId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 🔄 FIXED: Query data safely through the unified repository layer instead of flat collections
                val feedingEvent = repository.getFeedingEventById(userId, id)

                if (feedingEvent != null) {
                    // Extract the date out of your YYYY-MM-DD space-split timestamp format layout string
                    val extractedDate = feedingEvent.dateTimeString.split(" ").firstOrNull() ?: _uiState.value.date

                    _uiState.update {
                        it.copy(
                            id = feedingEvent.id,
                            date = extractedDate,
                            startTime = feedingEvent.time,
                            bottleAmountMl = feedingEvent.bottleAmountMl,
                            leftDuration = feedingEvent.leftDuration,
                            rightDuration = feedingEvent.rightDuration,
                            isLoading = false
                        )
                    }
                    // Seed your commonised base duration maps cleanly
                    baseDurations[FeedingSide.LEFT] = feedingEvent.leftDuration
                    baseDurations[FeedingSide.RIGHT] = feedingEvent.rightDuration
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }


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

    fun onStartTimeSelected(hour: Int, minute: Int) {
        val formattedTime = String.format("%02d:%02d", hour, minute)
        _uiState.update { it.copy(startTime = formattedTime) }
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

        // Dynamic clean UUID validator parameter logic
        val isCurrentIdUuid = try {
            currentState.id?.let { UUID.fromString(it) } != null
        } catch (e: Exception) {
            false
        }
        val feedingId = if (isCurrentIdUuid) currentState.id!! else UUID.randomUUID().toString()

        // ➕ 1. Map your UI state values directly into a clean UnifiedEventDto payload instance
        val unifiedFeedingEvent = UnifiedEventDto(
            id = feedingId,
            type = "FEEDING",
            time = currentState.startTime,
            dateTimeString = "${currentState.date} ${currentState.startTime}",
            mainFeedingSide = mainFeedingSide,
            leftDuration = currentState.leftDuration,
            rightDuration = currentState.rightDuration,
            totalDuration = currentState.leftDuration + currentState.rightDuration,
            bottleAmountMl = currentState.bottleAmountMl
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Check if an entry ID already existed inside your state layer
                val isEditingExistingItem = !currentState.id.isNullOrEmpty()

                if (isEditingExistingItem) {
                    // 🔄 TRIGGER AN UPDATE TRANSACTION TASK
                    repository.updateActivityEvent(
                        userId = userId,
                        date = currentState.date,
                        eventId = currentState.id!!,
                        updatedEvent = unifiedFeedingEvent
                    )
                } else {
                    // 🚀 TRIGGER A STANDARD ATOMIC INSERT PUSH
                    repository.saveActivityEvent(
                        userId = userId,
                        date = currentState.date,
                        event = unifiedFeedingEvent
                    )
                }

                _events.send(FeedingEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
