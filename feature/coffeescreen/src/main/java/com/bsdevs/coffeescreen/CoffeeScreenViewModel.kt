package com.bsdevs.coffeescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.coffeescreen.viewdata.CoffeeScreenViewData
import com.bsdevs.coffeescreen.viewdata.InputType
import com.bsdevs.coffeescreen.viewdata.InputViewData
import com.bsdevs.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
}