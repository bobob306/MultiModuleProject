package com.bsdevs.homescreen.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.homescreen.FormSeeds
import com.bsdevs.data.repository.FormRepository
import com.bsdevs.data.repository.MetadataRepository
import com.bsdevs.data.repository.ScreenRepository
import com.bsdevs.network.FormDtoMapper
import com.bsdevs.network.dto.AppMetadataDto
import com.bsdevs.network.dto.FormSchemaDto
import com.bsdevs.network.dto.ScreenDto
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
    private val metadataRepository: MetadataRepository,
    private val formMapper: FormDtoMapper,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeveloperMenuUiState())
    val uiState: StateFlow<DeveloperMenuUiState> = _uiState.asStateFlow()

    fun syncSduiConfigs() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isSeeding = true, error = null, seedSuccess = false) }
            try {
                val screens = mutableMapOf<String, List<ScreenDto>>()
                val forms = mutableMapOf<String, FormSchemaDto>()

                // Populate Forms
                forms["coffeeLog"] = formMapper.mapToDto(FormSeeds.coffeeLog)
                forms["nappyLog"] = formMapper.mapToDto(FormSeeds.nappyLog)
                forms["temperatureLog"] = formMapper.mapToDto(FormSeeds.temperatureLog)
                forms["measurementLog"] = formMapper.mapToDto(FormSeeds.measurementLog)
                forms["vaccinationLog"] = formMapper.mapToDto(FormSeeds.vaccinationLog)

                // Populate Screens
                screens["baby_home"] = getBabyHomeScreen()
                screens["measurement_screen"] = getMeasurementScreen()
                screens["vaccination_history"] = getVaccinationHistoryScreen()
                screens["temperature_screen"] = getTemperatureHistoryScreen()
                screens["analysis_screen"] = getAnalysisScreen()
                screens["shopping_list"] = getShoppingListScreen()

                val metadata = AppMetadataDto(screens = screens, forms = forms)
                metadataRepository.updateMetadata(metadata)

                // Individual deletions if still needed
                formRepository.deleteForm("sleepLog")
                screenRepository.deleteScreen("sleep_screen")

                _uiState.update { it.copy(isSeeding = false, seedSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSeeding = false, error = e.message ?: "Sync failed") }
            }
        }
    }

    private fun getMeasurementScreen(): List<ScreenDto> {
        return listOf(
            ScreenDto.GrowthChartDto(index = 0, title = "Weight Trend (kg)", dataType = "WEIGHT"),
            ScreenDto.GrowthChartDto(index = 1, title = "Height Trend (cm)", dataType = "HEIGHT"),
            ScreenDto.GrowthChartDto(index = 2, title = "Head Circumference (cm)", dataType = "HEAD"),
            ScreenDto.MeasurementHistoryDto(index = 3)
        )
    }

    private fun getBabyHomeScreen(): List<ScreenDto> {
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
                sharedElementKey = "tile_analysis",
                requiredRoles = listOf("parent", "admin")
            )
        )

        return listOf(
            ScreenDto.TileRowDto(index = 0, tiles = babyHomeTiles),
            ScreenDto.ActivityFeedDto(index = 1)
        )
    }

    private fun getVaccinationHistoryScreen(): List<ScreenDto> {
        return listOf(
            ScreenDto.VaccinationHistoryDto(index = 0)
        )
    }

    private fun getTemperatureHistoryScreen(): List<ScreenDto> {
        return listOf(
            ScreenDto.TemperatureChartDto(index = 0),
            ScreenDto.TemperatureHistoryDto(index = 1)
        )
    }

    private fun getAnalysisScreen(): List<ScreenDto> {
        return listOf(
            ScreenDto.FeedingFrequencyChartDto(index = 0),
            ScreenDto.FeedingGapChartDto(index = 1),
            ScreenDto.FeedingInsightCardDto(index = 2)
        )
    }

    private fun getShoppingListScreen(): List<ScreenDto> {
        return listOf(
            ScreenDto.ShoppingListDto(index = 0)
        )
    }
}
