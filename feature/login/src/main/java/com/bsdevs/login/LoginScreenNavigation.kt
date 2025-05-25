package com.bsdevs.login

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.bsdevs.login.LoginScreenRoute
import kotlinx.serialization.Serializable

@Serializable
data object LoginScreenRoute

@Serializable
data object LoginScreenBaseRoute

fun NavController.navigateToLoginScreen(navOptions: NavOptions?) {
    navigate(route = LoginScreenRoute, navOptions = navOptions)
}

fun NavGraphBuilder.loginScreenSection(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToCoffeeHome: (navOptions: NavOptions?) -> Unit,
) {
    navigation<LoginScreenBaseRoute>(startDestination = LoginScreenRoute) {
        composable<LoginScreenRoute> {
            LoginScreenRoute(
                onShowSnackBar,
                onNavigateToCoffeeHome,
            )
        }
    }
}