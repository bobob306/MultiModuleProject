package com.bsdevs.coffeescreen.screens.inputscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.Keep
import com.bsdevs.authentication.AccountService
import com.bsdevs.coffeescreen.network.CoffeeApiService
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.CoffeeScreenViewData
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.InputType
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.InputViewData.InputRadioVD
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.InputViewData.InputVD
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.RadioInputViewData
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.beanPreparationMethod
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.coffeeBeanTypes
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.coffeeRoasters
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.coffeeTastingNotesList
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.originCountries
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.common.result.Result.Success
import com.google.firebase.firestore.PropertyName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CoffeeInputScreenViewModel @Inject constructor(
    private val accountService: AccountService,
    private val apiService: CoffeeApiService,
    private val dispatchers: DispatcherProvider
) : ViewModel() {
    private val _viewData = MutableStateFlow<Result<CoffeeScreenViewData>>(value = Result.Loading)
    val viewData: StateFlow<Result<CoffeeScreenViewData>> = _viewData.onStart {
        loadDataFromNetwork()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = Result.Loading
    )

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow() // Expose as Flow

    val isButtonEnabled: StateFlow<Boolean> = viewData.map { currentResult ->
        if (currentResult is Success) {
            val viewData = currentResult.data
            val inputs = viewData.inputs

            // Check mutable sets (assuming BEANS, ORIGIN, TASTE are the ones)
            val areSetsValid = inputs.all { input ->
                when (input) {
                    is InputVD -> {
                        when (input.inputType) {
                            InputType.BEANS, InputType.ORIGIN, InputType.TASTE, InputType.METHOD, InputType.ROASTER -> input.selectedSet.isNotEmpty()
                        }
                    }

                    else -> true // Other input types (like InputRadioVD) are checked separately
                }
            }

            // Check the date field
            val isDateValid = viewData.roastDate != null
            
            _viewData.update {
                Success(
                    data = viewData.copy(
                        isButtonEnabled = areSetsValid && isDateValid
                    )
                )
            }
            areSetsValid && isDateValid
        } else {
            false
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    private suspend fun loadDataFromNetwork() {
        try {
            val data = apiService.getCoffeeInputScreenData()
            val viewData = CoffeeScreenViewData()
            val updatedViewData = viewData.copy(
                inputs = listOf(
                    InputVD(
                        label = "Coffee Type(s)",
                        inputList = data?.BEANS ?: coffeeBeanTypes,
                        selectedSet = emptySet(),
                        searchText = null,
                        inputType = InputType.BEANS
                    ),
                    InputVD(
                        label = "Coffee Origin(s)",
                        inputList = data?.ORIGIN ?: originCountries,
                        selectedSet = emptySet(),
                        searchText = null,
                        inputType = InputType.ORIGIN
                    ),
                    InputVD(
                        label = "Coffee Tasting Notes",
                        inputList = data?.TASTE ?: coffeeTastingNotesList,
                        selectedSet = emptySet(),
                        searchText = "",
                        inputType = InputType.TASTE
                    ),
                    InputVD(
                        label = "Coffee Preparation Method",
                        inputList = data?.METHOD ?: beanPreparationMethod,
                        selectedSet = emptySet(),
                        searchText = null,
                        inputType = InputType.METHOD,
                        singleInput = true,
                    ),
                    InputVD(
                        label = "Roaster",
                        inputList = data?.ROASTER ?: coffeeRoasters,
                        selectedSet = emptySet(),
                        searchText = "",
                        inputType = InputType.ROASTER,
                        singleInput = true,
                    ),
                    InputRadioVD(
                        label = "Decaf",
                        option = listOf(
                            RadioInputViewData(
                                label = "Caffeinated",
                                isDecaf = false,
                            ),
                            RadioInputViewData(
                                label = "Decaffeinated",
                                isDecaf = true,
                            )
                        ),
                        isDecaf = false,
                    ),
                ),
            )
            _viewData.value = Success(data = updatedViewData)
        } catch (e: Exception) {
            _viewData.value = Result.Error(e)
        }
    }

    fun processIntent(intent: CoffeeInputScreenIntent) {
        when (intent) {
            is CoffeeInputScreenIntent.UpdateRoastDate -> onUpdateRoastData(intent.date)
            is CoffeeInputScreenIntent.SetDecaf -> onToggleDecaf(intent.isDecaf)
            CoffeeInputScreenIntent.SubmitCoffee -> onEnterPress()
            is CoffeeInputScreenIntent.ToggleDropdownSelection -> handleToggleDropdownSelection(intent.inputType, intent.selection)
            is CoffeeInputScreenIntent.UpdateSearchText -> handleUpdateSearchText(intent.inputType, intent.searchText)
            CoffeeInputScreenIntent.NavigateHome -> {
                viewModelScope.launch {
                    _navigationEvent.send(NavigationEvent.NavigateToHome)
                }
            }
        }
    }

    fun onUpdateRoastData(date: LocalDate) {
        val currentViewData = _viewData.value as? Success<CoffeeScreenViewData> ?: return
        _viewData.update {
            Success(data = currentViewData.data.copy(roastDate = date))
        }
    }

    fun onToggleDecaf(isDecaf: Boolean) {
        _viewData.update { currentResult ->
            if (currentResult is Success) {
                val currentViewData = currentResult.data
                val updatedInputs = currentViewData.inputs.map { input ->
                    if (input is InputRadioVD) {
                        input.copy(isDecaf = isDecaf)
                    } else {
                        input
                    }
                }
                Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                currentResult
            }
        }
    }

    fun onEnterPress() {
        viewModelScope.launch {
            val currentViewData = _viewData.value as? Success<CoffeeScreenViewData> ?: return@launch
            val coffeeDto = withContext(dispatchers.default) {
                mapToCoffeeDto(currentViewData.data)
            }
            apiService.uploadCoffee(accountService.currentUserId, coffeeDto)
            _navigationEvent.send(NavigationEvent.NavigateToHome)
        }
    }

    private fun mapToCoffeeDto(viewData: CoffeeScreenViewData): CoffeeDto {
        var beanTypes = emptySet<String>()
        var originCountries = emptySet<String>()
        var tastingNotes = emptySet<String>()
        var beanPreparationMethod = emptySet<String>()
        var roaster: String = ""
        var isDecaf: Boolean? = null
        val formattedRoastDate: String =
            viewData.roastDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""

        viewData.inputs.forEach { input ->
            when (input) {
                is InputVD -> {
                    when (input.inputType) {
                        InputType.BEANS -> beanTypes = input.selectedSet
                        InputType.ORIGIN -> originCountries = input.selectedSet
                        InputType.TASTE -> tastingNotes = input.selectedSet
                        InputType.METHOD -> beanPreparationMethod = input.selectedSet
                        InputType.ROASTER -> roaster =
                            input.selectedSet.firstOrNull() ?: "no roaster"
                    }
                }
                is InputRadioVD -> isDecaf = input.isDecaf
            }
        }

        return CoffeeDto(
            roastDate = formattedRoastDate,
            beanTypes = beanTypes.toList(),
            originCountries = originCountries.toList(),
            tastingNotes = tastingNotes.toList(),
            beanPreparationMethod = beanPreparationMethod.toList(),
            roaster = roaster,
            isDecaf = isDecaf == true,
            label = "$roaster ${originCountries.joinToString(", ")} ${beanPreparationMethod.firstOrNull() ?: ""} $formattedRoastDate",
            id = UUID.randomUUID().toString()
        )
    }

    private fun handleToggleDropdownSelection(inputType: InputType, selection: String) {
        _viewData.update { currentResult ->
            if (currentResult is Success) {
                val currentViewData = currentResult.data
                val updatedInputs = currentViewData.inputs.map { input ->
                    if (input is InputVD && input.inputType == inputType) {
                        val newSelectedSet = if (input.singleInput) {
                            if (input.selectedSet.contains(selection)) emptySet() else setOf(selection)
                        } else {
                            if (input.selectedSet.contains(selection)) input.selectedSet - selection else input.selectedSet + selection
                        }
                        input.copy(selectedSet = newSelectedSet)
                    } else {
                        input
                    }
                }
                Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateSearchText(inputType: InputType, searchText: String) {
        _viewData.update { currentResult ->
            if (currentResult is Success) {
                val currentViewData = currentResult.data
                val updatedInputs = currentViewData.inputs.map { input ->
                    if (input is InputVD && input.inputType == inputType) {
                        input.copy(searchText = searchText)
                    } else {
                        input
                    }
                }
                Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                currentResult
            }
        }
    }
}

sealed class NavigationEvent {
    object NavigateToHome : NavigationEvent()
    object NavigateToInput : NavigationEvent()
    data object NavigateToLogin : NavigationEvent()
    data class NavigateToDetail(val coffeeId: String) : NavigationEvent()
}

sealed class CoffeeInputScreenIntent {
    data class UpdateRoastDate(val date: LocalDate) : CoffeeInputScreenIntent()
    data class SetDecaf(val isDecaf: Boolean) : CoffeeInputScreenIntent()
    object SubmitCoffee : CoffeeInputScreenIntent()
    data class ToggleDropdownSelection(val inputType: InputType, val selection: String) : CoffeeInputScreenIntent()
    data class UpdateSearchText(val inputType: InputType, val searchText: String) : CoffeeInputScreenIntent()
    object NavigateHome : CoffeeInputScreenIntent()
}

@Keep
data class CoffeeInputScreenDto(
    @get:PropertyName("BEANS") val BEANS: List<String> = emptyList(),
    @get:PropertyName("CAFFEINE") val CAFFEINE: List<String> = emptyList(),
    @get:PropertyName("METHOD") val METHOD: List<String> = emptyList(),
    @get:PropertyName("ORIGIN") val ORIGIN: List<String> = emptyList(),
    @get:PropertyName("ROASTER") val ROASTER: List<String> = emptyList(),
    @get:PropertyName("TASTE") val TASTE: List<String> = emptyList(),
)
