package com.bsdevs.firstscreen.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class SplashScreenViewModel @Inject constructor(
    private val accountService: AccountService
) : ViewModel() {
    private val _navigationEvent = Channel<SplashScreenNavigationEvents>()
    val navigationEvent get() = _navigationEvent.receiveAsFlow()
    fun onAppStart() {
        if (accountService.hasUser()) {
            viewModelScope.launch {
                _navigationEvent.send(SplashScreenNavigationEvents.NavigateToHomeScreen)
                // Navigate to the home screen
            }
        } else {
            // Navigate to the sign-in screen
            viewModelScope.launch {
                _navigationEvent.send(SplashScreenNavigationEvents.NavigateToHomeScreen)
            }
        }
    }
}

sealed class SplashScreenNavigationEvents {
    object NavigateToHomeScreen : SplashScreenNavigationEvents()
    object NavigateToSignInScreen : SplashScreenNavigationEvents()
}