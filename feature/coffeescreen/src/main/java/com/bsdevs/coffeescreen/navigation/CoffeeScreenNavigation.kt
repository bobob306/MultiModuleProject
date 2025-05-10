package com.bsdevs.coffeescreen.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import com.bsdevs.coffeescreen.CoffeeScreenRoute

@Serializable
data object CoffeeScreenRoute

@Serializable
data object CoffeeScreenBaseRoute

fun NavController.navigateToCoffee(navOptions: NavOptions? = null) =
    navigate(route = CoffeeScreenRoute, navOptions = navOptions)

fun NavGraphBuilder.coffeeScreenSection(onShowSnackBar: suspend (String, String?) -> Unit) {
    navigation<CoffeeScreenBaseRoute>(startDestination = CoffeeScreenRoute) {
        composable<CoffeeScreenRoute> {
            CoffeeScreenRoute(onShowSnackBar)
        }
    }
}