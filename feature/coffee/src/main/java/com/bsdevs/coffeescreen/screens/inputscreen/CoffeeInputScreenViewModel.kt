package com.bsdevs.coffeescreen.screens.inputscreen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.CoffeeScreenViewData
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.InputType
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.InputViewData
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.InputViewData.InputRadioVD
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.InputViewData.InputVD
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.RadioInputViewData
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.beanPreparationMethod
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.coffeeBeanTypes
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.coffeeRoasters
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.coffeeTastingNotesList
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.originCountries
import com.bsdevs.common.result.Result
import com.bsdevs.common.result.Result.Success
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import kotlin.collections.List

@HiltViewModel
class CoffeeInputScreenViewModel @Inject constructor(
    private val accountService: AccountService
) : ViewModel() {
    private val _viewData = MutableStateFlow<Result<CoffeeScreenViewData>>(value = Result.Loading)
    val viewData: StateFlow<Result<CoffeeScreenViewData>> = _viewData.onStart {
            loadDataFromNetwork()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = Result.Loading
        )

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow() // Expose as Flow

    val isButtonEnabled: StateFlow<Boolean> = viewData.map { currentResult ->
        if (currentResult is Success) {
            val viewData = currentResult.data
            val inputs = viewData.inputs

            // Check mutable sets (assuming BEANS, ORIGIN, TASTE are the ones)
            val areSetsValid = inputs.all { input ->
                when (input) {
                    is InputViewData.InputVD -> {
                        when (input.inputType) {
                            InputType.BEANS, InputType.ORIGIN, InputType.TASTE, InputType.METHOD, InputType.ROASTER -> input.selectedSet.isNotEmpty()
                        }
                    }

                    else -> true // Other input types (like InputRadioVD) are checked separately
                }
            }

            // Note: Assuming isDecaf being non-null indicates a selection. Adjust logic based on your ViewData.

            // Check the date field
            val isDateValid = viewData.roastDate != null
            // Note: Assuming a non-null roastDate indicates a valid entry. Adjust logic based on your ViewData.
            _viewData.update {
                Success(
                    data = viewData.copy(
                        isButtonEnabled = areSetsValid && isDateValid
                    )
                )
            }
            areSetsValid && isDateValid
        } else {
            // If the result is not Success (e.g., Loading or Error), the button should be disabled
            false
        }
    }
        // Convert the Flow<Boolean> into a StateFlow<Boolean>
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false // Initial state is disabled
        )

    private fun loadData() {
        _viewData.value = Success(
            data = CoffeeScreenViewData()
        )
    }

    private suspend fun loadDataFromNetwork() {
        val collectionReference = Firebase.firestore.collection("screens").document("coffeeInput")
        val documentSnapshot = collectionReference.get().await()
        val data = documentSnapshot.toObject(CoffeeInputScreenDto::class.java)
        val viewData = CoffeeScreenViewData()
        val updatedViewData = viewData.copy(
            inputs = listOf(
                InputVD(
                    label = "Coffee Type(s)",
                    inputList = data?.BEANS ?: coffeeBeanTypes,
                    selectedSet = emptySet(),
                    searchText = null,
                    inputType = InputType.BEANS
                ),
                InputVD(
                    label = "Coffee Origin(s)",
                    inputList = data?.ORIGIN ?: originCountries,
                    selectedSet = emptySet(),
                    searchText = null,
                    inputType = InputType.ORIGIN
                ),
                InputVD(
                    label = "Coffee Tasting Notes",
                    inputList = data?.TASTE ?: coffeeTastingNotesList,
                    selectedSet = emptySet(),
                    searchText = "",
                    inputType = InputType.TASTE
                ),
                InputVD(
                    label = "Coffee Preparation Method",
                    inputList = data?.METHOD ?: beanPreparationMethod,
                    selectedSet = emptySet(),
                    searchText = null,
                    inputType = InputType.METHOD,
                    singleInput = true,
                ),
                InputVD(
                    label = "Roaster",
                    inputList = data?.ROASTER ?: coffeeRoasters,
                    selectedSet = emptySet(),
                    searchText = "",
                    inputType = InputType.ROASTER,
                    singleInput = true,
                ),
                InputRadioVD(
                    label = "Decaf",
                    option = listOf(
                        RadioInputViewData(
                            label = "Caffeinated",
                            isDecaf = false,
                        ),
                        RadioInputViewData(
                            label = "Decaffeinated",
                            isDecaf = true,
                            )
                    ),
                    isDecaf = false,
                ),
            ),
            )
        _viewData.value = Success(
            data = updatedViewData
        )
    }

    fun processIntent(intent: CoffeeInputScreenIntent) {
        when (intent) {
            is CoffeeInputScreenIntent.UpdateRoastDate -> onUpdateRoastData(intent.date)
            is CoffeeInputScreenIntent.SetDecaf -> onToggleDecaf(intent.isDecaf)
            CoffeeInputScreenIntent.SubmitCoffee -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    onEnterPress()
                }
            }

            is CoffeeInputScreenIntent.ToggleDropdownSelection -> {
                handleToggleDropdownSelection(intent.inputType, intent.selection)
            }

            is CoffeeInputScreenIntent.UpdateSearchText -> {
                handleUpdateSearchText(intent.inputType, intent.searchText)
            }

            CoffeeInputScreenIntent.NavigateHome -> {
                // Handle navigation to the home screen
                viewModelScope.launch {
                    _navigationEvent.send(NavigationEvent.NavigateToHome)
                }
            }
        }
    }

    fun onUpdateRoastData(date: LocalDate) {
        val currentViewData = _viewData.value as Success<CoffeeScreenViewData>
        _viewData.update {
            Success(
                data = currentViewData.data.copy(
                    roastDate = date
                )
            )
        }
    }

    fun onToggleDecaf(isDecaf: Boolean) {
        _viewData.update { currentResult ->
            if (currentResult is Success) {
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
                Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                // If the current state is not Success, return it unchanged
                currentResult
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onEnterPress() {
        viewModelScope.launch {
            val currentViewData = _viewData.value as Success<CoffeeScreenViewData>
            val coffeeDto = mapToCoffeeDto(currentViewData.data)
            uploadCoffee(coffeeDto)
        }.invokeOnCompletion {
            if (it == null) {
                viewModelScope.launch {
                    _navigationEvent.send(NavigationEvent.NavigateToHome)

                }
            } else {
                println(it)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun mapToCoffeeDto(viewData: CoffeeScreenViewData): CoffeeDto {
        var beanTypes = emptySet<String>()
        var originCountries = emptySet<String>()
        var tastingNotes = emptySet<String>()
        var beanPreparationMethod = emptySet<String>() // For METHOD
        var roaster: String = ""
        var isDecaf: Boolean? = null
        val formattedRoastDate: String =
            viewData.roastDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""

        viewData.inputs.forEach { input ->
            when (input) {
                is InputViewData.InputVD -> {
                    when (input.inputType) {
                        InputType.BEANS -> beanTypes = input.selectedSet
                        InputType.ORIGIN -> originCountries = input.selectedSet
                        InputType.TASTE -> tastingNotes = input.selectedSet
                        InputType.METHOD -> beanPreparationMethod = input.selectedSet // Map METHOD
                        InputType.ROASTER -> roaster =
                            input.selectedSet.firstOrNull() ?: "no roaster"
                    }
                }

                is InputViewData.InputRadioVD -> {
                    // Assuming the label for the decaf radio button is consistent
                    // or you only have one InputRadioVD for decaf.
                    isDecaf = input.isDecaf
                }
            }
        }

        return CoffeeDto(
            roastDate = formattedRoastDate,
            beanTypes = beanTypes.toList(),
            originCountries = originCountries.toList(),
            tastingNotes = tastingNotes.toList(),
            beanPreparationMethod = beanPreparationMethod.toList(),
            roaster = roaster,
            isDecaf = isDecaf == true,
            label = roaster + " " + originCountries.joinToString(", ") + " " +
                    beanPreparationMethod.first() + " " + formattedRoastDate,
            id = UUID.randomUUID().toString()
        )
    }

    private fun uploadCoffee(coffee: CoffeeDto) {
        val item = mapOf(
            "isDecaf" to coffee.isDecaf,
            "roastDate" to coffee.roastDate,
            "beanTypes" to coffee.beanTypes,
            "originCountries" to coffee.originCountries,
            "tastingNotes" to coffee.tastingNotes,
            "beanPreparationMethod" to coffee.beanPreparationMethod,
            "roaster" to coffee.roaster,
            "label" to coffee.label,
            "userId" to accountService.currentUserId,
            "id" to coffee.id
        )
//        val itemWithUserId = item + ("userId" to accountService.currentUserId)

        val userId = accountService.currentUserId
        val request = Firebase.firestore.collection("coffeeUploads")
        val something = Firebase.auth.addAuthStateListener {
            it.currentUser?.let {
                request.document("${coffee.label}").set(item)
            }
        }

        viewModelScope.launch { doSomething(something) }

//        val collectionReference = FirebaseFirestore.getInstance().collection("$userId")
//        viewModelScope.launch {
//            request.document().collection("$userId").add(mapOf(userId to "id")).await().id
//        }
    }

    private suspend fun doSomething(something: Unit) {
        something
    }

    private fun handleToggleDropdownSelection(inputType: InputType, selection: String) {
        _viewData.update { currentResult ->
            if (currentResult is Success) {
                val currentViewData = currentResult.data
                val updatedInputs = currentViewData.inputs.map { input ->
                    if (input is InputViewData.InputVD && input.inputType == inputType) {
                        val newSelectedSet = if (input.singleInput) {
                            // Logic for single input
                            if (input.selectedSet.contains(selection)) {
                                // If the selected item is clicked again, clear the set
                                emptySet()
                            } else {
                                // Otherwise, replace the set with the new single selection
                                setOf(selection)
                            }
                        } else {
                            // Logic for multi-input (existing logic)
                            if (input.selectedSet.contains(selection)) {
                                input.selectedSet - selection
                            } else {
                                input.selectedSet + selection
                            }
                        }
                        input.copy(selectedSet = newSelectedSet)
                    } else {
                        input
                    }
                }
                Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateSearchText(inputType: InputType, searchText: String) {
        _viewData.update { currentResult ->
            if (currentResult is Success) {
                val currentViewData = currentResult.data
                val updatedInputs = currentViewData.inputs.map { input ->
                    if (input is InputViewData.InputVD && input.inputType == inputType) {
                        input.copy(searchText = searchText)
                    } else {
                        input
                    }
                }
                Success(data = currentViewData.copy(inputs = updatedInputs))
            } else {
                currentResult
            }
        }
    }
}

sealed class NavigationEvent {
    object NavigateToHome : NavigationEvent()
    object NavigateToInput : NavigationEvent()
    data object NavigateToLogin : NavigationEvent()

    //    data class NavigateToDetail(val coffee: CoffeeDto) : NavigationEvent()
    // Add other navigation events here if needed, e.g.:
    data class NavigateToDetail(val coffeeId: String) : NavigationEvent()
}

sealed class CoffeeInputScreenIntent {
    data class UpdateRoastDate(val date: LocalDate) : CoffeeInputScreenIntent()
    data class SetDecaf(val isDecaf: Boolean) : CoffeeInputScreenIntent()
    object SubmitCoffee : CoffeeInputScreenIntent() // For the "Enter" press or submit button
    data class ToggleDropdownSelection(val inputType: InputType, val selection: String) :
        CoffeeInputScreenIntent()

    data class UpdateSearchText(val inputType: InputType, val searchText: String) :
        CoffeeInputScreenIntent()

    object NavigateHome : CoffeeInputScreenIntent()
}

data class CoffeeInputScreenDto(
    @get:PropertyName("BEANS") val BEANS: List<String> = emptyList(),
    @get:PropertyName("CAFFEINE") val CAFFEINE: List<String> = emptyList(),
    @get:PropertyName("METHOD") val METHOD: List<String> = emptyList(),
    @get:PropertyName("ORIGIN") val ORIGIN: List<String> = emptyList(),
    @get:PropertyName("ROASTER") val ROASTER: List<String> = emptyList(),
    @get:PropertyName("TASTE") val TASTE: List<String> = emptyList(),
)