package com.bsdevs.babycare.presentation.feeding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class FeedingViewModel @Inject constructor(
    private val accountService: AccountService,
    private val repository: BabyCareRepository,
    private val timerManager: FeedingTimerManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val activityIdArg: String? = savedStateHandle["activityId"]
    private val startSide: String? = savedStateHandle["startSide"]

    private val _localState = MutableStateFlow(FeedingUiState())
    
    val uiState: StateFlow<FeedingUiState> = combine(
        _localState,
        timerManager.timerState
    ) { local, timer ->
        local.copy(
            id = local.id ?: timer.activityId,
            leftDuration = timer.leftDuration,
            rightDuration = timer.rightDuration,
            isLeftRunning = timer.isLeftRunning,
            isRightRunning = timer.isRightRunning,
            startTime = timer.startTime ?: local.startTime,
            date = timer.date ?: local.date
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FeedingUiState()
    )

    private val _events = Channel<FeedingEvent>()
    val events = _events.receiveAsFlow()

    init {
        val currentTimerState = timerManager.timerState.value

        if (activityIdArg != null) {
            // 🔄 EDIT MODE: If the manager is holding a different activity, reset it first
            if (currentTimerState.activityId != activityIdArg) {
                timerManager.reset()
            }
            // Always load data for edit mode to populate local UI state (comment, bottle, etc.)
            loadFeeding(activityIdArg)
        } else {
            // 🆕 NEW FEED: If the manager is holding an "Edit" session, clear it for a fresh start.
            // Note: We don't reset if it's a "New" session (id == null) already in progress.
            if (currentTimerState.activityId != null) {
                timerManager.reset()
            }
        }
        
        startSide?.let { sideStr ->
            val side = when (sideStr.lowercase()) {
                "left" -> FeedingSide.LEFT
                "right" -> FeedingSide.RIGHT
                else -> null
            }
            side?.let { 
                timerManager.startTimer(it, activityIdArg)
                FeedingTimerService.start(context)
            }
        }
    }



    private fun loadFeeding(id: String) {
        val userId = accountService.currentUserId
        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true) }
            try {
                val feedingEvent = repository.getFeedingEventById(userId, id)

                if (feedingEvent != null) {
                    val extractedDate = feedingEvent.dateTimeString.split(" ").firstOrNull() ?: _localState.value.date

                    _localState.update {
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
                    
                    // 🌟 SYNC METADATA: When editing an existing feed, update the manager so notifications 
                    // and process restarts keep the correct historical start time.
                    timerManager.setSessionMetadata(feedingEvent.time, extractedDate)
                } else {
                    _localState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _localState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun onCommentChanged(newComment: String) {
        _localState.update { it.copy(comment = newComment) }
    }

    fun toggleTimer(side: FeedingSide) {
        val currentUi = uiState.value
        // 🌟 LOCK IN START TIME: Before starting the timer, push the current UI start time 
        // to the singleton manager if it hasn't been locked in yet.
        if (timerManager.timerState.value.startTime == null) {
            timerManager.setSessionMetadata(currentUi.startTime, currentUi.date)
        }

        timerManager.toggleTimer(side, currentUi.id)
        if (timerManager.isAnyTimerRunning()) {
            FeedingTimerService.start(context)
        }
    }

    fun onLeftDurationChanged(duration: Long) {
        timerManager.setDuration(FeedingSide.LEFT, duration, uiState.value.id)
    }

    fun onRightDurationChanged(duration: Long) {
        timerManager.setDuration(FeedingSide.RIGHT, duration, uiState.value.id)
    }

    fun onStartTimeSelected(hour: Int, minute: Int) {
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        _localState.update { it.copy(startTime = formattedTime) }
        
        // 🌟 UPDATE PERSISTENT TIMER STATE: If a timer is already running, ensure manual time 
        // changes are also locked into the singleton manager for persistence.
        if (timerManager.isAnyTimerRunning()) {
            timerManager.setSessionMetadata(formattedTime, uiState.value.date)
        }
    }

    fun updateBottleAmount(amount: Int?) {
        _localState.update { it.copy(bottleAmountMl = amount, showBottleDialog = false) }
    }

    fun setShowBottleDialog(show: Boolean) {
        _localState.update { it.copy(showBottleDialog = show) }
    }

    fun setShowTimePicker(show: Boolean) {
        _localState.update { it.copy(showTimePicker = show) }
    }

    fun setShowDurationDialog(side: String?) {
        _localState.update { it.copy(showDurationDialogForSide = side) }
    }

    fun setShowDeleteConfirmation(show: Boolean) {
        _localState.update { it.copy(showDeleteConfirmation = show) }
    }

    fun setShowCancelConfirmation(show: Boolean) {
        _localState.update { it.copy(showCancelConfirmation = show) }
    }

    fun setIsPlayingSplodge(isPlaying: Boolean) {
        _localState.update { it.copy(isPlayingSplodge = isPlaying) }
    }

    fun submitFeeding() {
        val currentState = uiState.value
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
            _localState.update { it.copy(isLoading = true, error = null) }
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
                _localState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun deleteFeeding() {
        val currentState = uiState.value
        val userId = accountService.currentUserId
        val eventId = currentState.id ?: return
        val date = currentState.date

        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.deleteActivityEvent(userId, date, eventId)
                timerManager.reset()
                _events.send(FeedingEvent.DeleteSuccess)
            } catch (e: Exception) {
                _localState.update { it.copy(error = e.message, isLoading = false) }
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
