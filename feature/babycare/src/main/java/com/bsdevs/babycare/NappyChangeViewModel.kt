package com.bsdevs.babycare

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.navigation.NappyChangeRoute
import com.bsdevs.babycare.network.NappyChangeDto
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
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

data class NappyChangeUiState(
    val id: String? = null,
    val originalDocId: String? = null,
    val date: String = LocalDate.now().toString(),
    val time: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val type: String = "Wet",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class NappyChangeEvent {
    object SaveSuccess : NappyChangeEvent()
    data class SaveError(val message: String) : NappyChangeEvent()
}

@HiltViewModel
class NappyChangeViewModel @Inject constructor(
    private val accountService: AccountService,
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
                val snapshot = FirebaseFirestore.getInstance().collection("nappyChanges")
                    .document(userId).collection("changes")
                    .whereEqualTo("id", id)
                    .get()
                    .await()
                
                val doc = snapshot.documents.firstOrNull()
                val nappy = doc?.toObject(NappyChangeDto::class.java)
                if (nappy != null) {
                    _uiState.update { 
                        it.copy(
                            id = nappy.id,
                            originalDocId = doc.id,
                            date = nappy.date ?: it.date,
                            time = nappy.time ?: it.time,
                            type = nappy.type ?: it.type,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun onTimeChanged(newTime: String) {
        _uiState.update { it.copy(time = newTime) }
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
        
        // Ensure we have a valid UUID. If the current ID is null or a legacy timestamp string, generate a new UUID.
        val isCurrentIdUuid = try {
            currentState.id?.let { UUID.fromString(it) } != null
        } catch (e: Exception) {
            false
        }
        
        val nappyId = if (isCurrentIdUuid) currentState.id!! else UUID.randomUUID().toString()
        val combinedDateTime = "${currentState.date} ${currentState.time}"
        val nappyChange = NappyChangeDto(
            id = nappyId,
            userId = userId,
            date = currentState.date,
            time = currentState.time,
            dateTime = combinedDateTime,
            type = currentState.type
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val db = FirebaseFirestore.getInstance()
                val collection = db.collection("nappyChanges")
                    .document(userId).collection("changes")
                
                // MIGRATION LOGIC:
                // If we are editing an existing record (originalDocId exists) 
                // AND that document was stored under a different ID (like the old timestamp string),
                // we delete the old document to prevent duplicates.
                if (currentState.originalDocId != null && currentState.originalDocId != nappyId) {
                    collection.document(currentState.originalDocId).delete().await()
                }

                // Save the record using the UUID as the document ID
                collection.document(nappyId)
                    .set(nappyChange)
                    .await()
                
                _events.send(NappyChangeEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                _events.send(NappyChangeEvent.SaveError(e.message ?: "Unknown error"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
