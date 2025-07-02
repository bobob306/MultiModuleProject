package com.bsdevs.coffeescreen.screens.detailscreen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bsdevs.authentication.AccountService
import com.bsdevs.coffeescreen.navigation.CoffeeDetailScreenRoute
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.common.result.Result
import com.bsdevs.common.result.Result.Loading
import com.google.firebase.firestore.FirebaseFirestore
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
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CoffeeDetailsViewData(
    val coffeeDto: CoffeeDto,
    val shotList: List<ShotDto>?,
)

@HiltViewModel
class CoffeeDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val accountService: AccountService,
) : ViewModel() {
    private lateinit var selectedCoffee: String

    private val _viewData = MutableStateFlow<Result<CoffeeDetailsViewData>>(Loading)
    val viewData: StateFlow<Result<CoffeeDetailsViewData>> = _viewData.onStart {
        loadDataFromNetwork()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Loading
    )

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow() // Expose as Flow

    init {
        val detailsRoute: CoffeeDetailScreenRoute = savedStateHandle.toRoute()
        selectedCoffee = detailsRoute.coffeeId
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun loadDataFromNetwork() {

        val currentUser = accountService.currentUserId
        val collectionReference =
            FirebaseFirestore.getInstance().collection("coffeeUploads")
                .whereEqualTo("userId", currentUser)
                .whereEqualTo("id", selectedCoffee)
                .get()
                .await()
        val coffeeListFromNetwork = collectionReference.toObjects(CoffeeDto::class.java).first()
        val shotsFromNetwork = collectionReference.documents.first().reference.collection("shots").get().await()
        val formattedShots = shotsFromNetwork?.let {
            it.map { docs ->
                docs.toObject(ShotDto::class.java)
            }
        }
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val sortedFormattedShots = formattedShots?.sortedByDescending { shotDto ->
            shotDto.date?.let { dateString ->
                LocalDate.parse(dateString, formatter)
            }
        }


        _viewData.value = Result.Success(
            data = CoffeeDetailsViewData(
                coffeeDto = coffeeListFromNetwork,
                shotList = sortedFormattedShots,
            )
        )
    }

    private fun loadData() {
        _viewData.value = Result.Success(
            data = CoffeeDetailsViewData(
                coffeeDto = CoffeeDto(
                    label = "a coffee",
                    roastDate = TODO(),
                    beanTypes = TODO(),
                    originCountries = TODO(),
                    tastingNotes = TODO(),
                    beanPreparationMethod = TODO(),
                    roaster = TODO(),
                    isDecaf = TODO(),
                    userId = TODO(),
                    id = TODO()
                ),
                null,
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun processIntent(intent: CoffeeDetailsIntent) {
        when (intent) {
            CoffeeDetailsIntent.NavigateHome -> {
                // Handle navigation to the home screen
                viewModelScope.launch {
                    _navigationEvent.send(NavigationEvent.NavigateHome)
                }
            }

            is CoffeeDetailsIntent.SubmitShot -> {
                viewModelScope.launch {
                    try {
                        val coffeeLabel = viewData.value as Result.Success<CoffeeDetailsViewData>
                        val label = coffeeLabel.data.coffeeDto.label
                        _viewData.update {
                            Result.Loading
                        }
                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        val formattedDate = formatter.format(intent.shot.date)

                        val shotDto = ShotDto(
                            id = intent.shot.id,
                            date = formattedDate,
                            weightIn = intent.shot.weightInGrams.toDouble()/10,
                            weightOut = intent.shot.weightOutGrams.toDouble()/10,
                            time = intent.shot.timeInSeconds,
                            rating = intent.shot.rating,
                        )
                        val currentUser = accountService.currentUserId
                        val database = FirebaseFirestore.getInstance().collection("coffeeUploads")
                            .document("$label").collection("shots")
                            .document(intent.shot.id)
                            .set(shotDto)
                            .await()
                        val downloadedShots =
                            FirebaseFirestore.getInstance().collection("coffeeUploads")
                                .document("$label").collection("shots")
                                .get()
                                .await()
                        val formattedShots = downloadedShots.map {
                            it.toObject(ShotDto::class.java)
                        }
                        val dateSortFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        val sortedFormattedShots = formattedShots.sortedByDescending { shotDto ->
                            shotDto.date?.let { dateString ->
                                LocalDate.parse(dateString, dateSortFormatter)
                            }
                        }
                        _viewData.update {
                            Result.Success(
                                data = CoffeeDetailsViewData(
                                    coffeeDto = coffeeLabel.data.coffeeDto,
                                    shotList = sortedFormattedShots,
                                ),
                            )
                        }
                    } catch (e: Exception) {
                        println("error uploading shot ${e.message} ${e.cause}")
                        // Handle error, e.g., show a toast or log the error
                    }
                }
            }
        }
    }
}

data class ShotList(
    val shots: List<ShotDto>
)

data class ShotDto(
    val id: String? = null,
    val date: String? = null,
    val weightIn: Double? = null,
    val weightOut: Double? = null,
    val time: Int? = null,
    val rating: Int? = null,
)

sealed class CoffeeDetailsIntent {
    object NavigateHome : CoffeeDetailsIntent()
    data class SubmitShot(val shot: EspressoShotDetails) : CoffeeDetailsIntent()
}

sealed class NavigationEvent {
    object NavigateHome : NavigationEvent()
}