package com.bsdevs.babycare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
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
    val leftDuration: Long = 0,
    val rightDuration: Long = 0,
    val bottleAmountMl: Int? = null,
    val isLeftRunning: Boolean = false,
    val isRightRunning: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class FeedingEvent {
    object SaveSuccess : FeedingEvent()
    data class SaveError(val message: String) : FeedingEvent()
}

@HiltViewModel
class FeedingViewModel @Inject constructor(
    private val accountService: AccountService
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedingUiState())
    val uiState: StateFlow<FeedingUiState> = _uiState.asStateFlow()

    private val _events = Channel<FeedingEvent>()
    val events = _events.receiveAsFlow()

    private var leftTimerJob: Job? = null
    private var rightTimerJob: Job? = null

    private var leftStartTime: Long? = null
    private var rightStartTime: Long? = null
    private var leftBaseDuration: Long = 0
    private var rightBaseDuration: Long = 0

    fun toggleLeftTimer() {
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
        val now = LocalDate.now()
        val timeNow = LocalTime.now()

        val mainFeedingSide = when {
            currentState.bottleAmountMl != null -> "Bottle"
            currentState.leftDuration > currentState.rightDuration -> "Left"
            currentState.rightDuration > currentState.leftDuration -> "Right"
            currentState.leftDuration > 0 -> "Both"
            else -> null
        }

        val feeding = FeedingDto(
            id = UUID.randomUUID().toString(),
            userId = userId,
            date = now.toString(),
            startTime = timeNow.format(DateTimeFormatter.ofPattern("HH:mm")),
            leftDuration = currentState.leftDuration,
            rightDuration = currentState.rightDuration,
            totalDuration = currentState.leftDuration + currentState.rightDuration,
            mainFeedingSide = mainFeedingSide,
            bottleAmountMl = currentState.bottleAmountMl
        )
        
        val currentDateTime = "${now} ${timeNow.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                FirebaseFirestore.getInstance().collection("feedings")
                    .document(userId).collection("records")
                    .document(currentDateTime)
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
