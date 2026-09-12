package com.bsdevs.firstscreen.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.data.repository.MetadataRepository
import com.bsdevs.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    private val accountService: AccountService,
    private val userRepository: UserRepository,
    private val metadataRepository: MetadataRepository
) : ViewModel() {
    private val _navigationEvent = Channel<SplashScreenNavigationEvents>()
    val navigationEvent get() = _navigationEvent.receiveAsFlow()

    fun onAppStart() {
        viewModelScope.launch {
            metadataRepository.fetchMetadata()
            
            if (accountService.hasUser()) {
                val user = userRepository.getUser(accountService.currentUserId)
                if (user != null) {
                    _navigationEvent.send(SplashScreenNavigationEvents.NavigateToHomeScreen)
                } else {
                    // If auth exists but no Firestore profile, maybe they need to sign in again or it's a legacy account
                    _navigationEvent.send(SplashScreenNavigationEvents.NavigateToHomeScreen)
                }
            } else {
                _navigationEvent.send(SplashScreenNavigationEvents.NavigateToSignInScreen)
            }
        }
    }
}

sealed class SplashScreenNavigationEvents {
    object NavigateToHomeScreen : SplashScreenNavigationEvents()
    object NavigateToSignInScreen : SplashScreenNavigationEvents()
}
