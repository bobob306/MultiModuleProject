package com.bsdevs.babycare.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.navDeepLink
import com.bsdevs.babycare.presentation.babyhome.BabyCareHomeScreenRoute
import com.bsdevs.babycare.presentation.babyactivities.FeedingScreenRoute
import com.bsdevs.babycare.presentation.babyactivities.NappyChangeScreenRoute
import kotlinx.serialization.Serializable

@Serializable
data object BabyCareBaseRoute

@Serializable
data object BabyCareHomeRoute

@Serializable
data class NappyChangeRoute(val activityId: String? = null)

@Serializable
data class FeedingRoute(val activityId: String? = null, val startSide: String? = null)

fun NavController.navigateToBabyCareHome(navOptions: NavOptions? = null) =
    navigate(route = BabyCareHomeRoute, navOptions = navOptions)

fun NavController.navigateToNappyChange(activityId: String? = null, navOptions: NavOptions? = null) =
    navigate(route = NappyChangeRoute(activityId), navOptions = navOptions)

fun NavController.navigateToFeeding(activityId: String? = null, navOptions: NavOptions? = null) =
    navigate(route = FeedingRoute(activityId), navOptions = navOptions)

fun NavGraphBuilder.babyCareSection(
    navController: NavController,
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToDeepLink: (String) -> Unit,
) {
    navigation<BabyCareBaseRoute>(startDestination = BabyCareHomeRoute) {
        composable<BabyCareHomeRoute> {
            BabyCareHomeScreenRoute(
                onShowSnackBar = onShowSnackBar,
                onNavigateToDeepLink = onNavigateToDeepLink
            )
        }
        composable<NappyChangeRoute>(
            deepLinks = listOf(
                navDeepLink<NappyChangeRoute>(basePath = "babycare://nappy")
            )
        ) {
            NappyChangeScreenRoute(
                onShowSnackBar = onShowSnackBar,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<FeedingRoute>(
            deepLinks = listOf(
                navDeepLink<FeedingRoute>(basePath = "babycare://feeding")
            )
        ) {
            FeedingScreenRoute(
                onShowSnackBar = onShowSnackBar,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
