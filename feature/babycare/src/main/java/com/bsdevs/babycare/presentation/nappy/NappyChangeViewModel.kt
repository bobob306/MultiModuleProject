package com.bsdevs.babycare.presentation.nappy

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.presentation.navigation.NappyChangeRoute
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.common.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NappyChangeViewModel @Inject constructor(
    private val accountService: AccountService,
    private val repository: BabyCareRepository,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val activityIdArg: String? = savedStateHandle["activityId"]

    private val _localState = MutableStateFlow(
        NappyChangeUiState(
            date = repository.getCurrentDate().toString(),
            time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        )
    )
    val uiState: StateFlow<NappyChangeUiState> = _localState.asStateFlow()

    private val _events = Channel<NappyChangeEvent>()
    val events = _events.receiveAsFlow()

    init {
        activityIdArg?.let { id ->
            loadNappyChange(id)
        }
    }

    private fun loadNappyChange(id: String) {
        val userId = accountService.currentUserId
        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true) }
            try {
                // 🔄 Query data safely through the repository layer instead of legacy flat collections
                val nappyEvent = repository.getNappyEventById(userId, id)

                if (nappyEvent != null) {
                    val extractedDate = nappyEvent.dateTimeString.split(" ").firstOrNull() ?: _localState.value.date

                    _localState.update {
                        it.copy(
                            id = nappyEvent.id,
                            date = extractedDate,
                            time = nappyEvent.time,
                            // Map the nested custom variant string parameter safely straight back into your radio choice state
                            type = nappyEvent.nappyType ?: "",
                            isLoading = false,
                            comment = nappyEvent.comment ?: ""
                        )
                    }
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

    fun onTimeSelected(hour: Int, minute: Int) {
        val formattedTime = String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute)
        _localState.update { it.copy(time = formattedTime, error = null) }
    }

    fun onTypeChanged(newType: String) {
        _localState.update { it.copy(type = newType) }
    }

    fun setShowTimePicker(show: Boolean) {
        _localState.update { it.copy(showTimePicker = show) }
    }

    fun setShowDeleteConfirmation(show: Boolean) {
        _localState.update { it.copy(showDeleteConfirmation = show) }
    }

    fun setIsPlayingTurdAnimation(isPlaying: Boolean) {
        _localState.update { it.copy(isPlayingTurdAnimation = isPlaying) }
    }

    fun submitNappyChange() {
        val currentState = _localState.value
        val userId = accountService.currentUserId

        // Validate or generate an explicit unique event identifier string
        val isCurrentIdUuid = try {
            currentState.id?.let { UUID.fromString(it) } != null
        } catch (e: Exception) {
            false
        }
        val isEditing = !currentState.id.isNullOrEmpty()
        val nappyId = if (isCurrentIdUuid) currentState.id!! else UUID.randomUUID().toString()

        // ➕ Map your local parameters directly into our modern Unified Event payload schema
        val unifiedNappyEvent = UnifiedEventDto(
            id = nappyId,
            time = currentState.time,
            dateTimeString = "${currentState.date} ${currentState.time}",
            type = "NAPPY",
            nappyType = currentState.type, // e.g., "Wet", "Dirty", "Both"
            comment = currentState.comment.trim().takeIf { it.isNotEmpty() },
        )

        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true, error = null) }
            try {
                if (isEditing) {
                    // 🔄 Execute update transaction against the nested entry element
                    repository.updateActivityEvent(
                        userId = userId,
                        date = currentState.date,
                        eventId = nappyId,
                        updatedEvent = unifiedNappyEvent,
                    )
                } else {
                    // 🚀 Execute clean atomic insert push using arrayUnion tokens
                    repository.saveActivityEvent(
                        userId = userId,
                        date = currentState.date,
                        event = unifiedNappyEvent
                    )
                }

                _events.send(NappyChangeEvent.SaveSuccess)
            } catch (e: Exception) {
                _localState.update { it.copy(error = e.message, isLoading = false) }
                _events.send(NappyChangeEvent.SaveError(e.message ?: "Failed to save nappy record change."))
            }
        }
    }

    fun deleteNappyChange() {
        val currentState = _localState.value
        val userId = accountService.currentUserId
        val eventId = currentState.id ?: return
        val date = currentState.date

        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.deleteActivityEvent(userId, date, eventId)
                _events.send(NappyChangeEvent.DeleteSuccess)
            } catch (e: Exception) {
                _localState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
