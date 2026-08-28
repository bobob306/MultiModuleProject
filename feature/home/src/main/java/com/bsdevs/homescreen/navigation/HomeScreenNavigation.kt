package com.bsdevs.homescreen.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.bsdevs.homescreen.HomeScreenRoute
import com.bsdevs.homescreen.presentation.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeScreenRoute

@Serializable
data object HomeScreenBaseRoute

@Serializable
data object SettingsRoute

@Serializable
data object SettingsBaseRoute

fun NavController.navigateToHome(navOptions: NavOptions) =
    navigate(route = HomeScreenRoute, navOptions)

fun NavController.navigateToSettings(navOptions: NavOptions? = null) {
    if (navOptions != null) {
        navigate(route = SettingsRoute, navOptions)
    } else {
        navigate(route = SettingsRoute)
    }
}

fun NavGraphBuilder.homeScreenSection(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToCoffee: () -> Unit,
    onNavigateToBabyCare: () -> Unit,
) {
    navigation<HomeScreenBaseRoute>(startDestination = HomeScreenRoute) {
        composable<HomeScreenRoute> {
            HomeScreenRoute(
                onShowSnackBar,
                onNavigateToCoffee,
                onNavigateToBabyCare,
            )
        }
    }
}

fun NavGraphBuilder.settingsSection(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onLogout: () -> Unit,
) {
    navigation<SettingsBaseRoute>(startDestination = SettingsRoute) {
        composable<SettingsRoute> {
            SettingsRoute(onShowSnackBar, onLogout)
        }
    }
}
