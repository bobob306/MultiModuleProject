package com.bsdevs.homescreen.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.bsdevs.homescreen.HomeScreenRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeScreenRoute

@Serializable
data object HomeScreenBaseRoute

fun NavController.navigateToHome(navOptions: NavOptions) =
    navigate(route = HomeScreenRoute, navOptions)

fun NavGraphBuilder.homeScreenSection(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToCoffee: () -> Unit,
    ) {
    navigation<HomeScreenBaseRoute>(startDestination = HomeScreenRoute) {
        composable<HomeScreenRoute> {
            HomeScreenRoute(
                onShowSnackBar,
                onNavigateToCoffee,
            )
        }
    }
}