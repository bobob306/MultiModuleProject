package com.bsdevs.coffeescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.common.result.Result
import com.bsdevs.data.NetworkScreenData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CoffeeScreenViewModel @Inject constructor() : ViewModel() {
    private val _viewData = MutableStateFlow<Result<List<NetworkScreenData>>>(value = Result.Loading)
    val viewData: StateFlow<Result<List<NetworkScreenData>>>
        get() = _viewData.onStart {
            loadData()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    private fun loadData() {

    }
}