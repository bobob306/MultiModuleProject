package com.bsdevs.coffeescreen.screens.detailscreen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bsdevs.authentication.AccountService
import com.bsdevs.coffeescreen.navigation.CoffeeDetailScreenRoute
import com.bsdevs.coffeescreen.network.CoffeeApiService
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.common.result.Result
import com.bsdevs.common.result.Result.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CoffeeDetailsViewData(
    val coffeeDto: CoffeeDto,
    val shotList: List<ShotDto>?,
    val showSheet: Boolean = false,
)

@HiltViewModel
class CoffeeDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountService: AccountService,
    private val apiService: CoffeeApiService
) : ViewModel() {
    
    private val detailsRoute: CoffeeDetailScreenRoute = savedStateHandle.toRoute()
    private val selectedCoffeeId: String = detailsRoute.coffeeId

    private val _viewData = MutableStateFlow<Result<CoffeeDetailsViewData>>(Loading)
    val viewData: StateFlow<Result<CoffeeDetailsViewData>> = _viewData.onStart {
        loadDataFromNetwork()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Loading
    )

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private suspend fun loadDataFromNetwork() {
        try {
            val currentUser = accountService.currentUserId
            val coffee = apiService.getCoffeeById(currentUser, selectedCoffeeId)
                ?: throw Exception("Coffee not found")

            val label = coffee.label ?: throw Exception("Coffee label missing")
            val shots = apiService.getShotsForCoffee(label)
            
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val sortedShots = shots.sortedByDescending { shotDto ->
                shotDto.date?.let { dateString ->
                    try {
                        LocalDate.parse(dateString, formatter)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            _viewData.value = Result.Success(
                data = CoffeeDetailsViewData(
                    coffeeDto = coffee,
                    shotList = sortedShots,
                )
            )
        } catch (e: Exception) {
            _viewData.value = Result.Error(e)
        }
    }

    fun processIntent(intent: CoffeeDetailsIntent) {
        when (intent) {
            CoffeeDetailsIntent.NavigateHome -> {
                viewModelScope.launch {
                    _navigationEvent.send(NavigationEvent.NavigateHome)
                }
            }

            is CoffeeDetailsIntent.ShowSheet -> {
                _viewData.update { current ->
                    if (current is Result.Success) {
                        Result.Success(current.data.copy(showSheet = true))
                    } else current
                }
            }

            is CoffeeDetailsIntent.HideSheet -> {
                _viewData.update { current ->
                    if (current is Result.Success) {
                        Result.Success(current.data.copy(showSheet = false))
                    } else current
                }
            }

            is CoffeeDetailsIntent.SubmitShot -> {
                viewModelScope.launch {
                    try {
                        val currentSuccess = _viewData.value as? Result.Success ?: return@launch
                        val coffee = currentSuccess.data.coffeeDto
                        val label = coffee.label ?: return@launch

                        _viewData.update { Loading }
                        
                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        val formattedDate = formatter.format(intent.shot.date)

                        val shotDto = ShotDto(
                            id = intent.shot.id,
                            date = formattedDate,
                            weightIn = intent.shot.weightInGrams.toDouble() / 10,
                            weightOut = intent.shot.weightOutGrams.toDouble() / 10,
                            time = intent.shot.timeInSeconds,
                            rating = intent.shot.rating,
                        )
                        
                        apiService.uploadShot(label, shotDto)
                        
                        val updatedShots = apiService.getShotsForCoffee(label)
                        val sortedShots = updatedShots.sortedByDescending { s ->
                            s.date?.let { 
                                try { LocalDate.parse(it, formatter) } catch(e: Exception) { null }
                            }
                        }

                        _viewData.update {
                            Result.Success(
                                data = CoffeeDetailsViewData(
                                    coffeeDto = coffee,
                                    shotList = sortedShots,
                                    showSheet = false
                                ),
                            )
                        }
                    } catch (e: Exception) {
                        println("error uploading shot ${e.message}")
                    }
                }
            }
        }
    }
}

data class ShotList(
    val shots: List<ShotDto>
)

data class ShotDto(
    val id: String? = null,
    val date: String? = null,
    val weightIn: Double? = null,
    val weightOut: Double? = null,
    val time: Int? = null,
    val rating: Int? = null,
)

sealed class CoffeeDetailsIntent {
    object NavigateHome : CoffeeDetailsIntent()
    data class SubmitShot(val shot: EspressoShotDetails) : CoffeeDetailsIntent()
    object ShowSheet : CoffeeDetailsIntent()
    object HideSheet : CoffeeDetailsIntent()
}

sealed class NavigationEvent {
    object NavigateHome : NavigationEvent()
}
