package com.bsdevs.babycare.presentation.measurement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MeasurementViewModel @Inject constructor(
    private val accountService: AccountService,
    private val repository: BabyCareRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val activityIdArg: String? = savedStateHandle["activityId"]

    private val _localState = MutableStateFlow(MeasurementUiState())
    
    val uiState: StateFlow<MeasurementUiState> = combine(
        _localState,
        repository.measurements
    ) { local, allMeasurements ->
        val mapped = allMeasurements.map { event ->
            com.bsdevs.babycare.network.MeasurementDto(
                id = event.id,
                date = event.dateTimeString.split(" ").first(),
                time = event.time,
                dateTime = event.dateTimeString,
                height = event.height,
                weight = event.weight,
                headCircumference = event.headCircumference,
                isMedical = event.isMedical ?: false,
                comment = event.comment
            )
        }

        val filtered = if (local.showMedicalOnly) {
            mapped.filter { it.isMedical }
        } else {
            mapped
        }

        local.copy(allMeasurements = filtered)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MeasurementUiState()
    )

    private val _events = Channel<MeasurementEvent>()
    val events = _events.receiveAsFlow()

    init {
        activityIdArg?.let { id ->
            loadMeasurement(id)
        }
        
        // Robustly load baby profile and birth date
        accountService.currentUser.onEach { authUser ->
            authUser?.id?.let { userId ->
                val user = userRepository.getUser(userId)
                user?.babyId?.let { babyId ->
                    val baby = userRepository.getBaby(babyId)
                    _localState.update { it.copy(
                        birthDate = baby?.effectiveBirthDate,
                        babyGender = baby?.gender
                    ) }
                }
            }
        }.launchIn(viewModelScope)

        // Also react to updates in the shared userProfile flow
        userRepository.userProfile.onEach { user ->
            user?.babyId?.let { babyId ->
                val baby = userRepository.getBaby(babyId)
                _localState.update { it.copy(
                    birthDate = baby?.effectiveBirthDate,
                    babyGender = baby?.gender
                ) }
            }
        }.launchIn(viewModelScope)
    }

    private fun loadMeasurement(id: String) {
        val userId = accountService.currentUserId
        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true) }
            try {
                val event = repository.getMeasurementEventById(userId, id)

                if (event != null && event.type == "MEASUREMENT") {
                    _localState.update {
                        it.copy(
                            id = event.id,
                            date = event.dateTimeString.split(" ").firstOrNull() ?: it.date,
                            time = event.time,
                            height = event.height,
                            weight = event.weight,
                            headCircumference = event.headCircumference,
                            recordHeight = event.height != null,
                            recordWeight = event.weight != null,
                            recordHeadCircumference = event.headCircumference != null,
                            isMedical = event.isMedical ?: false,
                            comment = event.comment ?: "",
                            isLoading = false
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

    fun onDateSelected(date: String) {
        _localState.update { it.copy(date = date) }
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        _localState.update { it.copy(time = formattedTime) }
    }

    fun onHeightChanged(heightValue: Int) {
        _localState.update { it.copy(height = heightValue.toDouble() / 10.0, recordHeight = true) }
    }

    fun onWeightChanged(weightValue: Int) {
        _localState.update { it.copy(weight = weightValue.toDouble() / 100.0, recordWeight = true) }
    }

    fun onHeadCircumferenceChanged(headValue: Int) {
        _localState.update { it.copy(headCircumference = headValue.toDouble() / 10.0, recordHeadCircumference = true) }
    }

    fun toggleRecordHeight(enabled: Boolean) {
        _localState.update { it.copy(recordHeight = enabled) }
    }

    fun toggleRecordWeight(enabled: Boolean) {
        _localState.update { it.copy(recordWeight = enabled) }
    }

    fun toggleRecordHeadCircumference(enabled: Boolean) {
        _localState.update { it.copy(recordHeadCircumference = enabled) }
    }

    fun onIsMedicalChanged(isMedical: Boolean) {
        _localState.update { it.copy(isMedical = isMedical) }
    }

    fun onCommentChanged(comment: String) {
        _localState.update { it.copy(comment = comment) }
    }

    fun toggleMedicalOnly(medicalOnly: Boolean) {
        _localState.update { it.copy(showMedicalOnly = medicalOnly) }
    }

    fun toggleWhoOverlay(show: Boolean) {
        _localState.update { it.copy(showWhoOverlay = show) }
    }

    fun setShowSheet(show: Boolean) {
        _localState.update { it.copy(showSheet = show) }
    }

    fun setShowTimePicker(show: Boolean) {
        _localState.update { it.copy(showTimePicker = show) }
    }

    fun setShowDatePicker(show: Boolean) {
        _localState.update { it.copy(showDatePicker = show) }
    }

    fun setShowDeleteConfirmation(show: Boolean) {
        _localState.update { it.copy(showDeleteConfirmation = show) }
    }

    fun resetForm() {
        _localState.update {
            it.copy(
                id = null,
                date = java.time.LocalDate.now().toString(),
                time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                height = null,
                weight = null,
                headCircumference = null,
                recordHeight = false,
                recordWeight = false,
                recordHeadCircumference = false,
                isMedical = false,
                comment = "",
                error = null,
                showSheet = true
            )
        }
    }

    fun onEditMeasurement(id: String) {
        _localState.update { it.copy(showSheet = true) }
        loadMeasurement(id)
    }

    fun submitMeasurement() {
        val currentState = uiState.value
        
        if (!currentState.recordHeight && !currentState.recordWeight && !currentState.recordHeadCircumference) {
            _localState.update { it.copy(error = "Please record at least height, weight or head circumference") }
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
            headCircumference = if (currentState.recordHeadCircumference) (currentState.headCircumference ?: 40.0) else null,
            isMedical = currentState.isMedical
        )

        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true, error = null) }
            try {
                if (currentState.id != null) {
                    repository.updateActivityEvent(userId, currentState.date, currentState.id, unifiedEvent)
                } else {
                    repository.saveActivityEvent(userId, currentState.date, unifiedEvent)
                }
                _localState.update { it.copy(isLoading = false) }
                _events.send(MeasurementEvent.SaveSuccess)
            } catch (e: Exception) {
                _localState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun deleteMeasurement() {
        val currentState = uiState.value
        val userId = accountService.currentUserId
        val eventId = currentState.id ?: return

        viewModelScope.launch {
            _localState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.deleteActivityEvent(userId, currentState.date, eventId)
                _localState.update { it.copy(isLoading = false) }
                _events.send(MeasurementEvent.DeleteSuccess)
            } catch (e: Exception) {
                _localState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun deleteMeasurement(id: String, date: String) {
        val userId = accountService.currentUserId
        viewModelScope.launch {
            try {
                repository.deleteActivityEvent(userId, date, id)
                _events.send(MeasurementEvent.DeleteSuccess)
            } catch (e: Exception) {
                _localState.update { it.copy(error = e.message) }
            }
        }
    }
}
