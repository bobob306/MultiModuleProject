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
            is RegisterScreenIntent.UpdateBabyGender -> handleUpdateBabyGender(intent.gender)
            is RegisterScreenIntent.SetBabyEntryMethod -> handleSetBabyEntryMethod(intent.method)
            is RegisterScreenIntent.SetDatePickerVisibility -> {
                _viewData.update { currentResult ->
                    if (currentResult is Result.Success) {
                        Result.Success(currentResult.data.copy(isDatePickerVisible = intent.isVisible))
                    } else currentResult
                }
            }
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
            if (data.roles.contains("parent")) {
                when (data.babyEntryMethod) {
                    BabyEntryMethod.BY_ID -> {
                        if (data.babyId.isEmpty()) {
                            _viewData.update { res ->
                                if (res is Result.Success) Result.Success(res.data.copy(babyError = "Existing Baby ID is required.")) else res
                            }
                            return
                        }
                    }

                    BabyEntryMethod.BY_DETAILS -> {
                        if (data.babyFirstName.isEmpty() || data.babyLastName.isEmpty() || data.babyBirthDate.isEmpty() || data.babyGender.isEmpty()) {
                            _viewData.update { res ->
                                if (res is Result.Success) Result.Success(res.data.copy(babyError = "Baby names, birth date and gender are mandatory.")) else res
                            }
                            return
                        }
                    }

                    BabyEntryMethod.NONE -> {
                        _viewData.update { res ->
                            if (res is Result.Success) Result.Success(res.data.copy(babyError = "Please choose how to add your baby.")) else res
                        }
                        return
                    }
                }
            }

            viewModelScope.launch {
                _viewData.update { res ->
                    if (res is Result.Success) Result.Success(
                        res.data.copy(
                            isLoading = true,
                            emailError = null,
                            passwordError = null,
                            babyError = null,
                            generalError = null
                        )
                    ) else res
                }

                try {
                    accountService.signUp(
                        email = data.email,
                        password = data.password
                    )

                    val userId = accountService.currentUserId

                    var finalBabyId: String? = null

                    if (data.babyEntryMethod == BabyEntryMethod.BY_ID && data.babyId.isNotEmpty()) {
                        if (userRepository.babyExists(data.babyId)) {
                            finalBabyId = data.babyId
                        } else {
                            _viewData.update { res ->
                                if (res is Result.Success) Result.Success(
                                    res.data.copy(
                                        isLoading = false,
                                        babyError = "Baby ID does not exist."
                                    )
                                ) else res
                            }
                            return@launch
                        }
                    } else if (data.roles.contains("parent") && data.babyEntryMethod == BabyEntryMethod.BY_DETAILS) {
                        finalBabyId = UUID.randomUUID().toString()
                        userRepository.saveBaby(
                            BabyDto(
                                id = finalBabyId,
                                firstName = data.babyFirstName,
                                lastName = data.babyLastName,
                                middleName = data.babyMiddleName.takeIf { it.isNotEmpty() },
                                birthDate = data.babyBirthDate,
                                gender = data.babyGender
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
                } catch (e: Exception) {
                    Log.e("RegisterViewModel", "Registration failed", e)
                    _viewData.update { res ->
                        if (res is Result.Success) {
                            val message = e.message ?: "An unexpected error occurred"
                            viewModelScope.launch {
                                _navigationEvent.send(RegisterNavigationEvent.Failure(message))
                            }
                            when {
                                message.contains("email", ignoreCase = true) -> {
                                    Result.Success(res.data.copy(isLoading = false, emailError = message))
                                }
                                message.contains("password", ignoreCase = true) -> {
                                    Result.Success(res.data.copy(isLoading = false, passwordError = message))
                                }
                                else -> {
                                    Result.Success(res.data.copy(isLoading = false, generalError = message))
                                }
                            }
                        } else {
                            res
                        }
                    }
                }
            }
        }
    }

    private fun handleUpdateFirstName(firstName: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(firstName = firstName, generalError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateLastName(lastName: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(lastName = lastName, generalError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateMiddleName(middleName: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(middleName = middleName, generalError = null))
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
                Result.Success(currentResult.data.copy(roles = newRoles, babyError = null, generalError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateBabyId(babyId: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(babyId = babyId, babyError = null, generalError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateBabyFirstName(babyFirstName: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(babyFirstName = babyFirstName, babyError = null, generalError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateBabyLastName(babyLastName: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(babyLastName = babyLastName, babyError = null, generalError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateBabyMiddleName(babyMiddleName: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(babyMiddleName = babyMiddleName, babyError = null, generalError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateBabyBirthDate(babyBirthDate: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(babyBirthDate = babyBirthDate, babyError = null, generalError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateBabyGender(gender: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(babyGender = gender, babyError = null, generalError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleSetBabyEntryMethod(method: BabyEntryMethod) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(
                    currentResult.data.copy(
                        babyEntryMethod = method,
                        babyId = "",
                        babyFirstName = "",
                        babyLastName = "",
                        babyMiddleName = "",
                        babyBirthDate = "",
                        babyError = null,
                        generalError = null
                    )
                )
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdateEmail(email: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(email = email, emailError = null, generalError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdatePassword(password: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(password = password, passwordError = null, generalError = null))
            } else {
                currentResult
            }
        }
    }

    private fun handleUpdatePasswordConfirmation(password: String) {
        _viewData.update { currentResult ->
            if (currentResult is Result.Success) {
                Result.Success(currentResult.data.copy(passwordConfirmation = password, passwordError = null, generalError = null))
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
    data class UpdateBabyGender(val gender: String) : RegisterScreenIntent()
    data class SetBabyEntryMethod(val method: BabyEntryMethod) : RegisterScreenIntent()
    data class SetDatePickerVisibility(val isVisible: Boolean) : RegisterScreenIntent()
    data object Register : RegisterScreenIntent()
    data object UpdatePasswordVisibility : RegisterScreenIntent()
    data object UpdatePasswordConfirmationVisibility : RegisterScreenIntent()
    data object NavigateToLogin : RegisterScreenIntent()
}

sealed class RegisterNavigationEvent {
    data object SuccessfulAccountCreation : RegisterNavigationEvent()
    data class Failure(val message: String) : RegisterNavigationEvent()
    data object NavigateToLogin : RegisterNavigationEvent()
}

enum class BabyEntryMethod {
    BY_ID,
    BY_DETAILS,
    NONE
}

data class RegisterScreenViewData(
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val middleName: String = "",
    val roles: List<String> = emptyList(),
    val babyEntryMethod: BabyEntryMethod = BabyEntryMethod.NONE,
    val babyId: String = "",
    val babyFirstName: String = "",
    val babyLastName: String = "",
    val babyMiddleName: String = "",
    val babyBirthDate: String = "",
    val babyGender: String = "",
    val isLoading: Boolean = false,
    val isEnabled: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isPasswordConfirmationVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val babyError: String? = null,
    val generalError: String? = null,
    val isDatePickerVisible: Boolean = false,
)
