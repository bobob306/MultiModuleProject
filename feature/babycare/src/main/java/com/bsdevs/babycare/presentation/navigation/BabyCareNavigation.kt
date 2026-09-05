package com.bsdevs.babycare.presentation.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.toRoute
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bsdevs.babycare.presentation.common.BabyActivity
import com.bsdevs.babycare.presentation.feeding.FeedingScreenRoute
import com.bsdevs.babycare.presentation.common.GenericSduiScreen
import com.bsdevs.babycare.presentation.home.BabyCareTileRowComponent
import com.bsdevs.babycare.presentation.home.ActivityFeedItems
import com.bsdevs.babycare.presentation.home.BabyCareHomeViewModel
import com.bsdevs.common.result.Result
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.babycare.presentation.graph.BabyGraphViewModel
import com.bsdevs.babycare.presentation.vaccination.VaccinationHistoryItems
import com.bsdevs.babycare.presentation.temperature.TemperatureHistoryComponent
import com.bsdevs.babycare.presentation.temperature.TemperatureChartComponent
import com.bsdevs.babycare.presentation.graph.FeedingFrequencyChartComponent
import com.bsdevs.babycare.presentation.graph.FeedingGapChartComponent
import com.bsdevs.babycare.presentation.graph.FeedingInsightComponent
import com.bsdevs.babycare.network.MeasurementDto
import com.bsdevs.babycare.network.VaccinationDto
import com.bsdevs.babycare.presentation.home.BabyCareHomeViewData
import com.bsdevs.babycare.presentation.measurement.GrowthChartComponent
import com.bsdevs.babycare.presentation.measurement.MeasurementHistoryItems
import com.bsdevs.babycare.presentation.measurement.MeasurementViewModel
import com.bsdevs.babycare.presentation.temperature.TemperatureDataViewModel
import com.bsdevs.babycare.presentation.vaccination.VaccinationDataViewModel
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.uicomponents.DeleteConfirmationDialog
import kotlinx.serialization.Serializable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
            val homeViewModel: BabyCareHomeViewModel = hiltViewModel()
            val homeViewState by homeViewModel.viewData.collectAsStateWithLifecycle()
            
            var activityToDelete by remember { mutableStateOf<BabyActivity?>(null) }
            
            if (activityToDelete != null) {
                DeleteConfirmationDialog(
                    onConfirm = {
                        activityToDelete?.let { homeViewModel.deleteActivity(it) }
                        activityToDelete = null
                    },
                    onDismiss = { activityToDelete = null }
                )
            }

            GenericSduiScreen(
                screenId = "baby_home",
                title = "Baby Care",
                onNavigateBack = { navController.popBackStack() },
                onDynamicClick = { destination, _ ->
                    when (destination) {
                        "babycare://nappy" -> navigateToForm("nappyLog", null)
                        "babycare://feeding" -> navController.navigateToFeeding()
                        "babycare://temperature" -> navController.navigateToTemperature()
                        "babycare://measurement" -> navController.navigateToMeasurement()
                        "babycare://vaccination" -> navController.navigateToVaccinationHistory()
                        "babycare://graph" -> navController.navigateToGraph()
                    }
                },
                lazyFeatureContent = { component ->
                    when (component) {
                        is NetworkScreenData.TileRowDataNetwork -> {
                            item(key = "tiles_${component.index}") {
                                val data = (homeViewState as? Result.Success<BabyCareHomeViewData>)?.data
                                BabyCareTileRowComponent(
                                    viewData = data,
                                    tiles = component.tiles,
                                    onDynamicClick = { destination, _ ->
                                        when (destination) {
                                            "babycare://nappy" -> navigateToForm("nappyLog", null)
                                            "babycare://feeding" -> navController.navigateToFeeding()
                                            "babycare://temperature" -> navController.navigateToTemperature()
                                            "babycare://measurement" -> navController.navigateToMeasurement()
                                            "babycare://vaccination" -> navController.navigateToVaccinationHistory()
                                            "babycare://graph" -> navController.navigateToGraph()
                                        }
                                    },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = this@composable
                                )
                            }
                            true
                        }
                        is NetworkScreenData.ActivityFeedDataNetwork -> {
                            (homeViewState as? Result.Success<BabyCareHomeViewData>)?.data?.let { data ->
                                ActivityFeedItems(
                                    viewData = data,
                                    onToggleHeaderCollapse = homeViewModel::toggleHeaderCollapse,
                                    onToggleActivityFilter = homeViewModel::toggleActivityFilter,
                                    onDeleteActivity = { activityToDelete = it },
                                    onToggleVitaminD = homeViewModel::toggleVitaminD,
                                    onLoadMore = homeViewModel::loadMore,
                                    onNavigateToEditNappyChange = { id -> navigateToForm("nappyLog", id) },
                                    onNavigateToEditFeeding = { id -> navController.navigateToFeeding(id) },
                                    onNavigateToEditTemperature = { id -> navigateToForm("temperatureLog", id) },
                                    onNavigateToEditMeasurement = { id -> navigateToForm("measurementLog", id) },
                                    onNavigateToEditVaccination = { id -> navigateToForm("vaccinationLog", id) },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = this@composable
                                )
                            }
                            true
                        }
                        else -> false
                    }
                }
            )
        }
        composable<NappyChangeRoute>(
            deepLinks = listOf(
                navDeepLink<NappyChangeRoute>(basePath = "babycare://nappy")
            )
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<NappyChangeRoute>()
            navigateToForm("nappyLog", route.activityId)
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
            val graphViewModel: BabyGraphViewModel = hiltViewModel()
            val graphUiState by graphViewModel.uiState.collectAsStateWithLifecycle()

            GenericSduiScreen(
                screenId = "analysis_screen",
                title = "Routine Analysis",
                onNavigateBack = { navController.popBackStack() },
                lazyFeatureContent = { item ->
                    when (item) {
                        is NetworkScreenData.FeedingFrequencyChartDataNetwork -> {
                            item { FeedingFrequencyChartComponent(uiState = graphUiState) }
                            true
                        }
                        is NetworkScreenData.FeedingGapChartDataNetwork -> {
                            item { FeedingGapChartComponent(uiState = graphUiState) }
                            true
                        }
                        is NetworkScreenData.FeedingInsightCardDataNetwork -> {
                            item { FeedingInsightComponent(uiState = graphUiState) }
                            true
                        }
                        else -> false
                    }
                }
            )
        }
        composable<TemperatureRoute>(
            deepLinks = listOf(
                navDeepLink<TemperatureRoute>(basePath = "babycare://temperature")
            )
        ) {
            val tempViewModel: TemperatureDataViewModel = hiltViewModel()
            val tempUiState by tempViewModel.uiState.collectAsStateWithLifecycle()

            GenericSduiScreen(
                screenId = "temperature_screen",
                title = "Temperature",
                onNavigateBack = { navController.popBackStack() },
                onAddNew = { navigateToForm("temperatureLog", null) },
                lazyFeatureContent = { item ->
                    when (item) {
                        is NetworkScreenData.TemperatureHistoryDataNetwork -> {
                            item {
                                TemperatureHistoryComponent(
                                    uiData = tempUiState,
                                    onEdit = { id -> navigateToForm("temperatureLog", id) },
                                    onDelete = { id, date -> tempViewModel.deleteTemperature(id, date) }
                                )
                            }
                            true
                        }
                        is NetworkScreenData.TemperatureChartDataNetwork -> {
                            item {
                                TemperatureChartComponent(uiData = tempUiState)
                            }
                            true
                        }
                        else -> false
                    }
                }
            )
        }
        composable<MeasurementRoute>(
            deepLinks = listOf(
                navDeepLink<MeasurementRoute>(basePath = "babycare://measurement")
            )
        ) {
            val measureViewModel: MeasurementViewModel = hiltViewModel()
            val measureUiState by measureViewModel.uiState.collectAsStateWithLifecycle()

            var itemToDelete by remember { mutableStateOf<MeasurementDto?>(null) }

            if (itemToDelete != null) {
                DeleteConfirmationDialog(
                    onConfirm = {
                        itemToDelete?.let { item ->
                            val id = item.id
                            val date = item.date
                            if (id != null && date != null) {
                                measureViewModel.deleteMeasurement(id, date)
                            }
                        }
                        itemToDelete = null
                    },
                    onDismiss = { itemToDelete = null }
                )
            }

            GenericSduiScreen(
                screenId = "measurement_screen",
                title = "Growth tracking",
                onNavigateBack = { navController.popBackStack() },
                onAddNew = { navigateToForm("measurementLog", null) },
                lazyFeatureContent = { component ->
                    when (component) {
                        is NetworkScreenData.GrowthChartDataNetwork -> {
                            item(key = "growth_chart_${component.index}") {
                                GrowthChartComponent(
                                    title = component.title,
                                    dataType = component.dataType,
                                    measurements = measureUiState.allMeasurements,
                                    showWhoOverlay = measureUiState.showWhoOverlay,
                                    birthDate = measureUiState.birthDate,
                                    gender = measureUiState.babyGender
                                )
                            }
                            true
                        }
                        is NetworkScreenData.MeasurementHistoryDataNetwork -> {
                            MeasurementHistoryItems(
                                measurements = measureUiState.allMeasurements,
                                showMedicalOnly = measureUiState.showMedicalOnly,
                                onMedicalOnlyChange = measureViewModel::toggleMedicalOnly,
                                showWhoOverlay = measureUiState.showWhoOverlay,
                                onWhoOverlayChange = measureViewModel::toggleWhoOverlay,
                                gender = measureUiState.babyGender,
                                onEdit = { id -> navigateToForm("measurementLog", id) },
                                onDelete = { itemToDelete = it }
                            )
                            true
                        }
                        else -> false
                    }
                }
            )
        }
        composable<VaccinationHistoryRoute>(
            deepLinks = listOf(
                navDeepLink<VaccinationHistoryRoute>(basePath = "babycare://vaccination")
            )
        ) {
            val vaccViewModel: VaccinationDataViewModel = hiltViewModel()
            val groupedVaccinations by vaccViewModel.groupedVaccinations.collectAsStateWithLifecycle()

            var itemToDelete by remember { mutableStateOf<VaccinationDto?>(null) }

            if (itemToDelete != null) {
                DeleteConfirmationDialog(
                    onConfirm = {
                        itemToDelete?.let { item ->
                            val id = item.id
                            val date = item.date
                            if (id != null && date != null) {
                                vaccViewModel.deleteVaccination(id, date)
                            }
                        }
                        itemToDelete = null
                    },
                    onDismiss = { itemToDelete = null }
                )
            }

            GenericSduiScreen(
                screenId = "vaccination_history",
                title = "Vaccinations",
                onNavigateBack = { navController.popBackStack() },
                onAddNew = { navigateToForm("vaccinationLog", null) },
                lazyFeatureContent = { component ->
                    when (component) {
                        is NetworkScreenData.VaccinationHistoryDataNetwork -> {
                            VaccinationHistoryItems(
                                groupedVaccinations = groupedVaccinations,
                                onEdit = { id -> navigateToForm("vaccinationLog", id) },
                                onDelete = { itemToDelete = it }
                            )
                            true
                        }
                        else -> false
                    }
                }
            )
        }
    }
}
