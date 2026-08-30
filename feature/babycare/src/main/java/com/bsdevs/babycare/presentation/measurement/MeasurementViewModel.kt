package com.bsdevs.babycare.presentation.measurement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.presentation.navigation.MeasurementRoute
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.common.DispatcherProvider
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
class MeasurementViewModel @Inject constructor(
    private val accountService: AccountService,
    private val repository: BabyCareRepository,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<MeasurementRoute>()

    private val _uiState = MutableStateFlow(MeasurementUiState())
    val uiState: StateFlow<MeasurementUiState> = _uiState.asStateFlow()

    private val _events = Channel<MeasurementEvent>()
    val events = _events.receiveAsFlow()

    init {
        // Observe all measurements for the charts
        viewModelScope.launch {
            repository.measurements.collect { allMeasurements ->
                _uiState.update { it.copy(allMeasurements = allMeasurements.map { event ->
                    com.bsdevs.babycare.network.MeasurementDto(
                        id = event.id,
                        date = event.dateTimeString.split(" ").first(),
                        time = event.time,
                        dateTime = event.dateTimeString,
                        height = event.height,
                        weight = event.weight,
                        isMedical = event.isMedical ?: false,
                        comment = event.comment
                    )
                }) }
            }
        }

        route.activityId?.let { id ->
            loadMeasurement(id)
        }
    }

    private fun loadMeasurement(id: String) {
        val userId = accountService.currentUserId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val event = repository.getMeasurementEventById(userId, id)

                if (event != null && event.type == "MEASUREMENT") {
                    _uiState.update {
                        it.copy(
                            id = event.id,
                            date = event.dateTimeString.split(" ").firstOrNull() ?: it.date,
                            time = event.time,
                            height = event.height,
                            weight = event.weight,
                            recordHeight = event.height != null,
                            recordWeight = event.weight != null,
                            isMedical = event.isMedical ?: false,
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

    fun onDateSelected(date: String) {
        _uiState.update { it.copy(date = date) }
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        _uiState.update { it.copy(time = formattedTime) }
    }

    fun onHeightChanged(heightValue: Int) {
        _uiState.update { it.copy(height = heightValue.toDouble() / 10.0, recordHeight = true) }
    }

    fun onWeightChanged(weightValue: Int) {
        _uiState.update { it.copy(weight = weightValue.toDouble() / 100.0, recordWeight = true) }
    }

    fun toggleRecordHeight(enabled: Boolean) {
        _uiState.update { it.copy(recordHeight = enabled) }
    }

    fun toggleRecordWeight(enabled: Boolean) {
        _uiState.update { it.copy(recordWeight = enabled) }
    }

    fun onIsMedicalChanged(isMedical: Boolean) {
        _uiState.update { it.copy(isMedical = isMedical) }
    }

    fun onCommentChanged(comment: String) {
        _uiState.update { it.copy(comment = comment) }
    }

    fun toggleMedicalOnly(medicalOnly: Boolean) {
        _uiState.update { it.copy(showMedicalOnly = medicalOnly) }
    }

    fun setShowSheet(show: Boolean) {
        _uiState.update { it.copy(showSheet = show) }
    }

    fun setShowTimePicker(show: Boolean) {
        _uiState.update { it.copy(showTimePicker = show) }
    }

    fun setShowDatePicker(show: Boolean) {
        _uiState.update { it.copy(showDatePicker = show) }
    }

    fun setShowDeleteConfirmation(show: Boolean) {
        _uiState.update { it.copy(showDeleteConfirmation = show) }
    }

    fun resetForm() {
        _uiState.update {
            it.copy(
                id = null,
                date = java.time.LocalDate.now().toString(),
                time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                height = null,
                weight = null,
                recordHeight = false,
                recordWeight = false,
                isMedical = false,
                comment = "",
                error = null,
                showSheet = true
            )
        }
    }

    fun onEditMeasurement(id: String) {
        _uiState.update { it.copy(showSheet = true) }
        loadMeasurement(id)
    }

    fun submitMeasurement() {
        val currentState = _uiState.value
        
        if (!currentState.recordHeight && !currentState.recordWeight) {
            _uiState.update { it.copy(error = "Please record at least height or weight") }
            return
        }

        val userId = accountService.currentUserId
        val measurementId = currentState.id ?: UUID.randomUUID().toString()

        val unifiedEvent = UnifiedEventDto(
            id = measurementId,
            type = "MEASUREMENT",
            time = currentState.time,
            dateTimeString = "${currentState.date} ${currentState.time}",
            comment = currentState.comment.trim().takeIf { it.isNotEmpty() },
            height = if (currentState.recordHeight) (currentState.height ?: 50.0) else null,
            weight = if (currentState.recordWeight) (currentState.weight ?: 3.5) else null,
            isMedical = currentState.isMedical
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                if (currentState.id != null) {
                    repository.updateActivityEvent(userId, currentState.date, currentState.id, unifiedEvent)
                } else {
                    repository.saveActivityEvent(userId, currentState.date, unifiedEvent)
                }
                _uiState.update { it.copy(isLoading = false) }
                _events.send(MeasurementEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun deleteMeasurement() {
        val currentState = _uiState.value
        val userId = accountService.currentUserId
        val eventId = currentState.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.deleteActivityEvent(userId, currentState.date, eventId)
                _uiState.update { it.copy(isLoading = false) }
                _events.send(MeasurementEvent.DeleteSuccess)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
