package com.bsdevs.coffeescreen.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.bsdevs.coffeescreen.navigation.CoffeeDetailScreenRoute
import com.bsdevs.coffeescreen.screens.detailscreen.CoffeeDetailScreenRoute
import com.bsdevs.coffeescreen.screens.homescreen.CoffeeHomeScreenRoute
import com.bsdevs.coffeescreen.screens.inputscreen.CoffeeInputScreenRoute
import kotlinx.serialization.Serializable

@Serializable
data object CoffeeInputScreenRoute

@Serializable
data object CoffeeHomeScreenRoute

@Serializable
data object CoffeeDetailScreenRoute

@Serializable
data object CoffeeScreenBaseRoute

fun NavController.navigateToCoffeeInput(navOptions: NavOptions? = null) =
    navigate(route = CoffeeInputScreenRoute, navOptions = navOptions)

fun NavController.navigateToCoffeeHome(navOptions: NavOptions? = null) =
    navigate(route = CoffeeHomeScreenRoute, navOptions = navOptions)

@RequiresApi(Build.VERSION_CODES.O)
fun NavGraphBuilder.coffeeScreenSection(
    onShowSnackBar: suspend (String, String?) -> Unit,
    navigateToCoffeeInput: () -> Unit,
    navigateToCoffeeHome: (navOptions: NavOptions?) -> Unit
) {
    navigation<CoffeeScreenBaseRoute>(startDestination = CoffeeHomeScreenRoute) {
        composable<CoffeeInputScreenRoute> {
            CoffeeInputScreenRoute(onShowSnackBar, navigateToCoffeeHome = navigateToCoffeeHome)
        }
        composable<CoffeeHomeScreenRoute> {
            CoffeeHomeScreenRoute(onShowSnackBar, navigateToCoffeeInput = navigateToCoffeeInput)
        }
        composable<CoffeeDetailScreenRoute> {
            CoffeeDetailScreenRoute(onShowSnackBar, navigateToCoffeeHome = navigateToCoffeeHome)
        }
    }
}