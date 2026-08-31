package com.bsdevs.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.ScreenDataMapper
import com.bsdevs.network.repository.FormRepository
import com.bsdevs.network.repository.ScreenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: ScreenRepository,
    private val formRepository: FormRepository,
    private val mapper: ScreenDataMapper,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    init {
        android.util.Log.d("HomeScreenViewModel", "ViewModel Initialized: $this")
        seedForms()
    }

    // To add a new SDUI form:
    // 1. Add the seed data to FormSeeds.kt
    // 2. Call seedFormIfAbsent here with the new formId
    // 3. Add a matching branch to FormSubmitRouter, FormPrefillerImpl, FormDeleterImpl
    // 4. To update an existing form after it has been seeded, delete the Firestore
    //    document manually (Firebase console) - it will be re-seeded on next launch.
    //    See FormSeedsTest for field type reference and full documentation.
    private fun seedForms() {
        viewModelScope.launch(dispatchers.io) {
            formRepository.seedFormIfAbsent("coffeeLog", FormSeeds.coffeeLog)
            formRepository.seedFormIfAbsent("nappyLog", FormSeeds.nappyLog)
            formRepository.seedFormIfAbsent("temperatureLog", FormSeeds.temperatureLog)
            formRepository.seedFormIfAbsent("measurementLog", FormSeeds.measurementLog)
        }
    }

    private val _viewData = MutableStateFlow<Result<List<NetworkScreenData>>>(value = Result.Loading)
    val viewData: StateFlow<Result<List<NetworkScreenData>>> = _viewData.onStart {
            getScreen()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    fun getScreen() {
        android.util.Log.d("HomeScreenViewModel", "getScreen() called")
        viewModelScope.launch {
            repository.getScreenFlow("home").collect { result ->
                android.util.Log.d("HomeScreenViewModel", "Received result: $result")
                when (result) {
                    is Result.Success -> {
                        val mappedData = withContext(dispatchers.default) {
                            mapper.mapToData(result.data)
                        }
                        _viewData.update { Result.Success(mappedData) }
                    }

                    is Result.Error -> {
                        _viewData.update { result }
                    }

                    is Result.Loading -> {
                        _viewData.update { Result.Loading }
                    }
                }
            }

        }
    }

    fun click(destination: String, label: String) {
        println("Destination: $destination, Label: $label")
    }
}
