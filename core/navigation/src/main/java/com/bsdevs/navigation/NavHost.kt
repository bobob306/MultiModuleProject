package com.bsdevs.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.bsdevs.babycare.presentation.navigation.babyCareSection
import com.bsdevs.babycare.presentation.navigation.navigateToBabyCareHome
import com.bsdevs.coffeescreen.navigation.coffeeScreenSection
import com.bsdevs.coffeescreen.navigation.navigateToCoffeeDetail
import com.bsdevs.coffeescreen.navigation.navigateToCoffeeHome
import com.bsdevs.coffeescreen.navigation.navigateToCoffeeInput
import com.bsdevs.firstscreen.navigation.SplashScreenBaseRoute
import com.bsdevs.firstscreen.navigation.splashScreenSection
import com.bsdevs.homescreen.navigation.HomeScreenBaseRoute
import com.bsdevs.homescreen.navigation.homeScreenSection
import com.bsdevs.homescreen.navigation.settingsSection
import com.bsdevs.login.loginScreenSection
import com.bsdevs.login.navigateToLoginScreen
import com.bsdevs.login.navigateToRegisterScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MMPNavHost(
    navController: NavHostController,
    userRoles: List<String>,
    onShowSnackBar: suspend (String, String?) -> Unit,
    modifier: Modifier,
    rootPadding: PaddingValues,
) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = SplashScreenBaseRoute,
            modifier = modifier.padding(
                bottom = rootPadding.calculateBottomPadding()
            )
        ) {
            homeScreenSection(
                onShowSnackBar = onShowSnackBar,
                onNavigateToCoffee = {
                    if (userRoles.contains("coffee")) {
                        navController.navigateToCoffeeHome()
                    } else {
                        navController.navigate(HomeScreenBaseRoute)
                    }
                },
                onNavigateToBabyCare = {
                    if (userRoles.contains("parent")) {
                        navController.navigateToBabyCareHome()
                    } else {
                        navController.navigate(HomeScreenBaseRoute)
                    }
                },
            )
            settingsSection(
                onShowSnackBar = onShowSnackBar,
                onLogout = {
                    navController.navigate(SplashScreenBaseRoute) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                }
            )
            coffeeScreenSection(
                onShowSnackBar,
                navigateToCoffeeInput = navController::navigateToCoffeeInput,
                navigateToCoffeeHome = navController::navigateToCoffeeHome,
                navigateToLogin = navController::navigateToLoginScreen,
                navigateToCoffeeDetail = navController::navigateToCoffeeDetail,
                sharedTransitionScope = this@SharedTransitionLayout
            )
            loginScreenSection(
                onShowSnackBar,
                onNavigateToCoffeeHome = {
                    if (userRoles.contains("coffee")) {
                        navController.navigateToCoffeeHome()
                    } else {
                        navController.navigate(HomeScreenBaseRoute)
                    }
                },
                onNavigateToLogin = navController::navigateToLoginScreen,
                onNavigateToRegisterScreen = navController::navigateToRegisterScreen,
            )
            splashScreenSection(
                onShowSnackBar,
                onNavigateToBabyHome = {
                    if (userRoles.contains("parent")) {
                        navController.navigateToBabyCareHome()
                    } else {
                        navController.navigate(HomeScreenBaseRoute)
                    }
                },
                onNavigateToSignIn = navController::navigateToLoginScreen,
            )
            babyCareSection(
                navController = navController,
                onShowSnackBar = onShowSnackBar,
                sharedTransitionScope = this@SharedTransitionLayout
            )
        }
    }
}
