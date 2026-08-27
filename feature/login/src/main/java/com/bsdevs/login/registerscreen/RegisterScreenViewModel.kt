package com.bsdevs.login.registerscreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.network.dto.BabyDto
import com.bsdevs.network.dto.UserDto
import com.bsdevs.network.repository.UserRepository
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
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RegisterScreenViewModel @Inject constructor(
    private val accountService: AccountService,
    private val userRepository: UserRepository,
    private val dispatchers: DispatcherProvider
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
    val navigationEvent = _navigationEvent.receiveAsFlow()

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
            is RegisterScreenIntent.UpdateFirstName -> handleUpdateFirstName(intent.firstName)
            is RegisterScreenIntent.UpdateLastName -> handleUpdateLastName(intent.lastName)
            is RegisterScreenIntent.UpdateMiddleName -> handleUpdateMiddleName(intent.middleName)
            is RegisterScreenIntent.ToggleRole -> handleToggleRole(intent.role)
            is RegisterScreenIntent.UpdateBabyId -> handleUpdateBabyId(intent.babyId)
            is RegisterScreenIntent.UpdateBabyFirstName -> handleUpdateBabyFirstName(intent.babyFirstName)
            is RegisterScreenIntent.UpdateBabyLastName -> handleUpdateBabyLastName(intent.babyLastName)
            is RegisterScreenIntent.UpdateBabyMiddleName -> handleUpdateBabyMiddleName(intent.babyMiddleName)
            is RegisterScreenIntent.UpdateBabyBirthDate -> handleUpdateBabyBirthDate(intent.babyBirthDate)
            RegisterScreenIntent.Register -> onRegisterClick()
            RegisterScreenIntent.UpdatePasswordVisibility -> handleUpdatePasswordVisibility()
            RegisterScreenIntent.UpdatePasswordConfirmationVisibility -> handleUpdatePasswordConfirmationVisibility()
            RegisterScreenIntent.NavigateToLogin -> onNavigateToLogin()
        }
    }

    private fun onRegisterClick() {
        val currentResult = _viewData.value
        if (currentResult is Result.Success) {
            val data = currentResult.data
            
            // Validation for parent role
            if (data.roles.contains("parent") && data.babyId.isEmpty()) {
                if (data.babyFirstName.isEmpty() || data.babyLastName.isEmpty() || data.babyBirthDate.isEmpty()) {
                    _viewData.update { res ->
                        if (res is Result.Success) Result.Success(res.data.copy(babyError = "Baby first name, last name and birth date are mandatory for parents without an existing Baby ID.")) else res
                    }
                    return
                }
            }

            launchCatching {
                _viewData.update { res ->
                    if (res is Result.Success) Result.Success(res.data.copy(isLoading = true)) else res
                }
                
                accountService.signUp(
                    email = data.email,
                    password = data.password
                )
                
                val userId = accountService.currentUserId
                
                var finalBabyId: String? = null
                
                if (data.babyId.isNotEmpty()) {
                    if (userRepository.babyExists(data.babyId)) {
                        finalBabyId = data.babyId
                    }
                } else if (data.roles.contains("parent")) {
                    finalBabyId = UUID.randomUUID().toString()
                    userRepository.saveBaby(
                        BabyDto(
                            id = finalBabyId,
                            firstName = data.babyFirstName,
                            lastName = data.babyLastName,
                            middleName = data.babyMiddleName.takeIf { it.isNotEmpty() },
                            birthDate = data.babyBirthDate
                        )
                    )
                }

                userRepository.saveUser(
                    UserDto(
                        id = userId,
                        firstName = data.firstName,
                        lastName = data.lastName,
                        middleName = data.middleName.takeIf { it.isNotEmpty() },
                        roles = data.roles,
                        babyId = finalBabyId,
                        babyIds = finalBabyId?.let { listOf(it) } ?: emptyList()
                    )
                )

                _navigationEvent.send(RegisterNavigationEvent.SuccessfulAccountCreation)
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
                                    emailError = "Please ensure this is a valid email and not already in use.",
                                    passwordError = "Please ensure the passwords match and are valid."
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

    private fun handleUpdateFirstName(firstName: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(firstName = firstName))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateLastName(lastName: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(lastName = lastName))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateMiddleName(middleName: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(middleName = middleName))
            } else {
                currentResult
            }
        }
    }

    private fun handleToggleRole(role: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                val currentRoles = currentResult.data.roles
                val newRoles = if (currentRoles.contains(role)) {
                    currentRoles.filter { it != role }
                } else {
                    currentRoles + role
                }
                Result.Success(currentResult.data.copy(roles = newRoles, babyError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateBabyId(babyId: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(babyId = babyId, babyError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateBabyFirstName(babyFirstName: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(babyFirstName = babyFirstName, babyError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateBabyLastName(babyLastName: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(babyLastName = babyLastName, babyError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateBabyMiddleName(babyMiddleName: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(babyMiddleName = babyMiddleName, babyError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateBabyBirthDate(babyBirthDate: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(babyBirthDate = babyBirthDate, babyError = null))
            } else {
                currentResult
            }
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

    private fun handleUpdatePassword(password: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(password = password, passwordError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdatePasswordConfirmation(password: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(passwordConfirmation = password, passwordError = null))
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
    data class UpdateFirstName(val firstName: String) : RegisterScreenIntent()
    data class UpdateLastName(val lastName: String) : RegisterScreenIntent()
    data class UpdateMiddleName(val middleName: String) : RegisterScreenIntent()
    data class ToggleRole(val role: String) : RegisterScreenIntent()
    data class UpdateBabyId(val babyId: String) : RegisterScreenIntent()
    data class UpdateBabyFirstName(val babyFirstName: String) : RegisterScreenIntent()
    data class UpdateBabyLastName(val babyLastName: String) : RegisterScreenIntent()
    data class UpdateBabyMiddleName(val babyMiddleName: String) : RegisterScreenIntent()
    data class UpdateBabyBirthDate(val babyBirthDate: String) : RegisterScreenIntent()
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
    val firstName: String = "",
    val lastName: String = "",
    val middleName: String = "",
    val roles: List<String> = emptyList(),
    val babyId: String = "",
    val babyFirstName: String = "",
    val babyLastName: String = "",
    val babyMiddleName: String = "",
    val babyBirthDate: String = "",
    val isLoading: Boolean = false,
    val isEnabled: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isPasswordConfirmationVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val babyError: String? = null,
)
