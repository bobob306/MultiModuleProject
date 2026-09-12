package com.bsdevs.babycare.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.core.domain.BabyCareRepository
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.ScreenDataMapper
import com.bsdevs.data.repository.ScreenRepository
import com.bsdevs.data.repository.UserRepository
import com.bsdevs.network.dto.ScreenDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GenericSduiViewModel @Inject constructor(
    private val screenRepository: ScreenRepository,
    private val userRepository: UserRepository,
    private val mapper: ScreenDataMapper,
    private val dispatchers: DispatcherProvider,
    private val babyRepository: BabyCareRepository,
    private val accountService: AccountService,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(value = false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val uiStateCache = mutableMapOf<String, StateFlow<Result<List<NetworkScreenData>>>>()

    fun getUiState(screenId: String): StateFlow<Result<List<NetworkScreenData>>> {
        return uiStateCache.getOrPut(screenId) {
            combine(
                screenRepository.getScreenFlow(screenId),
                userRepository.userProfile
            ) { screenResult, user ->
                when (screenResult) {
                    is Result.Success -> {
                        val filteredDtos = screenResult.data.filter { dto ->
                            val requiredRoles = dto.requiredRoles
                            requiredRoles == null || user?.roles?.any { it in requiredRoles } == true
                        }.map { dto ->
                            // Also filter tiles in TileRowDto
                            when (dto) {
                                is ScreenDto.TileRowDto -> dto.copy(
                                    tiles = dto.tiles.filter { tile ->
                                        val tileRoles = tile.requiredRoles
                                        tileRoles == null || user?.roles?.any { it in tileRoles } == true
                                    }
                                )
                                else -> dto
                            }
                        }
                        Result.Success(withContext(dispatchers.default) {
                            mapper.mapToData(filteredDtos)
                        })
                    }
                    is Result.Error -> screenResult
                    Result.Loading -> Result.Loading
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = Result.Loading
            )
        }
    }

    fun refresh(screenId: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Force refresh screen config
                screenRepository.getScreenFlow(screenId, forceRefresh = true).collect { }
                // Force refresh baby data (since components depend on it)
                babyRepository.refreshData(accountService.currentUserId, 20)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
