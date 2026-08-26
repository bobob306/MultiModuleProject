package com.bsdevs.login.loginscreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.common.DispatcherProvider
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
class LoginScreenViewModel @Inject constructor(
    private val accountService: AccountService,
    private val dispatchers: DispatcherProvider
) : ViewModel() {
    private val _viewData = MutableStateFlow<Result<LoginViewData>>(value = Result.Loading)
    val viewData: StateFlow<Result<LoginViewData>> = _viewData.onStart {
            getInitialViewData()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    private fun getInitialViewData() {
        if (_viewData.value is Result.Success) return
        _viewData.value = Result.Success(LoginViewData())
    }

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    fun processIntent(intent: LoginScreenIntent) {
        when (intent) {
            is LoginScreenIntent.UpdateEmail -> handleUpdateEmail(intent.email)
            is LoginScreenIntent.UpdatePassword -> onUpdatePassword(intent.password)
            LoginScreenIntent.Login -> onLoginClick()
            LoginScreenIntent.Register -> onRegisterClick()
            LoginScreenIntent.UpdatePasswordVisibility -> handleUpdatePasswordVisibility()
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

    private fun onLoginClick() {
        val currentResult = _viewData.value
        if (currentResult is Result.Success) {
            val data = currentResult.data
            launchCatching {
                _viewData.update { res ->
                    if (res is Result.Success) Result.Success(res.data.copy(isLoading = true)) else res
                }
                accountService.signIn(
                    email = data.email,
                    password = data.password,
                )
                _navigationEvent.send(NavigationEvent.NavigateToCoffeeHome)
                _viewData.update { res ->
                    if (res is Result.Success) Result.Success(res.data.copy(isLoading = false)) else res
                }
            }.invokeOnCompletion { throwable ->
                if (throwable != null) {
                    _viewData.update { res ->
                        if (res is Result.Success) {
                            Result.Success(
                                res.data.copy(
                                    isLoading = false,
                                    emailError = "The details you have provided do not match our records."
                                )
                            )
                        } else {
                            res
                        }
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

    private fun onRegisterClick() {
        viewModelScope.launch {
            _navigationEvent.send(NavigationEvent.NavigateToRegister)
        }
    }

    private fun handleUpdateEmail(email: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(email = email, emailError = null))
            } else {
                currentResult
            }
        }
    }

    private fun onUpdatePassword(password: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                val data = currentResult.data
                val visible = if (password.isEmpty()) false else data.isPasswordVisible
                Result.Success(data.copy(password = password, isPasswordVisible = visible, emailError = null))
            } else {
                currentResult
            }
        }
    }
}

sealed class LoginScreenIntent {
    data class UpdateEmail(val email: String) : LoginScreenIntent()
    data class UpdatePassword(val password: String) : LoginScreenIntent()
    data object Login : LoginScreenIntent()
    data object Register : LoginScreenIntent()
    data object UpdatePasswordVisibility : LoginScreenIntent()
}

sealed class NavigationEvent {
    data object NavigateToCoffeeHome : NavigationEvent()
    data object NavigateToRegister : NavigationEvent()
}

data class LoginViewData(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isEnabled: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val otherError: String? = null
)