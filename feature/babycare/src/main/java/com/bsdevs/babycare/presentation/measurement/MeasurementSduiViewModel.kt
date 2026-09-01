package com.bsdevs.babycare.presentation.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.MeasurementDto
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.ScreenDataMapper
import com.bsdevs.network.repository.ScreenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class MeasurementSduiUiState(
    val screenLayout: Result<List<NetworkScreenData>> = Result.Loading,
    val allMeasurements: List<MeasurementDto> = emptyList(),
    val showMedicalOnly: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class MeasurementSduiViewModel @Inject constructor(
    private val accountService: AccountService,
    private val repository: BabyCareRepository,
    private val screenRepository: ScreenRepository,
    private val mapper: ScreenDataMapper,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _showMedicalOnly = MutableStateFlow(false)
    private val _screenLayout = MutableStateFlow<Result<List<NetworkScreenData>>>(Result.Loading)

    val uiState: StateFlow<MeasurementSduiUiState> = combine(
        _screenLayout,
        repository.measurements,
        _showMedicalOnly
    ) { layout, allMeasurements, medicalOnly ->
        val mapped = allMeasurements.map { event ->
            MeasurementDto(
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
        MeasurementSduiUiState(
            screenLayout = layout,
            allMeasurements = mapped,
            showMedicalOnly = medicalOnly
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MeasurementSduiUiState()
    )

    init {
        loadScreenLayout()
    }

    fun loadScreenLayout() {
        viewModelScope.launch {
            screenRepository.getScreenFlow("measurement_screen").collect { result ->
                when (result) {
                    is Result.Success -> {
                        val mappedData = withContext(dispatchers.default) {
                            mapper.mapToData(result.data)
                        }
                        _screenLayout.update { Result.Success(mappedData) }
                    }
                    is Result.Error -> _screenLayout.update { Result.Error(result.exception) }
                    is Result.Loading -> _screenLayout.update { Result.Loading }
                }
            }
        }
    }

    fun toggleMedicalOnly(medicalOnly: Boolean) {
        _showMedicalOnly.value = medicalOnly
    }

    fun deleteMeasurement(measurement: MeasurementDto) {
        val userId = accountService.currentUserId
        val date = measurement.date ?: return
        val eventId = measurement.id ?: return

        viewModelScope.launch {
            try {
                repository.deleteActivityEvent(userId, date, eventId)
            } catch (e: Exception) {
                // Error handling could be added via an event channel if needed
            }
        }
    }
}
