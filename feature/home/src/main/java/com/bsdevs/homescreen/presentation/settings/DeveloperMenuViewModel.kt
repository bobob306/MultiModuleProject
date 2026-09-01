package com.bsdevs.homescreen.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.homescreen.FormSeeds
import com.bsdevs.network.dto.ScreenDto
import com.bsdevs.network.repository.FormRepository
import com.bsdevs.network.repository.ScreenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeveloperMenuUiState(
    val isSeeding: Boolean = false,
    val seedSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DeveloperMenuViewModel @Inject constructor(
    private val formRepository: FormRepository,
    private val screenRepository: ScreenRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeveloperMenuUiState())
    val uiState: StateFlow<DeveloperMenuUiState> = _uiState.asStateFlow()

    fun syncSduiConfigs() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isSeeding = true, error = null, seedSuccess = false) }
            try {
                // Sync Forms
                formRepository.updateForm("coffeeLog", FormSeeds.coffeeLog)
                formRepository.updateForm("nappyLog", FormSeeds.nappyLog)
                formRepository.updateForm("temperatureLog", FormSeeds.temperatureLog)
                formRepository.updateForm("measurementLog", FormSeeds.measurementLog)

                // Sync Screens
                seedMeasurementScreen()

                _uiState.update { it.copy(isSeeding = false, seedSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSeeding = false, error = e.message ?: "Sync failed") }
            }
        }
    }

    private suspend fun seedMeasurementScreen() {
        val measurementScreen = listOf(
            ScreenDto.GrowthChartDto(index = 0, title = "Weight Trend (kg)", dataType = "WEIGHT"),
            ScreenDto.GrowthChartDto(index = 1, title = "Height Trend (cm)", dataType = "HEIGHT"),
            ScreenDto.GrowthChartDto(index = 2, title = "Head Circumference (cm)", dataType = "HEAD"),
            ScreenDto.MeasurementHistoryDto(index = 3)
        )
        screenRepository.updateScreen("measurement_screen", measurementScreen)
    }
}
