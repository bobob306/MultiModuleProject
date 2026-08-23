package com.bsdevs.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.bsdevs.babycare.presentation.navigation.babyCareSection
import com.bsdevs.babycare.presentation.navigation.navigateToBabyCareHome
import com.bsdevs.coffeescreen.navigation.coffeeScreenSection
import com.bsdevs.coffeescreen.navigation.navigateToCoffeeDetail
import com.bsdevs.coffeescreen.navigation.navigateToCoffeeHome
import com.bsdevs.coffeescreen.navigation.navigateToCoffeeInput
import com.bsdevs.firstscreen.navigation.SplashScreenBaseRoute
import com.bsdevs.firstscreen.navigation.splashScreenSection
import com.bsdevs.homescreen.navigation.homeScreenSection
import com.bsdevs.login.loginScreenSection
import com.bsdevs.login.navigateToLoginScreen
import com.bsdevs.login.navigateToRegisterScreen

@Composable
fun MMPNavHost(
    navController: NavHostController,
    onShowSnackBar: suspend (String, String?) -> Unit,
    modifier: Modifier,
    rootPadding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = SplashScreenBaseRoute,
        modifier = modifier.padding(
            bottom = rootPadding.calculateBottomPadding()
        )
    ) {
        homeScreenSection(
            onShowSnackBar = onShowSnackBar,
            onNavigateToCoffee = navController::navigateToCoffeeHome,
            onNavigateToBabyCare = navController::navigateToBabyCareHome,
        )
        coffeeScreenSection(
            onShowSnackBar,
            navigateToCoffeeInput = navController::navigateToCoffeeInput,
            navigateToCoffeeHome = navController::navigateToCoffeeHome,
            navigateToLogin = navController::navigateToLoginScreen,
            navigateToCoffeeDetail = navController::navigateToCoffeeDetail,
        )
        loginScreenSection(
            onShowSnackBar,
            onNavigateToCoffeeHome = navController::navigateToCoffeeHome,
            onNavigateToLogin = navController::navigateToLoginScreen,
            onNavigateToRegisterScreen = navController::navigateToRegisterScreen,
        )
        splashScreenSection(
            onShowSnackBar,
            onNavigateToBabyHome = navController::navigateToBabyCareHome,
            onNavigateToSignIn = navController::navigateToLoginScreen,
        )
        babyCareSection(
            navController = navController,
            onShowSnackBar = onShowSnackBar
        )
    }
}
