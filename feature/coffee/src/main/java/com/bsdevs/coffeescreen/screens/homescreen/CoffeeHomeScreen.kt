package com.bsdevs.coffeescreen.screens.homescreen

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import androidx.window.core.layout.WindowWidthSizeClass
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.homescreen.viewdata.CoffeeHomeScreenViewData
import com.bsdevs.coffeescreen.screens.homescreen.viewdata.CoffeeHomeScreenViewDatas
import com.bsdevs.coffeescreen.screens.inputscreen.ErrorScreen
import com.bsdevs.coffeescreen.screens.inputscreen.LoadingScreen
import com.bsdevs.coffeescreen.screens.inputscreen.NavigationEvent
import com.bsdevs.common.result.Result
import kotlinx.coroutines.launch

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun CoffeeHomeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    navigateToCoffeeInput: () -> Unit,
    navigateToLogin: (navOptions: NavOptions?) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: CoffeeHomeScreenViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
//    scope.launch { viewModel.start() }
    val viewData = viewModel.viewData.collectAsStateWithLifecycle()
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (viewData.value) {
            is Result.Loading -> LoadingScreen()
            is Result.Error -> ErrorScreen()
            is Result.Success -> CoffeeHomeScreenContent(
                onShowSnackBar = onShowSnackBar,
                viewData = (viewData.value as Result.Success<CoffeeHomeScreenViewData>).data,
                onIntent = viewModel::processIntent,
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

@Composable
fun CoffeeHomeScreenContent(
    onShowSnackBar: suspend (String, String?) -> Unit,
    viewData: CoffeeHomeScreenViewData,
    onIntent: (CoffeeHomeScreenIntent) -> Unit,
) {
    val coffeeListItems = viewData.viewData
        .filterIsInstance<CoffeeHomeScreenViewDatas.CoffeeList>()
        .firstOrNull()?.coffeeList
    val configuration = LocalConfiguration.current
    val window = currentWindowAdaptiveInfo()
    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                || window.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column {
            Text("Coffee Home Screen")
            CoffeeHomeButtons(onIntent, isLandscape)
            LazyVerticalGrid(
                columns = if (isLandscape) GridCells.Fixed(2) else GridCells.Fixed(1),
                modifier = Modifier.fillMaxSize(),
            ) {
                coffeeListItems?.let { list ->
                    items(
                        count = list.size,
                        key = { index -> list[index].id ?: index } // Provide a stable key
                    ) { index ->
                        CoffeeListItem(
                            coffee = list[index],
                            onIntent = onIntent,
                            isLandscape = isLandscape,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoffeeHomeButtons(onIntent: (CoffeeHomeScreenIntent) -> Unit, isLandscape: Boolean) {
    if (isLandscape) {
        Row {
            Button(
                onClick = { onIntent.invoke(CoffeeHomeScreenIntent.NavigateToInput) },
                modifier = Modifier
                    .wrapContentSize()
                    .padding(
                        end = 16.dp
                    )
            ) { Text("Click to navigate to coffee input") }
            Button(
                onClick = { onIntent.invoke(CoffeeHomeScreenIntent.Logout) },
                modifier = Modifier.wrapContentSize()
            ) { Text("Logout") }
        }
    } else Column {
        Button(
            onClick = { onIntent.invoke(CoffeeHomeScreenIntent.NavigateToInput) },
            modifier = Modifier
                .wrapContentSize()
                .padding(bottom = 8.dp)
        ) { Text("Click to navigate to coffee input") }
        Button(
            onClick = { onIntent.invoke(CoffeeHomeScreenIntent.Logout) },
            modifier = Modifier
                .wrapContentSize()
                .padding(bottom = 16.dp)
        ) { Text("Logout") }
    }
}

@Composable
fun CoffeeListItem(coffee: CoffeeDto, onIntent: (CoffeeHomeScreenIntent) -> Unit, isLandscape: Boolean) {
    Card(
        modifier = Modifier
            .clickable {
                onIntent.invoke(CoffeeHomeScreenIntent.NavigateToDetail(coffee.id ?: ""))
            }
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = if (isLandscape) 8.dp else 0.dp ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    )
    {
        Text(
            text = coffee.label ?: "Unnamed Coffee",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(4.dp)
        )
    }
}