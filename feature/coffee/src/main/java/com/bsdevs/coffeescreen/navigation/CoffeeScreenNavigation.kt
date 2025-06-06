package com.bsdevs.coffeescreen.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.detailscreen.CoffeeDetailScreenRoute
import com.bsdevs.coffeescreen.screens.homescreen.CoffeeHomeScreenRoute
import com.bsdevs.coffeescreen.screens.inputscreen.CoffeeInputScreenRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
data object CoffeeInputScreenRoute

@Serializable
data object CoffeeHomeScreenRoute

@Serializable
data class CoffeeDetailScreenRoute(val coffeeId: String)

@Serializable
data object CoffeeScreenBaseRoute

fun NavController.navigateToCoffeeInput(navOptions: NavOptions? = null) =
    navigate(route = CoffeeInputScreenRoute, navOptions = navOptions)

fun NavController.navigateToCoffeeHome(navOptions: NavOptions? = null) =
    navigate(route = CoffeeHomeScreenRoute, navOptions = navOptions)

fun NavController.navigateToCoffeeDetail(coffeeId: String, navOptions: NavOptions? = null) =
    navigate(route = CoffeeDetailScreenRoute(coffeeId), navOptions = navOptions)

//fun NavController.navigateToCoffeeDetail(coffeeDetail: CoffeeDto, navOptions: NavOptions? = null) =
//    navigate(
//        route = CoffeeDetailScreenRoute(coffeeDetail),
//        navOptions = navOptions,
//    )

@RequiresApi(Build.VERSION_CODES.O)
fun NavGraphBuilder.coffeeScreenSection(
    onShowSnackBar: suspend (String, String?) -> Unit,
    navigateToCoffeeInput: () -> Unit,
    navigateToCoffeeHome: (navOptions: NavOptions?) -> Unit,
    navigateToLogin: (navOptions: NavOptions?) -> Unit,
    navigateToCoffeeDetail: (String) -> Unit,
) {
    navigation<CoffeeScreenBaseRoute>(startDestination = CoffeeHomeScreenRoute) {
        composable<CoffeeInputScreenRoute> {
            CoffeeInputScreenRoute(onShowSnackBar, navigateToCoffeeHome = navigateToCoffeeHome)
        }
        composable<CoffeeHomeScreenRoute>(
            deepLinks = listOf(navDeepLink { uriPattern = "app://com.bsdevs.multimoduleproject/coffeehome" })
        ) {
            CoffeeHomeScreenRoute(
                onShowSnackBar,
                navigateToCoffeeInput = navigateToCoffeeInput,
                navigateToLogin = navigateToLogin,
                onNavigateToDetail = navigateToCoffeeDetail,
            )
        }
        composable<CoffeeDetailScreenRoute> { backStackEntry ->
            CoffeeDetailScreenRoute(onShowSnackBar, navigateToCoffeeHome = navigateToCoffeeHome)
        }
    }
}