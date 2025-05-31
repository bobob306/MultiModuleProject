package com.bsdevs.login

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.bsdevs.login.loginscreen.LoginScreenRoute
import com.bsdevs.login.registerscreen.RegisterScreenRoute
import kotlinx.serialization.Serializable

@Serializable
data object LoginScreenRoute

@Serializable
data object LoginScreenBaseRoute

@Serializable
data object RegisterScreenRoute

@Serializable
data object RegisterScreenBaseRoute

fun NavController.navigateToLoginScreen(navOptions: NavOptions?) {
    navigate(route = LoginScreenRoute, navOptions = navOptions)
}

fun NavController.navigateToRegisterScreen(navOptions: NavOptions?) {
    navigate(route = RegisterScreenRoute, navOptions = navOptions)
}

fun NavGraphBuilder.loginScreenSection(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToCoffeeHome: (navOptions: NavOptions?) -> Unit,
    onNavigateToLogin: (navOptions: NavOptions?) -> Unit,
    onNavigateToRegisterScreen: (navOptions: NavOptions?) -> Unit,
) {
    navigation<LoginScreenBaseRoute>(startDestination = LoginScreenRoute) {
        composable<LoginScreenRoute> {
            LoginScreenRoute(
                onShowSnackBar,
                onNavigateToCoffeeHome,
                onNavigateToRegisterScreen,
            )
        }
        composable<RegisterScreenRoute> {
            RegisterScreenRoute(
                onShowSnackBar,
                onNavigateToLogin,
            )
        }
    }
}