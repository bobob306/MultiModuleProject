package com.bsdevs.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.common.result.Result
import com.bsdevs.data.ScreenData
import com.bsdevs.data.ScreenDataMapper
import com.bsdevs.network.repository.ScreenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: ScreenRepository,
    private val mapper: ScreenDataMapper
) : ViewModel() {

    private val _viewData = MutableStateFlow<Result<List<ScreenData>>>(value = Result.Loading)
    val viewData: StateFlow<Result<List<ScreenData>>> get() = _viewData

    fun getScreen() {
        viewModelScope.launch {
            repository.getScreenFlow("home").collect { result ->
                when (result) {
                    is Result.Success -> {
                        _viewData.update { Result.Success(mapper.mapToData(result.data)) }
                    }

                    is Result.Error -> {
                        _viewData.update { result }
                    }

                    is Result.Loading -> Result.Loading
                }
            }

        }
    }

    fun click(destination: String, label: String) {
        println("Destination: $destination, Label: $label")
    }
}