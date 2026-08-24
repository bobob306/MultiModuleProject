package com.bsdevs.babycare.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.presentation.navigation.NappyChangeRoute
import com.bsdevs.babycare.network.NappyChangeDto
import com.bsdevs.babycare.network.UnifiedEventDto
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
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
import kotlin.reflect.typeOf

data class NappyChangeUiState(
    val id: String? = null,
    val originalDocId: String? = null,
    val date: String = LocalDate.now().toString(),
    val time: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val type: String = "Wet",
    val isLoading: Boolean = false,
    val comment: String = "",
    val error: String? = null,
)

sealed class NappyChangeEvent {
    object SaveSuccess : NappyChangeEvent()
    data class SaveError(val message: String) : NappyChangeEvent()
}

@HiltViewModel
class NappyChangeViewModel @Inject constructor(
    private val accountService: AccountService,
    private val repository: BabyCareRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<NappyChangeRoute>()

    private val _uiState = MutableStateFlow(NappyChangeUiState())
    val uiState: StateFlow<NappyChangeUiState> = _uiState.asStateFlow()

    private val _events = Channel<NappyChangeEvent>()
    val events = _events.receiveAsFlow()

    init {
        route.activityId?.let { id ->
            loadNappyChange(id)
        }
    }

    private fun loadNappyChange(id: String) {
        val userId = accountService.currentUserId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 🔄 Query data safely through the repository layer instead of legacy flat collections
                val nappyEvent = repository.getNappyEventById(userId, id)

                if (nappyEvent != null) {
                    val extractedDate = nappyEvent.dateTimeString.split(" ").firstOrNull() ?: _uiState.value.date

                    _uiState.update {
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

    fun onTimeSelected(hour: Int, minute: Int) {
        val formattedTime = String.format("%02d:%02d", hour, minute)
        _uiState.update { it.copy(time = formattedTime, error = null) }
    }

    fun onTypeChanged(newType: String) {
        _uiState.update { it.copy(type = newType) }
    }

    fun submitNappyChange() {
        val currentState = _uiState.value
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
            _uiState.update { it.copy(isLoading = true, error = null) }
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
                _uiState.update { it.copy(error = e.message, isLoading = false) }
                _events.send(NappyChangeEvent.SaveError(e.message ?: "Failed to save nappy record change."))
            }
        }
    }
}
