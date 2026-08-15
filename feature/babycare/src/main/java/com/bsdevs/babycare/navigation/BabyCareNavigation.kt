package com.bsdevs.babycare.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.bsdevs.babycare.BabyCareHomeScreenRoute
import com.bsdevs.babycare.FeedingScreenRoute
import com.bsdevs.babycare.NappyChangeScreenRoute
import kotlinx.serialization.Serializable

@Serializable
data object BabyCareBaseRoute

@Serializable
data object BabyCareHomeRoute

@Serializable
data object NappyChangeRoute

@Serializable
data object FeedingRoute

fun NavController.navigateToBabyCareHome(navOptions: NavOptions? = null) =
    navigate(route = BabyCareHomeRoute, navOptions = navOptions)

fun NavController.navigateToNappyChange(navOptions: NavOptions? = null) =
    navigate(route = NappyChangeRoute, navOptions = navOptions)

fun NavController.navigateToFeeding(navOptions: NavOptions? = null) =
    navigate(route = FeedingRoute, navOptions = navOptions)

fun NavGraphBuilder.babyCareSection(
    onShowSnackBar: suspend (String, String?) -> Unit,
) {
    navigation<BabyCareBaseRoute>(startDestination = BabyCareHomeRoute) {
        composable<BabyCareHomeRoute> {
            BabyCareHomeScreenRoute(onShowSnackBar)
        }
        composable<NappyChangeRoute> {
            NappyChangeScreenRoute(onShowSnackBar)
        }
        composable<FeedingRoute> {
            FeedingScreenRoute(onShowSnackBar)
        }
    }
}
