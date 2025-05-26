package com.bsdevs.firstscreen.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavOptions
import kotlinx.coroutines.delay
private const val SPLASH_TIMEOUT = 1000L

@Composable
fun SplashScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToCoffeeHome: (navOptions: NavOptions?) -> Unit,
    onNavigateToSignIn: (navOptions: NavOptions?) -> Unit,
    viewModel: SplashScreenViewModel = hiltViewModel()
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
    }
    LaunchedEffect(true) {
        delay(SPLASH_TIMEOUT)
        viewModel.onAppStart()
    }
    LaunchedEffect(key1 = Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                SplashScreenNavigationEvents.NavigateToHomeScreen -> onNavigateToCoffeeHome
                SplashScreenNavigationEvents.NavigateToSignInScreen -> onNavigateToSignIn.invoke(null)
            }
        }
    }
}