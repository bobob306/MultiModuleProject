package com.bsdevs.coffeescreen.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.bsdevs.coffeescreen.screens.detailscreen.CoffeeDetailScreenRoute
import com.bsdevs.coffeescreen.screens.homescreen.CoffeeHomeScreenRoute
import kotlinx.serialization.Serializable

@Serializable
data object CoffeeHomeScreenRoute

@Serializable
data class CoffeeDetailScreenRoute(val coffeeId: String)

@Serializable
data object CoffeeScreenBaseRoute

fun NavController.navigateToCoffeeHome(navOptions: NavOptions? = null) =
    navigate(route = CoffeeHomeScreenRoute, navOptions = navOptions)

fun NavController.navigateToCoffeeDetail(coffeeId: String, navOptions: NavOptions? = null) =
    navigate(route = CoffeeDetailScreenRoute(coffeeId), navOptions = navOptions)

//fun NavController.navigateToCoffeeDetail(coffeeDetail: CoffeeDto, navOptions: NavOptions? = null) =
//    navigate(
//        route = CoffeeDetailScreenRoute(coffeeDetail),
//        navOptions = navOptions,
//    )

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.coffeeScreenSection(
    onShowSnackBar: suspend (String, String?) -> Unit,
    navigateToForm: (String) -> Unit,
    navigateToCoffeeHome: (navOptions: NavOptions?) -> Unit,
    navigateToLogin: (navOptions: NavOptions?) -> Unit,
    navigateToCoffeeDetail: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
) {
    navigation<CoffeeScreenBaseRoute>(startDestination = CoffeeHomeScreenRoute) {
        composable<CoffeeHomeScreenRoute>(
            deepLinks = listOf(navDeepLink { uriPattern = "app://com.bsdevs.multimoduleproject/coffeehome" })
        ) {
            CoffeeHomeScreenRoute(
                onShowSnackBar,
                navigateToForm = navigateToForm,
                navigateToLogin = navigateToLogin,
                onNavigateToDetail = navigateToCoffeeDetail,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable
            )
        }
        composable<CoffeeDetailScreenRoute> { backStackEntry ->
            CoffeeDetailScreenRoute(
                onShowSnackBar, 
                navigateToCoffeeHome = navigateToCoffeeHome,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable
            )
        }
    }
}
