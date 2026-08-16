package com.bsdevs.babycare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
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
    private val accountService: AccountService
) : ViewModel() {

    private val _uiState = MutableStateFlow(NappyChangeUiState())
    val uiState: StateFlow<NappyChangeUiState> = _uiState.asStateFlow()

    private val _events = Channel<NappyChangeEvent>()
    val events = _events.receiveAsFlow()

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
        
        val nappyChange = NappyChangeDto(
            id = UUID.randomUUID().toString(),
            userId = userId,
            date = currentState.date,
            time = currentState.time,
            type = currentState.type
        )
        val currentDateTime = "${currentState.date} ${currentState.time}"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val uploadNappy = FirebaseFirestore.getInstance().collection("nappyChanges")
                    .document(userId).collection("changes")
                    .document(currentDateTime)
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
