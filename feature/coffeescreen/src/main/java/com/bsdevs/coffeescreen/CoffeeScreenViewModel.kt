package com.bsdevs.coffeescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.coffeescreen.viewdata.CoffeeScreenViewData
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
        val currentViewData = _viewData.value as Result.Success<CoffeeScreenViewData>
        var newSelectedBean = currentViewData.data.beanTypeInput.selectedSet
        if (newSelectedBean.contains(coffeeType)) {
            newSelectedBean = newSelectedBean - coffeeType
        } else {
            newSelectedBean = newSelectedBean + coffeeType
        }
        _viewData.update {
            Result.Success(
                data = currentViewData.data.copy(
                    beanTypeInput = currentViewData.data.beanTypeInput.copy(
                        selectedSet = newSelectedBean
                    )
                )
            )
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
        val currentViewData = _viewData.value as Result.Success<CoffeeScreenViewData>
        var newSelectedCountry = currentViewData.data.originInput.selectedSet
        if (newSelectedCountry.contains(originCountry)) {
            newSelectedCountry = newSelectedCountry - originCountry
        } else {
            newSelectedCountry = newSelectedCountry + originCountry
        }
        _viewData.update {
            Result.Success(
                data = currentViewData.data.copy(
                    originInput = currentViewData.data.originInput.copy(
                        selectedSet = newSelectedCountry
                    )
                )
            )
        }
    }

    fun onToggleCoffeeTasteSelected(taste: String) {
        val currentViewData = _viewData.value as Result.Success<CoffeeScreenViewData>
        var newTaste = currentViewData.data.coffeeTastingNotesInput.selectedSet
        if (newTaste.contains(taste)) {
            newTaste = newTaste - taste
        } else {
            newTaste = newTaste + taste
        }
        _viewData.update {
            Result.Success(
                data = currentViewData.data.copy(
                    coffeeTastingNotesInput = currentViewData.data.coffeeTastingNotesInput.copy(
                        selectedSet = newTaste
                    )
                )
            )
        }
    }

    fun onToggleDecaf(isDecaf: Boolean) {
        println("isDecaf $isDecaf")
        val currentViewData = _viewData.value as Result.Success<CoffeeScreenViewData>
        _viewData.update {
            Result.Success(
                data = currentViewData.data.copy(
                    decafInput = currentViewData.data.decafInput.copy(
                        isDecaf = isDecaf
                    )
                )
            )
        }
    }

    fun onCoffeeTasteType(taste: String) {
        val searchText = taste
        val currentViewData = _viewData.value as Result.Success<CoffeeScreenViewData>
        _viewData.update {
            Result.Success(
                data = currentViewData.data.copy(
                    coffeeTastingNotesInput = currentViewData.data.coffeeTastingNotesInput.copy(
                        searchText = searchText
                    )
                )
            )
        }
    }
}