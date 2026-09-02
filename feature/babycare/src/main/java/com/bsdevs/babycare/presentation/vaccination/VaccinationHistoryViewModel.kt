package com.bsdevs.babycare.presentation.vaccination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.VaccinationDto
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class VaccinationGroup(
    val seriesId: String?,
    val vaccinations: List<VaccinationDto>
)

data class VaccinationHistoryUiState(
    val dynamicUi: Result<List<NetworkScreenData>> = Result.Loading,
    val groupedVaccinations: List<VaccinationGroup> = emptyList(),
    val isRefreshing: Boolean = false
)

@HiltViewModel
class VaccinationHistoryViewModel @Inject constructor(
    private val accountService: AccountService,
    private val repository: BabyCareRepository,
    private val screenRepository: ScreenRepository,
    private val mapper: ScreenDataMapper,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<VaccinationHistoryUiState> = flow {
        val screenFlow = screenRepository.getScreenFlow("vaccination_history")
        
        emitAll(combine(
            screenFlow,
            repository.vaccinations,
            _isRefreshing
        ) { screenResult, allVaccinations, refreshing ->
            val mappedUi = when (screenResult) {
                is Result.Success -> Result.Success(withContext(dispatchers.default) { mapper.mapToData(screenResult.data) })
                is Result.Error -> Result.Error(screenResult.exception)
                Result.Loading -> Result.Loading
            }

            val grouped = allVaccinations.map { event ->
                VaccinationDto(
                    id = event.id,
                    date = event.dateTimeString.split(" ").first(),
                    time = event.time,
                    dateTime = event.dateTimeString,
                    vaccinationNames = event.vaccinationNames ?: emptyList(),
                    location = event.location,
                    seriesId = event.seriesId,
                    comment = event.comment
                )
            }.groupBy { it.seriesId }
                .map { (seriesId, vaccines) -> 
                    VaccinationGroup(seriesId, vaccines.sortedBy { it.dateTime }) 
                }.sortedByDescending { it.vaccinations.lastOrNull()?.dateTime ?: "" }

            VaccinationHistoryUiState(
                dynamicUi = mappedUi,
                groupedVaccinations = grouped,
                isRefreshing = refreshing
            )
        })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VaccinationHistoryUiState()
    )

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.loadInitialData(accountService.currentUserId, 20, forceRefresh = true)
            _isRefreshing.value = false
        }
    }

    fun deleteVaccination(date: String, id: String) {
        viewModelScope.launch {
            repository.deleteActivityEvent(accountService.currentUserId, date, id)
        }
    }
}
