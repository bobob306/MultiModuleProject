package com.bsdevs.firstscreen.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.network.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    private val accountService: AccountService,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _navigationEvent = Channel<SplashScreenNavigationEvents>()
    val navigationEvent get() = _navigationEvent.receiveAsFlow()

    fun onAppStart() {
        if (accountService.hasUser()) {
            viewModelScope.launch {
                val user = userRepository.getUser(accountService.currentUserId)
                if (user != null) {
                    _navigationEvent.send(SplashScreenNavigationEvents.NavigateToHomeScreen)
                } else {
                    // If auth exists but no Firestore profile, maybe they need to sign in again or it's a legacy account
                    _navigationEvent.send(SplashScreenNavigationEvents.NavigateToHomeScreen)
                }
            }
        } else {
            viewModelScope.launch {
                _navigationEvent.send(SplashScreenNavigationEvents.NavigateToSignInScreen)
            }
        }
    }
}

sealed class SplashScreenNavigationEvents {
    object NavigateToHomeScreen : SplashScreenNavigationEvents()
    object NavigateToSignInScreen : SplashScreenNavigationEvents()
}