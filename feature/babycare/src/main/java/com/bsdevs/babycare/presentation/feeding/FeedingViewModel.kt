package com.bsdevs.babycare.presentation.feeding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.presentation.navigation.FeedingRoute
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.common.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class FeedingViewModel @Inject constructor(
    private val accountService: AccountService,
    private val repository: BabyCareRepository,
    private val dispatchers: DispatcherProvider,
    private val timerManager: FeedingTimerManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<FeedingRoute>()

    private val _uiState = MutableStateFlow(FeedingUiState())
    val uiState: StateFlow<FeedingUiState> = _uiState.asStateFlow()

    private val _events = Channel<FeedingEvent>()
    val events = _events.receiveAsFlow()

    init {
        route.activityId?.let { id ->
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
            side?.let { 
                timerManager.startTimer(it, route.activityId)
                FeedingTimerService.start(context)
            }
        }

        observeTimer()
    }

    private fun observeTimer() {
        viewModelScope.launch {
            timerManager.timerState.collect { timerState ->
                _uiState.update { it.copy(
                    id = it.id ?: timerState.activityId,
                    leftDuration = timerState.leftDuration,
                    rightDuration = timerState.rightDuration,
                    isLeftRunning = timerState.isLeftRunning,
                    isRightRunning = timerState.isRightRunning
                )}
            }
        }
    }

    private fun loadFeeding(id: String) {
        val userId = accountService.currentUserId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val feedingEvent = repository.getFeedingEventById(userId, id)

                if (feedingEvent != null) {
                    val extractedDate = feedingEvent.dateTimeString.split(" ").firstOrNull() ?: _uiState.value.date

                    _uiState.update {
                        it.copy(
                            id = feedingEvent.id,
                            date = extractedDate,
                            startTime = feedingEvent.time,
                            bottleAmountMl = feedingEvent.bottleAmountMl,
                            leftDuration = feedingEvent.leftDuration,
                            rightDuration = feedingEvent.rightDuration,
                            isLoading = false,
                            comment = feedingEvent.comment ?: ""
                        )
                    }
                    timerManager.setDuration(FeedingSide.LEFT, feedingEvent.leftDuration, feedingEvent.id)
                    timerManager.setDuration(FeedingSide.RIGHT, feedingEvent.rightDuration, feedingEvent.id)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun onCommentChanged(newComment: String) {
        _uiState.update { it.copy(comment = newComment) }
    }

    fun toggleTimer(side: FeedingSide) {
        timerManager.toggleTimer(side, _uiState.value.id)
        if (timerManager.isAnyTimerRunning()) {
            FeedingTimerService.start(context)
        }
    }

    fun onLeftDurationChanged(duration: Long) {
        timerManager.setDuration(FeedingSide.LEFT, duration, _uiState.value.id)
    }

    fun onRightDurationChanged(duration: Long) {
        timerManager.setDuration(FeedingSide.RIGHT, duration, _uiState.value.id)
    }

    fun onStartTimeSelected(hour: Int, minute: Int) {
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        _uiState.update { it.copy(startTime = formattedTime) }
    }

    fun updateBottleAmount(amount: Int?) {
        _uiState.update { it.copy(bottleAmountMl = amount, showBottleDialog = false) }
    }

    fun setShowBottleDialog(show: Boolean) {
        _uiState.update { it.copy(showBottleDialog = show) }
    }

    fun setShowTimePicker(show: Boolean) {
        _uiState.update { it.copy(showTimePicker = show) }
    }

    fun setShowDurationDialog(side: String?) {
        _uiState.update { it.copy(showDurationDialogForSide = side) }
    }

    fun setShowDeleteConfirmation(show: Boolean) {
        _uiState.update { it.copy(showDeleteConfirmation = show) }
    }

    fun setShowCancelConfirmation(show: Boolean) {
        _uiState.update { it.copy(showCancelConfirmation = show) }
    }

    fun setIsPlayingSplodge(isPlaying: Boolean) {
        _uiState.update { it.copy(isPlayingSplodge = isPlaying) }
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

            // 🌟 ATTACH COMMENT: Trim whitespace and store as null if empty or blank
            comment = currentState.comment.trim().takeIf { it.isNotEmpty() },

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
                        eventId = currentState.id,
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

                timerManager.reset()
                _events.send(FeedingEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun deleteFeeding() {
        val currentState = _uiState.value
        val userId = accountService.currentUserId
        val eventId = currentState.id ?: return
        val date = currentState.date

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.deleteActivityEvent(userId, date, eventId)
                timerManager.reset()
                _events.send(FeedingEvent.DeleteSuccess)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun cancelFeeding() {
        timerManager.reset()
        viewModelScope.launch {
            _events.send(FeedingEvent.CancelSuccess)
        }
    }
}
