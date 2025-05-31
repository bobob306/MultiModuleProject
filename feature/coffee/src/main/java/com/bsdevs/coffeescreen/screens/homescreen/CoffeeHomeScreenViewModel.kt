package com.bsdevs.coffeescreen.screens.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.coffeescreen.screens.homescreen.viewdata.ButtonDestination
import com.bsdevs.coffeescreen.screens.homescreen.viewdata.CoffeeHomeScreenViewData
import com.bsdevs.coffeescreen.screens.homescreen.viewdata.CoffeeHomeScreenViewDatas
import com.bsdevs.coffeescreen.screens.inputscreen.NavigationEvent
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.beanPreparationMethod
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.coffeeBeanTypes
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.coffeeRoasters
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.coffeeTastingNotesList
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.generateSampleCoffeeDto
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.originCountries
import com.bsdevs.common.result.Result
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoffeeHomeScreenViewModel @Inject constructor(
    private val accountService: AccountService
) : ViewModel() {
    private val _viewData = MutableStateFlow<Result<CoffeeHomeScreenViewData>>(Result.Loading)
    val viewData: StateFlow<Result<CoffeeHomeScreenViewData>>
        get() = _viewData.onStart {
            loadData()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private fun loadData() {
        _viewData.value = Result.Success(
            data = loadedData
        )
    }

    fun processIntent(intent: CoffeeHomeScreenIntent) {
        viewModelScope.launch {
            when (intent) {
                is CoffeeHomeScreenIntent.LoadData -> loadData()
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

//                is CoffeeHomeScreenIntent.NavigateToDetail -> {
//                    // Handle navigation to a detail screen
//                    _navigationEvent.send(NavigationEvent.NavigateToDetail(intent.coffee))
//                }
            }
        }
    }

    private fun handleSignOut() {
        viewModelScope.launch {
            accountService.signOut()
            _navigationEvent.send(NavigationEvent.NavigateToLogin)
        }
    }

    private fun uploadCoffeeData() {
        val beans = coffeeBeanTypes
        val method = beanPreparationMethod
        val taste = coffeeTastingNotesList
        val roaster = coffeeRoasters
        val caffeine = listOf("Caffeinated", "Decaffeinated")
        val origin = originCountries
        val coffeeDetails = listOf(
            beans, method, taste, roaster, caffeine, origin
        )

        val collectionReference =
            FirebaseFirestore.getInstance().collection("screens").document("coffeeInput")
        viewModelScope.launch {
            collectionReference.update("METHOD", method)
            collectionReference.update("TASTE", taste)
            collectionReference.update("ROASTER", roaster)
            collectionReference.update("CAFFEINE", caffeine)
            collectionReference.update("ORIGIN", origin)
        }
    }
}

sealed class CoffeeHomeScreenIntent {
    data object LoadData : CoffeeHomeScreenIntent()
    data object NavigateToInput : CoffeeHomeScreenIntent()
    data class ShowSnackBar(val message: String, val actionLabel: String?) :
        CoffeeHomeScreenIntent()

    data object Logout : CoffeeHomeScreenIntent()

//    data class NavigateToDetail(val coffee: CoffeeDto) : CoffeeHomeScreenIntent()
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
