package com.bsdevs.login.registerscreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
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

@HiltViewModel
class RegisterScreenViewModel @Inject constructor(
    private val accountService: AccountService
) : ViewModel() {
    private val _viewData = MutableStateFlow<Result<RegisterScreenViewData>>(value = Result.Loading)
    val viewData: StateFlow<Result<RegisterScreenViewData>> = _viewData.onStart {
            getInitialViewData()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    private val _navigationEvent = Channel<RegisterNavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow() // Expose as Flow

    private fun getInitialViewData() {
        if (_viewData.value is Result.Success) return
        _viewData.value = Result.Success(RegisterScreenViewData())
    }

    fun processIntent(intent: RegisterScreenIntent) {
        when (intent) {
            is RegisterScreenIntent.UpdateEmail -> handleUpdateEmail(intent.email)
            is RegisterScreenIntent.UpdatePassword -> handleUpdatePassword(intent.password)
            is RegisterScreenIntent.UpdatePasswordConfirmation -> handleUpdatePasswordConfirmation(
                intent.passwordConfirmation
            )

            RegisterScreenIntent.Register -> onRegisterClick()
            RegisterScreenIntent.UpdatePasswordVisibility -> handleUpdatePasswordVisibility()
            RegisterScreenIntent.UpdatePasswordConfirmationVisibility -> handleUpdatePasswordConfirmationVisibility()
            RegisterScreenIntent.NavigateToLogin -> onNavigateToLogin()
        }
    }

    private fun onRegisterClick() {
        if (_viewData.value is Result.Success) {
            val currentViewData = _viewData.value as Result.Success
            launchCatching {
                _viewData.update {
                    currentViewData.copy(
                        currentViewData.data.copy(
                            isLoading = true
                        )
                    )
                }
                accountService.signUp(
                    email = currentViewData.data.email,
                    password = currentViewData.data.password
                )
            }.invokeOnCompletion {
                if (it == null) {
                    viewModelScope.launch {
                        _navigationEvent.send(RegisterNavigationEvent.NavigateToLogin)
                    }
                }
                it?.let { throwable ->
                    _viewData.update {
                        Result.Success(
                            RegisterScreenViewData(
                                email = currentViewData.data.email,
                                password = currentViewData.data.password,
                                passwordConfirmation = currentViewData.data.passwordConfirmation,
                                isLoading = false,
                                emailError = "Please ensure this is a valid email and not already in use.",
                                passwordError = "Please ensure the passwords match and are valid."
                            )
                        )
                    }
                }
            }
        }

    }

    private fun launchCatching(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                Log.d("COFFEE_ERROR_TAG", throwable.message.orEmpty())
            },
            block = block
        )

    private fun handleUpdateEmail(email: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                val currentViewData = currentResult.data
                val updatedViewData = currentViewData.copy(email = email)
                Result.Success(updatedViewData)
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdatePassword(password: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                val currentViewData = currentResult.data
                val updatedViewData = currentViewData.copy(password = password)
                Result.Success(updatedViewData)
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdatePasswordConfirmation(password: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                val currentViewData = currentResult.data
                val updatedViewData = currentViewData.copy(passwordConfirmation = password)
                Result.Success(updatedViewData)
            } else {
                currentResult
            }
        }
    }

    private fun onNavigateToLogin() {
        viewModelScope.launch {
            _navigationEvent.send(RegisterNavigationEvent.NavigateToLogin)
        }
    }

    private fun handleUpdatePasswordVisibility() {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(isPasswordVisible = !currentResult.data.isPasswordVisible))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdatePasswordConfirmationVisibility() {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(isPasswordConfirmationVisible = !currentResult.data.isPasswordConfirmationVisible))
            } else {
                currentResult
            }
        }
    }
}

sealed class RegisterScreenIntent {
    data class UpdateEmail(val email: String) : RegisterScreenIntent()
    data class UpdatePassword(val password: String) : RegisterScreenIntent()
    data class UpdatePasswordConfirmation(val passwordConfirmation: String) : RegisterScreenIntent()
    data object Register : RegisterScreenIntent()
    data object UpdatePasswordVisibility : RegisterScreenIntent()
    data object UpdatePasswordConfirmationVisibility : RegisterScreenIntent()
    data object NavigateToLogin : RegisterScreenIntent()
}

sealed class RegisterNavigationEvent {
    data object SuccessfulAccountCreation : RegisterNavigationEvent()
    data object NavigateToLogin : RegisterNavigationEvent()
}

data class RegisterScreenViewData(
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val isLoading: Boolean = false,
    val isEnabled: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isPasswordConfirmationVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
)