package com.bsdevs.firstscreen.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.bsdevs.firstscreen.screen.SplashScreenRoute
import kotlinx.serialization.Serializable

@Serializable
data object SplashScreenRoute

@Serializable
data object SplashScreenBaseRoute

fun NavController.navigateToSplash(navOptions: NavOptions) =
    navigate(route = SplashScreenRoute, navOptions)

fun NavGraphBuilder.splashScreenSection(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToCoffeeHome: () -> Unit,
) {
    navigation<SplashScreenBaseRoute>(startDestination = SplashScreenBaseRoute) {
        composable<SplashScreenRoute> {
            SplashScreenRoute(
                onShowSnackBar,
                onNavigateToCoffeeHome,
            )
        }
    }
}