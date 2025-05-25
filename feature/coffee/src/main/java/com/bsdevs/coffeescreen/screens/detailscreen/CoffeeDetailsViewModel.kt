package com.bsdevs.coffeescreen.screens.detailscreen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bsdevs.coffeescreen.navigation.CoffeeDetailScreenRoute
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.common.result.Result
import com.bsdevs.common.result.Result.Loading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class CoffeeDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val detailsRoute: CoffeeDetailScreenRoute = savedStateHandle.toRoute()
//    private val selectedCoffee = detailsRoute.coffeeDetail

    private val _viewData = MutableStateFlow<com.bsdevs.common.result.Result<CoffeeDto>>(Loading)
    val viewData: StateFlow<Result<CoffeeDto>>
        get() = _viewData.onStart {
            loadData()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Loading
        )

    private fun loadData() {
        _viewData.value = Result.Success(
            data = CoffeeDto(
                label = "a coffee"
            )
        )
    }
}