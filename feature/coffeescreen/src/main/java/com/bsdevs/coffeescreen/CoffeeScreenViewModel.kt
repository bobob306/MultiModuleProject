package com.bsdevs.coffeescreen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.viewdata.CoffeeScreenViewData
import com.bsdevs.coffeescreen.viewdata.InputType
import com.bsdevs.coffeescreen.viewdata.InputViewData
import com.bsdevs.common.result.Result
import com.bsdevs.common.result.Result.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class CoffeeScreenViewModel @Inject constructor() : ViewModel() {
    private val _viewData = MutableStateFlow<Result<CoffeeScreenViewData>>(value = Result.Loading)
    val viewData: StateFlow<Result<CoffeeScreenViewData>>
        get() = _viewData.onStart {
            loadData()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    val isButtonEnabled: StateFlow<Boolean> = viewData.map { currentResult ->
        if (currentResult is Success) {
            val viewData = currentResult.data
            val inputs = viewData.inputs

            // Check mutable sets (assuming BEANS, ORIGIN, TASTE are the ones)
            val areSetsValid = inputs.all { input ->
                when (input) {
                    is InputViewData.InputVD -> {
                        when (input.inputType) {
                            InputType.BEANS, InputType.ORIGIN, InputType.TASTE, InputType.METHOD -> input.selectedSet.isNotEmpty()
                            else -> true // Other InputVD types are considered valid
                        }
                    }

                    else -> true // Other input types (like InputRadioVD) are checked separately
                }
            }

            // Note: Assuming isDecaf being non-null indicates a selection. Adjust logic based on your ViewData.

            // Check the date field
            val isDateValid = viewData.roastDate != null
            // Note: Assuming a non-null roastDate indicates a valid entry. Adjust logic based on your ViewData.
            _viewData.update {
                Success(
                    data = viewData.copy(
                        isButtonEnabled = areSetsValid && isDateValid
                    )
                )
            }
            areSetsValid && isDateValid
        } else {
            // If the result is not Success (e.g., Loading or Error), the button should be disabled
            false
        }
    }
        // Convert the Flow<Boolean> into a StateFlow<Boolean>
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false // Initial state is disabled
        )

    private fun loadData() {
        _viewData.value = Success(
            data = CoffeeScreenViewData()
        )
    }

    fun processIntent(intent: CoffeeScreenIntent) {
        when (intent) {
            is CoffeeScreenIntent.UpdateRoastDate -> onUpdateRoastData(intent.date)
            is CoffeeScreenIntent.SetDecaf -> onToggleDecaf(intent.isDecaf)
            CoffeeScreenIntent.SubmitCoffee -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    onEnterPress()
                }
            }
            is CoffeeScreenIntent.ToggleDropdownSelection -> {
                handleToggleDropdownSelection(intent.inputType, intent.selection)
            }
            is CoffeeScreenIntent.UpdateSearchText -> {
                handleUpdateSearchText(intent.inputType, intent.searchText)
            }
        }
    }

    fun onToggleCoffeeTypeSelected(coffeeType: String) {
        _viewData.update { currentResult ->
            if (currentResult is Success) {
                val currentViewData = currentResult.data
                val updatedInputs = currentViewData.inputs.map { input ->
                    if (input is InputViewData.InputVD && input.inputType == InputType.BEANS) {
                        // Update the selected set for the beans input
                        val newSelectedBeans = if (input.selectedSet.contains(coffeeType)) {
                            input.selectedSet - coffeeType
                        } else {
                            input.selectedSet + coffeeType
                        }
                        input.copy(selectedSet = newSelectedBeans)
                    } else {
                        // Keep other inputs as they are
                        input
                    }
                }
                // Return a new Success result with the updated inputs list
                Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                // If the current state is not Success, return it unchanged
                currentResult
            }
        }
    }

    fun onToggleProcessSelected(processType: String) {
        _viewData.update { currentResult ->
            if (currentResult is Success) {
                val currentViewData = currentResult.data
                val updatedInputs = currentViewData.inputs.map { input ->
                    if (input is InputViewData.InputVD && input.inputType == InputType.METHOD) {
                        // Update the selected set for the beans input
                        val newSelectedProcess = if (input.selectedSet.contains(processType)) {
                            input.selectedSet - processType
                        } else {
                            input.selectedSet + processType
                        }
                        input.copy(selectedSet = newSelectedProcess)
                    } else {
                        // Keep other inputs as they are
                        input
                    }
                }
                // Return a new Success result with the updated inputs list
                Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                // If the current state is not Success, return it unchanged
                currentResult
            }
        }
    }

    fun onUpdateRoastData(date: LocalDate) {
        val currentViewData = _viewData.value as Success<CoffeeScreenViewData>
        _viewData.update {
            Success(
                data = currentViewData.data.copy(
                    roastDate = date
                )
            )
        }
    }

    fun onToggleCoffeeOriginSelected(originCountry: String) {
        _viewData.update { currentResult ->
            if (currentResult is Success) {
                val currentViewData = currentResult.data
                val updatedInputs = currentViewData.inputs.map { input ->
                    if (input is InputViewData.InputVD && input.inputType == InputType.ORIGIN) {
                        // Update the selected set for the beans input
                        val newSelectedOrigin = if (input.selectedSet.contains(originCountry)) {
                            input.selectedSet - originCountry
                        } else {
                            input.selectedSet + originCountry
                        }
                        input.copy(selectedSet = newSelectedOrigin)
                    } else {
                        // Keep other inputs as they are
                        input
                    }
                }
                // Return a new Success result with the updated inputs list
                Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                // If the current state is not Success, return it unchanged
                currentResult
            }
        }
    }

    fun onToggleCoffeeTasteSelected(taste: String) {
        _viewData.update { currentResult ->
            if (currentResult is Success) {
                val currentViewData = currentResult.data
                val updatedInputs = currentViewData.inputs.map { input ->
                    if (input is InputViewData.InputVD && input.inputType == InputType.TASTE) {
                        // Update the selected set for the beans input
                        val newSelectedTaste = if (input.selectedSet.contains(taste)) {
                            input.selectedSet - taste
                        } else {
                            input.selectedSet + taste
                        }
                        input.copy(selectedSet = newSelectedTaste)
                    } else {
                        // Keep other inputs as they are
                        input
                    }
                }
                // Return a new Success result with the updated inputs list
                Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                // If the current state is not Success, return it unchanged
                currentResult
            }
        }
    }

    fun onToggleDecaf(isDecaf: Boolean) {
        _viewData.update { currentResult ->
            if (currentResult is Success) {
                val currentViewData = currentResult.data
                val updatedInputs = currentViewData.inputs.map { input ->
                    if (input is InputViewData.InputRadioVD) {
                        input.copy(isDecaf = isDecaf)
                    } else {
                        // Keep other inputs as they are
                        input
                    }
                }
                // Return a new Success result with the updated inputs list
                Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                // If the current state is not Success, return it unchanged
                currentResult
            }
        }
    }

    fun onCoffeeTasteType(taste: String) {
        _viewData.update { currentResult ->
            if (currentResult is Success) {
                val currentViewData = currentResult.data
                val updatedInputs = currentViewData.inputs.map { input ->
                    if (input is InputViewData.InputVD && input.inputType == InputType.TASTE) {
                        input.copy(searchText = taste)
                    } else {
                        // Keep other inputs as they are
                        input
                    }
                }
                // Return a new Success result with the updated inputs list
                Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                // If the current state is not Success, return it unchanged
                currentResult
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onEnterPress() {
        val currentViewData = _viewData.value as Success<CoffeeScreenViewData>
        val coffeeDto = mapToCoffeeDto(currentViewData.data)
        println(coffeeDto)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun mapToCoffeeDto(viewData: CoffeeScreenViewData): CoffeeDto {
        var beanTypes = emptySet<String>()
        var originCountries = emptySet<String>()
        var tastingNotes = emptySet<String>()
        var beanPreparationMethod = emptySet<String>() // For METHOD
        var isDecaf: Boolean? = null
        val formattedRoastDate: String = viewData.roastDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""

        viewData.inputs.forEach { input ->
            when (input) {
                is InputViewData.InputVD -> {
                    when (input.inputType) {
                        InputType.BEANS -> beanTypes = input.selectedSet
                        InputType.ORIGIN -> originCountries = input.selectedSet
                        InputType.TASTE -> tastingNotes = input.selectedSet
                        InputType.METHOD -> beanPreparationMethod = input.selectedSet // Map METHOD
                    }
                }
                is InputViewData.InputRadioVD -> {
                    // Assuming the label for the decaf radio button is consistent
                    // or you only have one InputRadioVD for decaf.
                    isDecaf = input.isDecaf
                }
            }
        }

        return CoffeeDto(
            roastDate = formattedRoastDate,
            beanTypes = beanTypes.toList(),
            originCountries = originCountries.toList(),
            tastingNotes = tastingNotes.toList(),
            beanPreparationMethod = beanPreparationMethod.toList(),
            isDecaf = isDecaf == true
        )
    }

    private fun handleToggleDropdownSelection(inputType: InputType, selection: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                val currentViewData = currentResult.data
                val updatedInputs = currentViewData.inputs.map { input ->
                    if (input is InputViewData.InputVD && input.inputType == inputType) {
                        val newSelectedSet = if (input.selectedSet.contains(selection)) {
                            input.selectedSet - selection
                        } else {
                            // For multi-select, add. For single-select, replace.
                            // Assuming multi-select for now based on selectedSet
                            input.selectedSet + selection
                        }
                        input.copy(selectedSet = newSelectedSet)
                    } else {
                        input
                    }
                }
                Result.Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateSearchText(inputType: InputType, searchText: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                val currentViewData = currentResult.data
                val updatedInputs = currentViewData.inputs.map { input ->
                    if (input is InputViewData.InputVD && input.inputType == inputType) {
                        input.copy(searchText = searchText)
                    } else {
                        input
                    }
                }
                Result.Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                currentResult
            }
        }
    }
}

sealed class CoffeeScreenIntent {
    data class UpdateRoastDate(val date: LocalDate) : CoffeeScreenIntent()
    data class SetDecaf(val isDecaf: Boolean) : CoffeeScreenIntent()
    object SubmitCoffee : CoffeeScreenIntent() // For the "Enter" press or submit button
    data class ToggleDropdownSelection(val inputType: InputType, val selection: String) : CoffeeScreenIntent()
    data class UpdateSearchText(val inputType: InputType, val searchText: String) : CoffeeScreenIntent()
}