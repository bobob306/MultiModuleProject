package com.bsdevs.babycare.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.ScreenDataMapper
import com.bsdevs.network.repository.ScreenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GenericSduiViewModel @Inject constructor(
    private val screenRepository: ScreenRepository,
    private val mapper: ScreenDataMapper,
    private val dispatchers: DispatcherProvider,
    private val babyRepository: BabyCareRepository,
    private val accountService: AccountService,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(value = false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun getUiState(screenId: String): StateFlow<Result<List<NetworkScreenData>>> = flow {
        emitAll(screenRepository.getScreenFlow(screenId).map { result ->
            when (result) {
                is Result.Success -> Result.Success(withContext(dispatchers.default) { mapper.mapToData(result.data) })
                is Result.Error -> result
                Result.Loading -> Result.Loading
            }
        })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Result.Loading
    )

    fun refresh(screenId: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Force refresh screen config
            screenRepository.getScreenFlow(screenId, forceRefresh = true).collect { }
            // Force refresh baby data (since components depend on it)
            babyRepository.refreshData(accountService.currentUserId, 20)
            _isRefreshing.value = false
        }
    }
}
