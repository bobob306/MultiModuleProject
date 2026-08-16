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
data class NappyChangeRoute(val activityId: String? = null)

@Serializable
data class FeedingRoute(val activityId: String? = null)

fun NavController.navigateToBabyCareHome(navOptions: NavOptions? = null) =
    navigate(route = BabyCareHomeRoute, navOptions = navOptions)

fun NavController.navigateToNappyChange(activityId: String? = null, navOptions: NavOptions? = null) =
    navigate(route = NappyChangeRoute(activityId), navOptions = navOptions)

fun NavController.navigateToFeeding(activityId: String? = null, navOptions: NavOptions? = null) =
    navigate(route = FeedingRoute(activityId), navOptions = navOptions)

fun NavGraphBuilder.babyCareSection(
    navController: NavController,
    onShowSnackBar: suspend (String, String?) -> Unit,
) {
    navigation<BabyCareBaseRoute>(startDestination = BabyCareHomeRoute) {
        composable<BabyCareHomeRoute> {
            BabyCareHomeScreenRoute(
                onShowSnackBar = onShowSnackBar,
                onNavigateToNappyChange = { navController.navigateToNappyChange() },
                onNavigateToFeeding = { navController.navigateToFeeding() },
                onNavigateToEditNappyChange = { id -> navController.navigateToNappyChange(id) },
                onNavigateToEditFeeding = { id -> navController.navigateToFeeding(id) }
            )
        }
        composable<NappyChangeRoute> {
            NappyChangeScreenRoute(
                onShowSnackBar = onShowSnackBar,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<FeedingRoute> {
            FeedingScreenRoute(
                onShowSnackBar = onShowSnackBar,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
