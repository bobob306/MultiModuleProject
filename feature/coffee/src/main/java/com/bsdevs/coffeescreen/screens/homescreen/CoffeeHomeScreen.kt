package com.bsdevs.coffeescreen.screens.homescreen

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import androidx.window.core.layout.WindowSizeClass
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.homescreen.viewdata.CoffeeHomeScreenViewData
import com.bsdevs.coffeescreen.screens.homescreen.viewdata.CoffeeHomeScreenViewDatas
import com.bsdevs.coffeescreen.screens.inputscreen.ErrorScreen
import com.bsdevs.coffeescreen.screens.inputscreen.NavigationEvent
import com.bsdevs.common.result.Result
import com.bsdevs.uicomponents.shimmer
import androidx.compose.ui.draw.clip
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi

import com.bsdevs.uicomponents.MMPScaffold

@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun CoffeeHomeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    navigateToCoffeeInput: () -> Unit,
    navigateToLogin: (navOptions: NavOptions?) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: CoffeeHomeScreenViewModel = hiltViewModel(),
) {
    val viewData = viewModel.viewData.collectAsStateWithLifecycle()
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (viewData.value) {
            is Result.Loading -> CoffeeHomeLoadingScreen()
            is Result.Error -> ErrorScreen()
            is Result.Success -> CoffeeHomeScreenContent(
                viewData = (viewData.value as Result.Success<CoffeeHomeScreenViewData>).data,
                onIntent = viewModel::processIntent,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
    LaunchedEffect(key1 = Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                NavigationEvent.NavigateToInput -> navigateToCoffeeInput()
                NavigationEvent.NavigateToHome -> {}
                NavigationEvent.NavigateToLogin -> {
                    onShowSnackBar.invoke("Logged out", "")
                    navigateToLogin.invoke(null)
                }

                is NavigationEvent.NavigateToDetail -> onNavigateToDetail(event.coffeeId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CoffeeHomeScreenContent(
    viewData: CoffeeHomeScreenViewData,
    onIntent: (CoffeeHomeScreenIntent) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val coffeeListItems = viewData.viewData
        .filterIsInstance<CoffeeHomeScreenViewDatas.CoffeeList>()
        .firstOrNull()?.coffeeList
    val configuration = LocalConfiguration.current
    @Suppress("DEPRECATION")
    val window = currentWindowAdaptiveInfo()
    @Suppress("DEPRECATION")
    val isLandscape =
        (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                || window.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val horizontalPadding = if (isLandscape) 8.dp else 16.dp

    MMPScaffold(
        title = "Coffee Home Screen",
        scrollBehavior = scrollBehavior
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = horizontalPadding, end = horizontalPadding, bottom = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column {
                CoffeeHomeButtons(
                    onIntent = onIntent,
                    isLandscape = isLandscape,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
                LazyVerticalGrid(
                    columns = if (isLandscape) GridCells.Fixed(2) else GridCells.Fixed(1),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    coffeeListItems?.let { list ->
                        items(
                            count = list.size,
                            key = { index ->
                                list[index].id ?: index
                            } // Provide a stable key
                        ) { index ->
                            CoffeeListItem(
                                coffee = list[index],
                                onIntent = onIntent,
                                isLandscape = isLandscape,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CoffeeHomeButtons(
    onIntent: (CoffeeHomeScreenIntent) -> Unit,
    isLandscape: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    if (isLandscape) {
        Row {
            with(sharedTransitionScope) {
                Button(
                    onClick = { onIntent.invoke(CoffeeHomeScreenIntent.NavigateToInput) },
                    modifier = Modifier
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "coffee_input_box"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .wrapContentSize()
                        .padding(
                            end = 16.dp
                        )
                ) { Text("Click to navigate to coffee input") }
            }
            Button(
                onClick = { onIntent.invoke(CoffeeHomeScreenIntent.Logout) },
                modifier = Modifier.wrapContentSize()
            ) { Text("Logout") }
        }
    } else Column {
        with(sharedTransitionScope) {
            Button(
                onClick = { onIntent.invoke(CoffeeHomeScreenIntent.NavigateToInput) },
                modifier = Modifier
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "coffee_input_box"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    .wrapContentSize()
                    .padding(bottom = 8.dp)
            ) { Text("Click to navigate to coffee input") }
        }
        Button(
            onClick = { onIntent.invoke(CoffeeHomeScreenIntent.Logout) },
            modifier = Modifier
                .wrapContentSize()
                .padding(bottom = 16.dp)
        ) { Text("Logout") }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CoffeeListItem(
    coffee: CoffeeDto,
    onIntent: (CoffeeHomeScreenIntent) -> Unit,
    isLandscape: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    with(sharedTransitionScope) {
        Card(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = if (isLandscape) 8.dp else 0.dp)
                .sharedElement(
                    sharedContentState = rememberSharedContentState(key = "coffee_card_${coffee.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .clickable {
                    onIntent.invoke(
                        CoffeeHomeScreenIntent.NavigateToDetail(
                            coffee.id ?: ""
                        )
                    )
                }
                .wrapContentHeight()
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        )
        {
            Text(
                text = coffee.label ?: "Unnamed Coffee",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(4.dp)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "coffee_label_${coffee.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CoffeeHomeLoadingScreen() {
    MMPScaffold(
        title = "Coffee Home Screen"
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Buttons Shimmer
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.width(200.dp).height(48.dp).clip(MaterialTheme.shapes.small).shimmer())
                Box(modifier = Modifier.width(100.dp).height(48.dp).clip(MaterialTheme.shapes.small).shimmer())
            }

            // Grid Shimmer
            repeat(4) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .shimmer()
                )
            }
        }
    }
}
