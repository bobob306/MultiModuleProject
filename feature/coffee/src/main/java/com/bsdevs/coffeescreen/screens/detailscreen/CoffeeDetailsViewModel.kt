package com.bsdevs.coffeescreen.screens.detailscreen

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CoffeeDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val accountService: AccountService,
) : ViewModel() {
    private lateinit var selectedCoffee: String

    private val _viewData = MutableStateFlow<Result<CoffeeDto>>(Loading)
    val viewData: StateFlow<Result<CoffeeDto>> = _viewData.onStart {
            loadDataFromNetwork()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Loading
        )

    init {
        val detailsRoute: CoffeeDetailScreenRoute = savedStateHandle.toRoute()
        selectedCoffee = detailsRoute.coffeeId
    }
    private suspend fun loadDataFromNetwork() {

        val currentUser = accountService.currentUserId
        val collectionReference =
            FirebaseFirestore.getInstance().collection("coffeeUploads")
                .whereEqualTo("userId", currentUser)
                .whereEqualTo("id", selectedCoffee)
                .get()
                .await()
        val coffeeListFromNetwork = collectionReference.toObjects(CoffeeDto::class.java).first()
        _viewData.value = Result.Success(
            data = coffeeListFromNetwork
        )
    }
    private fun loadData() {
        _viewData.value = Result.Success(
            data = CoffeeDto(
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
            )
        )
    }
}