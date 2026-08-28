package com.bsdevs.homescreen.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.network.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userName: String = "",
    val babyName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val accountDeleted: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val accountService: AccountService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = accountService.currentUserId
            val user = userRepository.getUser(userId)
            if (user != null) {
                val baby = user.babyId?.let { userRepository.getBaby(it) }
                _uiState.update {
                    it.copy(
                        userName = "${user.firstName} ${user.lastName}",
                        babyName = baby?.firstName,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load user data") }
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = accountService.currentUserId
            try {
                // 1. Delete data from Firestore
                userRepository.deleteUserData(userId)
                // 2. Wipe cache
                userRepository.clearCache()
                // 3. Delete the auth account
                accountService.deleteAccount()
                _uiState.update { it.copy(isLoading = false, accountDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to delete account") }
            }
        }
    }

    fun signOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                userRepository.clearCache()
                accountService.signOut()
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Logout failed") }
            }
        }
    }
}
