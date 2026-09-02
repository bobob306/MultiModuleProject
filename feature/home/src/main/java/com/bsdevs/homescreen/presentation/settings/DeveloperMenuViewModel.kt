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
                formRepository.updateForm("vaccinationLog", FormSeeds.vaccinationLog)

                // Sync Screens
                seedBabyHomeScreen()
                seedMeasurementScreen()
                seedVaccinationHistoryScreen()
                seedTemperatureHistoryScreen()
                seedAnalysisScreen()

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

    private suspend fun seedBabyHomeScreen() {
        val babyHomeTiles = listOf(
            ScreenDto.TileDto(
                index = 0,
                title = "Nappy",
                iconName = "ChildCare",
                destination = "babycare://nappy",
                subtitleType = "NAPPY",
                sharedElementKey = "tile_nappy"
            ),
            ScreenDto.TileDto(
                index = 1,
                title = "Feeding",
                iconName = "Restaurant",
                destination = "babycare://feeding",
                subtitleType = "FEEDING",
                sharedElementKey = "tile_feeding"
            ),
            ScreenDto.TileDto(
                index = 2,
                title = "Temperature",
                iconName = "Thermostat",
                destination = "babycare://temperature",
                subtitleType = "TEMPERATURE",
                sharedElementKey = "tile_temperature"
            ),
            ScreenDto.TileDto(
                index = 3,
                title = "Growth",
                iconName = "AutoGraph",
                destination = "babycare://measurement",
                subtitleType = "MEASUREMENT",
                sharedElementKey = "tile_measurement"
            ),
            ScreenDto.TileDto(
                index = 4,
                title = "Vaccination",
                iconName = "Vaccines",
                destination = "babycare://vaccination",
                subtitleType = "VACCINATION",
                sharedElementKey = "tile_vaccination"
            ),
            ScreenDto.TileDto(
                index = 5,
                title = "Analysis",
                iconName = "AutoGraph",
                destination = "babycare://graph",
                subtitleType = "ANALYSIS",
                sharedElementKey = "tile_analysis"
            )
        )

        val babyHomeScreen = listOf(
            ScreenDto.TileRowDto(index = 0, tiles = babyHomeTiles),
            ScreenDto.ActivityFeedDto(index = 1)
        )
        screenRepository.updateScreen("baby_home", babyHomeScreen)
    }

    private suspend fun seedVaccinationHistoryScreen() {
        val vaccinationHistoryScreen = listOf(
            ScreenDto.VaccinationHistoryDto(index = 0)
        )
        screenRepository.updateScreen("vaccination_history", vaccinationHistoryScreen)
    }

    private suspend fun seedTemperatureHistoryScreen() {
        val temperatureHistoryScreen = listOf(
            ScreenDto.TemperatureChartDto(index = 0),
            ScreenDto.TemperatureHistoryDto(index = 1)
        )
        screenRepository.updateScreen("temperature_screen", temperatureHistoryScreen)
    }

    private suspend fun seedAnalysisScreen() {
        val analysisScreen = listOf(
            ScreenDto.FeedingFrequencyChartDto(index = 0),
            ScreenDto.FeedingGapChartDto(index = 1),
            ScreenDto.FeedingInsightCardDto(index = 2)
        )
        screenRepository.updateScreen("analysis_screen", analysisScreen)
    }
}
