package com.bsdevs.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
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
    private val accountService: AccountService
) : ViewModel() {
    private val _viewData = MutableStateFlow<Result<LoginViewData>>(value = Result.Loading)
    val viewData: StateFlow<Result<LoginViewData>>
        get() = _viewData.onStart {
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
    val navigationEvent = _navigationEvent.receiveAsFlow() // Expose as Flow

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

//    val isEnabled = {
//        if (viewData.value is Result.Success) {
//            val currentViewData = viewData.value as Result.Success
//            if (currentViewData.data.email.isNotEmpty() && currentViewData.data.password.isNotEmpty()) {
//                _viewData.update {
//                    Result.Success(
//                        LoginViewData(
//                            email = currentViewData.data.email,
//                            password = currentViewData.data.password,
//                            isLoading = currentViewData.data.isLoading,
//                            isEnabled = true
//                        )
//                    )
//                }
//            }
//        }
//    }

    private fun onLoginClick() {
        println("start to do something")
        if (_viewData.value is Result.Success) {
            println("do something")
            viewModelScope.launch {
                CoroutineExceptionHandler { _, throwable ->
                    Log.d("COFFEE_ERROR_TAG", throwable.message.orEmpty())
                    println("COFFEE_ERROR_TAG " + throwable.message.orEmpty())
                }
                val currentViewData = _viewData.value as Result.Success
                accountService.signIn(
                    email = currentViewData.data.email,
                    password = currentViewData.data.password
                )
            }.invokeOnCompletion {
                it?.let {
                    println("COFFEE_ERROR_TAG " + it.toString())
                } ?: runCatching{
                    println("COFFEE_ERROR_TAG " + "success???")
                }
//                if (it == null) {
//                    viewModelScope.launch {
//                        println("COFFEE_ERROR_TAG " + "success???")
//                        _navigationEvent.send(NavigationEvent.NavigateToCoffeeHome)
//                    }
//                } else {
//                    println(it)
//                    println("fail to do something")
//                    _viewData.update {
//                        Result.Success(
//                            LoginViewData(
//                                email = (it as Result.Success).data.email,
//                                password = it.data.password,
//                                isLoading = false,
//                                isEnabled = it.data.isEnabled
//                            )
//                        )
//                    }
//                }
            }
        }
    }

    private fun onRegisterClick() {
        viewModelScope.launch {
            _navigationEvent.send(NavigationEvent.NavigateToRegister)
        }
    }

    private fun onUpdateEmail(email: String) {
        if (_viewData.value is Result.Success) {
            val currentViewData = _viewData.value as Result.Success
            _viewData.value =
                Result.Success(
                    LoginViewData(
                        email = email,
                        password = currentViewData.data.password,
                        isLoading = currentViewData.data.isLoading,
                        isEnabled = currentViewData.data.isEnabled
                    )
                )

        }
    }

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

    private fun onUpdatePassword(password: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                val currentViewData = currentResult.data
                val visible = if (password.isEmpty()) false else currentViewData.isPasswordVisible
                val updatedViewData =
                    currentViewData.copy(password = password, isPasswordVisible = visible)
                Result.Success(updatedViewData)

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
    val isPasswordVisible: Boolean = false
)