package com.bsdevs.coffeescreen

import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    val isButtonEnabledState = derivedStateOf {
        // Access the value of the State inside the derivedStateOf lambda
        // This lambda will re-run whenever viewDataResult changes.
        if (_viewData.value is Success<*>) {
            val cviewData = (_viewData.value as Success<*>).data as CoffeeScreenViewData
            val inputs = cviewData.inputs

            // Your validation logic here...
            val areSetsValid = inputs.all { input ->
                // ... validation logic for sets ...
                when (input) {
                    is InputViewData.InputVD -> {
                        when (input.inputType) {
                            InputType.BEANS, InputType.ORIGIN, InputType.TASTE -> input.selectedSet.isNotEmpty()
                            else -> true
                        }
                    }

                    else -> true
                }
            }

            val isRadioValid =
                inputs.filterIsInstance<InputViewData.InputRadioVD>().all { it.isDecaf != null }
            val isDateValid = cviewData.roastDate != null

            areSetsValid && isRadioValid && isDateValid
            _viewData.update {
                Success(
                    data = cviewData.copy(
                        isButtonEnabled = areSetsValid && isRadioValid && isDateValid
                    )
                )
            }
        } else {
            false
        }
    }


    val isButtonEnabled: StateFlow<Boolean> = viewData.map { currentResult ->
        if (currentResult is Success) {
            val viewData = currentResult.data
            val inputs = viewData.inputs

            // Check mutable sets (assuming BEANS, ORIGIN, TASTE are the ones)
            val areSetsValid = inputs.all { input ->
                when (input) {
                    is InputViewData.InputVD -> {
                        when (input.inputType) {
                            InputType.BEANS, InputType.ORIGIN, InputType.TASTE -> input.selectedSet.isNotEmpty()
                            else -> true // Other InputVD types are considered valid
                        }
                    }

                    else -> true // Other input types (like InputRadioVD) are checked separately
                }
            }

            // Check radio button (assuming there's one InputRadioVD)
            val isRadioValid =
                inputs.filterIsInstance<InputViewData.InputRadioVD>().all { it.isDecaf != null }
            // Note: Assuming isDecaf being non-null indicates a selection. Adjust logic based on your ViewData.

            // Check the date field
            val isDateValid = viewData.roastDate != null
            // Note: Assuming a non-null roastDate indicates a valid entry. Adjust logic based on your ViewData.
            _viewData.update {
                Success(
                    data = viewData.copy(
                        isButtonEnabled = areSetsValid && isRadioValid && isDateValid
                    )
                )
            }
            areSetsValid && isRadioValid && isDateValid
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
        _viewData.value = Result.Success(
            data = CoffeeScreenViewData()
        )
    }

    fun onToggleCoffeeTypeSelected(coffeeType: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
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
                Result.Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                // If the current state is not Success, return it unchanged
                currentResult
            }
        }
    }

    fun onUpdateRoastData(date: LocalDate) {
        val currentViewData = _viewData.value as Result.Success<CoffeeScreenViewData>
        _viewData.update {
            Result.Success(
                data = currentViewData.data.copy(
                    roastDate = date
                )
            )
        }
    }

    fun onToggleCoffeeOriginSelected(originCountry: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
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
                Result.Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                // If the current state is not Success, return it unchanged
                currentResult
            }
        }
    }

    fun onToggleCoffeeTasteSelected(taste: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
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
                Result.Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                // If the current state is not Success, return it unchanged
                currentResult
            }
        }
    }

    fun onToggleDecaf(isDecaf: Boolean) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
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
                Result.Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                // If the current state is not Success, return it unchanged
                currentResult
            }
        }
    }

    fun onCoffeeTasteType(taste: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
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
                Result.Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                // If the current state is not Success, return it unchanged
                currentResult
            }
        }
    }

    private fun buttonEnableLogic() {

    }

    fun onEnterPress() {

    }
}