package com.bsdevs.babycare.presentation.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.bsdevs.babycare.presentation.feeding.FeedingScreenRoute
import com.bsdevs.babycare.presentation.graph.BabyGraphRoute
import com.bsdevs.babycare.presentation.home.BabyCareHomeScreenRoute
import com.bsdevs.babycare.presentation.measurement.MeasurementScreenRoute
import com.bsdevs.babycare.presentation.nappy.NappyChangeScreenRoute
import com.bsdevs.babycare.presentation.temperature.TemperatureScreenRoute
import com.bsdevs.babycare.presentation.vaccination.VaccinationHistoryScreenRoute
import kotlinx.serialization.Serializable

@Serializable
data object BabyCareBaseRoute

@Serializable
data object BabyCareHomeRoute

@Serializable
data object BabyGraphRoute

@Serializable
data object VaccinationHistoryRoute

@Serializable
data class TemperatureRoute(val activityId: String? = null)

@Serializable
data class MeasurementRoute(val activityId: String? = null)

@Serializable
data class NappyChangeRoute(val activityId: String? = null)

@Serializable
data class FeedingRoute(val activityId: String? = null, val startSide: String? = null)

fun NavController.navigateToBabyCareHome(navOptions: NavOptions? = null) =
    navigate(route = BabyCareHomeRoute, navOptions = navOptions)

fun NavController.navigateToGraph(navOptions: NavOptions? = null) =
    navigate(route = BabyGraphRoute, navOptions = navOptions)

fun NavController.navigateToVaccinationHistory(navOptions: NavOptions? = null) =
    navigate(route = VaccinationHistoryRoute, navOptions = navOptions)

fun NavController.navigateToNappyChange(
    activityId: String? = null,
    navOptions: NavOptions? = null
) =
    navigate(route = NappyChangeRoute(activityId), navOptions = navOptions)

fun NavController.navigateToFeeding(activityId: String? = null, navOptions: NavOptions? = null) =
    navigate(route = FeedingRoute(activityId), navOptions = navOptions)

fun NavController.navigateToTemperature(
    activityId: String? = null,
    navOptions: NavOptions? = null
) =
    navigate(route = TemperatureRoute(activityId), navOptions = navOptions)

fun NavController.navigateToMeasurement(
    activityId: String? = null,
    navOptions: NavOptions? = null
) =
    navigate(route = MeasurementRoute(activityId), navOptions = navOptions)

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.babyCareSection(
    navController: NavController,
    onShowSnackBar: suspend (String, String?) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    navigateToForm: (formId: String, entityId: String?) -> Unit,
) {
    navigation<BabyCareBaseRoute>(startDestination = BabyCareHomeRoute) {
        composable<BabyCareHomeRoute> {
            BabyCareHomeScreenRoute(
                onNavigateToNappyChange = { navigateToForm("nappyLog", null) },
                onNavigateToFeeding = { navController.navigateToFeeding() },
                onNavigateToTemperature = { navController.navigateToTemperature() },
                onNavigateToMeasurement = { navController.navigateToMeasurement() },
                onNavigateToVaccination = { navController.navigateToVaccinationHistory() },
                onNavigateToEditNappyChange = { id -> navigateToForm("nappyLog", id) },
                onNavigateToEditFeeding = { id -> navController.navigateToFeeding(id) },
                onNavigateToEditTemperature = { id -> navigateToForm("temperatureLog", id) },
                onNavigateToEditMeasurement = { id -> navigateToForm("measurementLog", id) },
                onNavigateToEditVaccination = { id -> navigateToForm("vaccinationLog", id) },
                onNavigateToGraph = { navController.navigateToGraph() },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable
            )
        }
        composable<NappyChangeRoute>(
            deepLinks = listOf(
                navDeepLink<NappyChangeRoute>(basePath = "babycare://nappy")
            )
        ) {
            NappyChangeScreenRoute(
                onShowSnackBar = onShowSnackBar,
                onNavigateBack = { navController.popBackStack() },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable
            )
        }
        composable<FeedingRoute>(
            deepLinks = listOf(
                navDeepLink<FeedingRoute>(basePath = "babycare://feeding")
            )
        ) {
            FeedingScreenRoute(
                onShowSnackBar = onShowSnackBar,
                onNavigateBack = { navController.popBackStack() },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable
            )
        }
        composable<BabyGraphRoute>(
            deepLinks = listOf(
                navDeepLink<BabyGraphRoute>(basePath = "babycare://graph")
            )
        ) {
            BabyGraphRoute(
                onNavigateBack = { navController.popBackStack() },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable
            )
        }
        composable<TemperatureRoute>(
            deepLinks = listOf(
                navDeepLink<TemperatureRoute>(basePath = "babycare://temperature")
            )
        ) {
            TemperatureScreenRoute(
                onShowSnackBar = onShowSnackBar,
                onNavigateBack = { navController.popBackStack() },
                onAddNew = { navigateToForm("temperatureLog", null) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable
            )
        }
        composable<MeasurementRoute>(
            deepLinks = listOf(
                navDeepLink<MeasurementRoute>(basePath = "babycare://measurement")
            )
        ) {
            MeasurementScreenRoute(
                onShowSnackBar = onShowSnackBar,
                onNavigateBack = { navController.popBackStack() },
                onAddNew = { navigateToForm("measurementLog", null) },
                onEditItem = { id -> navigateToForm("measurementLog", id) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable
            )
        }
        composable<VaccinationHistoryRoute>(
            deepLinks = listOf(
                navDeepLink<VaccinationHistoryRoute>(basePath = "babycare://vaccination")
            )
        ) {
            VaccinationHistoryScreenRoute(
                onNavigateBack = { navController.popBackStack() },
                onAddNew = { navigateToForm("vaccinationLog", null) },
                onEditItem = { id -> navigateToForm("vaccinationLog", id) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this@composable
            )
        }
    }
}
