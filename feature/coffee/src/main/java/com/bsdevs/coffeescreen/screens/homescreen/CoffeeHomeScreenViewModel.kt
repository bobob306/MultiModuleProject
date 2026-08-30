package com.bsdevs.coffeescreen.screens.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.coffeescreen.data.CoffeeRepository
import com.bsdevs.coffeescreen.screens.homescreen.viewdata.ButtonDestination
import com.bsdevs.coffeescreen.screens.homescreen.viewdata.CoffeeHomeScreenViewData
import com.bsdevs.coffeescreen.screens.homescreen.viewdata.CoffeeHomeScreenViewDatas
import com.bsdevs.coffeescreen.screens.inputscreen.NavigationEvent
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.generateSampleCoffeeDto
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoffeeHomeScreenViewModel @Inject constructor(
    private val accountService: AccountService,
    private val repository: CoffeeRepository
) : ViewModel() {
    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private lateinit var currentUser: String
    private val _viewData = MutableStateFlow<Result<CoffeeHomeScreenViewData>>(Result.Loading)
    val viewData: StateFlow<Result<CoffeeHomeScreenViewData>> = _viewData.asStateFlow()

    init {
        viewModelScope.launch {
            start()
            repository.allCoffee.collect { coffeeListFromCache ->
                if (coffeeListFromCache.isNotEmpty()) {
                    updateDisplay(coffeeListFromCache)
                }
            }
        }
    }

    suspend fun start() {
        currentUser = try {
            accountService.currentUserId
        } catch (e: Exception) {
            _navigationEvent.send(NavigationEvent.NavigateToLogin)
            ""
        }
        if (currentUser.isNotEmpty()) {
            loadDataFromNetwork()
        }
    }

    private fun updateDisplay(coffeeList: List<com.bsdevs.coffeescreen.network.CoffeeDto>) {
        val vd = loadedData.copy(
            viewData = loadedData.viewData.map {
                when (it) {
                    is CoffeeHomeScreenViewDatas.CoffeeList -> {
                        it.copy(coffeeList = coffeeList)
                    }
                    else -> it
                }
            }
        )
        _viewData.value = Result.Success(data = vd)
    }

    private fun loadData() {
        _viewData.value = Result.Success(
            data = loadedData
        )
    }

    private suspend fun loadDataFromNetwork() {
        if (currentUser.isEmpty()) return
        
        try {
            repository.loadInitialData(currentUser)
        } catch (e: Exception) {
             if (_viewData.value is Result.Loading) {
                 _viewData.value = Result.Error(e)
             }
        }
    }

    fun processIntent(intent: CoffeeHomeScreenIntent) {
        viewModelScope.launch {
            when (intent) {
                is CoffeeHomeScreenIntent.LoadData -> loadData()
                is CoffeeHomeScreenIntent.RefreshData -> refreshData()
                is CoffeeHomeScreenIntent.NavigateToInput -> {
                    // Handle navigation to a destination
                    _navigationEvent.send(NavigationEvent.NavigateToInput)
                }

                is CoffeeHomeScreenIntent.ShowSnackBar -> {
                    // Handle showing a snack bar
                }

                is CoffeeHomeScreenIntent.Logout -> {
                    handleSignOut()
                }

                is CoffeeHomeScreenIntent.NavigateToDetail -> {
                    // Handle navigation to a detail screen
                    println("Navigating to detail screen with ID: ${intent.id}")
                    _navigationEvent.send(NavigationEvent.NavigateToDetail(intent.id))
                }
            }
        }
    }

    private fun handleSignOut() {
        viewModelScope.launch {
            accountService.signOut()
            _navigationEvent.send(NavigationEvent.NavigateToLogin)
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            val current = (_viewData.value as? Result.Success)?.data ?: loadedData
            _viewData.value = Result.Success(current.copy(isRefreshing = true))
            loadDataFromNetwork()
        }
    }
}

sealed class CoffeeHomeScreenIntent {
    data object LoadData : CoffeeHomeScreenIntent()
    data object RefreshData : CoffeeHomeScreenIntent()
    data object NavigateToInput : CoffeeHomeScreenIntent()
    data class ShowSnackBar(val message: String, val actionLabel: String?) :
        CoffeeHomeScreenIntent()

    data object Logout : CoffeeHomeScreenIntent()

    data class NavigateToDetail(val id: String) : CoffeeHomeScreenIntent()
}

private val coffeeList = generateSampleCoffeeDto(100).sortedBy { it.roastDate }.reversed()

private val loadedData = CoffeeHomeScreenViewData(
    viewData = listOf<CoffeeHomeScreenViewDatas>(
        CoffeeHomeScreenViewDatas.Button(
            label = "Input",
            destination = ButtonDestination.INPUT
        ),
        CoffeeHomeScreenViewDatas.Button(
            label = "Edit",
            destination = ButtonDestination.EDIT
        ),
        CoffeeHomeScreenViewDatas.Button(
            label = "Home",
            destination = ButtonDestination.HOME
        ),
        CoffeeHomeScreenViewDatas.Image(
            url = "https://example.com/image.jpg",
            description = "Image Description"
        ),
        CoffeeHomeScreenViewDatas.HeaderSection(
            title = "Header Title",
            description = "Header Description"
        ),
        CoffeeHomeScreenViewDatas.CoffeeList(
            coffeeList = coffeeList
        )
    )
)
