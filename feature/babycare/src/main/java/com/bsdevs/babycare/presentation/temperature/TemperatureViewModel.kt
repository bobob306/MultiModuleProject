package com.bsdevs.babycare.presentation.temperature

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.DailyLogDto
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.babycare.presentation.navigation.TemperatureRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TemperatureViewModel @Inject constructor(
    private val accountService: AccountService,
    private val repository: BabyCareRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<TemperatureRoute>()

    private val _uiState = MutableStateFlow(TemperatureUiState())
    val uiState: StateFlow<TemperatureUiState> = _uiState.asStateFlow()

    private val _events = Channel<TemperatureUiEffect>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.cachedDays.collect { dailyLogs ->
                updateHistory(dailyLogs)
            }
        }

        route.activityId?.let { id ->
            onEditTemperature(id)
        }
    }

    private fun updateHistory(dailyLogs: List<DailyLogDto>) {
        val allReadings = dailyLogs.flatMap { day ->
            day.events
                .filter { it.type == "TEMPERATURE" }
                .map { event ->
                    TemperatureItem(
                        id = event.id,
                        date = day.date,
                        time = event.time,
                        temperature = event.temperature ?: 37.0,
                        comment = event.comment
                    )
                }
        }

        val grouped = allReadings.groupBy { it.date }
        val sortedDates = grouped.keys.sortedDescending()

        _uiState.update { it.copy(
            dates = sortedDates,
            dailyReadings = grouped
        ) }
    }

    fun onEditTemperature(id: String) {
        val userId = accountService.currentUserId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val event = repository.getFeedingEventById(userId, id) // Repository uses unified lookup

                if (event != null && event.type == "TEMPERATURE") {
                    val extractedDate = event.dateTimeString.split(" ").firstOrNull() ?: _uiState.value.date
                    val temp = event.temperature ?: 37.0
                    val tempInt = (temp * 10).toInt()
                    _uiState.update {
                        it.copy(
                            id = event.id,
                            date = extractedDate,
                            time = event.time,
                            temperature = temp.toString(),
                            temperatureValue = tempInt,
                            comment = event.comment ?: "",
                            isLoading = false
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

    fun onTemperatureValueSelected(value: Int) {
        val tempDouble = value.toDouble() / 10.0
        _uiState.update { it.copy(temperatureValue = value, temperature = tempDouble.toString(), error = null) }
    }

    fun onCommentChanged(newComment: String) {
        _uiState.update { it.copy(comment = newComment) }
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        _uiState.update { it.copy(time = formattedTime, error = null) }
    }

    fun resetForm() {
        _uiState.update { current ->
            TemperatureUiState(
                dates = current.dates,
                dailyReadings = current.dailyReadings,
                isLoading = false
            )
        }
    }

    fun submitTemperature() {
        val currentState = _uiState.value
        val userId = accountService.currentUserId

        val tempValue = currentState.temperatureValue.toDouble() / 10.0

        val isEditing = !currentState.id.isNullOrEmpty()
        val temperatureId = currentState.id ?: UUID.randomUUID().toString()

        val unifiedEvent = UnifiedEventDto(
            id = temperatureId,
            time = currentState.time,
            dateTimeString = "${currentState.date} ${currentState.time}",
            type = "TEMPERATURE",
            temperature = tempValue,
            comment = currentState.comment.trim().takeIf { it.isNotEmpty() },
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                if (isEditing) {
                    repository.updateActivityEvent(
                        userId = userId,
                        date = currentState.date,
                        eventId = temperatureId,
                        updatedEvent = unifiedEvent,
                    )
                } else {
                    repository.saveActivityEvent(
                        userId = userId,
                        date = currentState.date,
                        event = unifiedEvent
                    )
                }
                _uiState.update { it.copy(isLoading = false) }
                _events.send(TemperatureUiEffect.SaveSuccess)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
                _events.send(TemperatureUiEffect.SaveError(e.message ?: "Failed to save temperature"))
            }
        }
    }

    fun deleteTemperature() {
        val currentState = _uiState.value
        val userId = accountService.currentUserId
        val eventId = currentState.id ?: return
        val date = currentState.date

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.deleteActivityEvent(userId, date, eventId)
                _uiState.update { it.copy(isLoading = false) }
                _events.send(TemperatureUiEffect.DeleteSuccess)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
