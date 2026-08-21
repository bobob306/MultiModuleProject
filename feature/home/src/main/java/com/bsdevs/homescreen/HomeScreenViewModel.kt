package com.bsdevs.homescreen

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.common.result.Result
import com.bsdevs.data.LocationTypeData
import com.bsdevs.data.LocationTypeData.INTERNAL
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.ScreenDataMapper
import com.bsdevs.network.repository.ScreenRepository
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
import javax.inject.Inject

sealed class HomeNavigationEvent {
    data class NavigateToDeepLink(val uriString: String) : HomeNavigationEvent()
}

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: ScreenRepository,
    private val mapper: ScreenDataMapper
) : ViewModel() {

    private val _viewData = MutableStateFlow<Result<List<NetworkScreenData>>>(value = Result.Loading)
    val viewData: StateFlow<Result<List<NetworkScreenData>>> = _viewData.onStart {
            getScreen()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    private val _navigationEvents = Channel<HomeNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

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

    fun handleServerButtonClick(destinationUrl: String, location: LocationTypeData, label: String) {
        println("Destination: $destinationUrl, Label: $label")
        viewModelScope.launch {
            if (location == INTERNAL) {
                // Emits the safe action token down into your screen view listener pipe
                _navigationEvents.send(HomeNavigationEvent.NavigateToDeepLink(destinationUrl))
            } else {
                // Optional: Handle EXTERNAL browser redirection URLs here down the road
            }
        }
    }
}