package com.bsdevs.homescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.common.result.Result
import com.bsdevs.data.LocationTypeData
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.renderer.RenderUI

@Composable
fun HomeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToCoffee: () -> Unit,
    onNavigateToBabyCare: () -> Unit,
    onNavigateToDeepLink: (String) -> Unit,
    viewModel: HomeScreenViewModel = hiltViewModel(),
) {
    val viewData = viewModel.viewData.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is HomeNavigationEvent.NavigateToDeepLink -> {
                    // 🚀 Fire the callback up to the NavHost to handle the actual navigation
                    onNavigateToDeepLink(event.uriString)
                }
            }
        }
    }

    when (viewData.value) {
        is Result.Success -> {
            HomeScreen(
                onShowSnackBar = onShowSnackBar,
                onLoadData = viewModel::getScreen,
                viewData = (viewData.value as Result.Success<List<NetworkScreenData>>).data,
                onClick = viewModel::handleServerButtonClick,
                onNavigationClick = {},
                onNavigateToCoffee = onNavigateToCoffee,
                onNavigateToBabyCare = onNavigateToBabyCare,
            )
        }

        is Result.Error -> ErrorScreen()
        is Result.Loading -> LoadingScreen()
    }
}

@Composable
internal fun ErrorScreen() {
    Text("Error")
}

@Composable
internal fun LoadingScreen() {
    Text("Loading")
}

@Composable
internal fun HomeScreen(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onLoadData: () -> Unit,
    viewData: List<NetworkScreenData>,
    onClick: (String, LocationTypeData, String) -> Unit,
    onNavigationClick: (String) -> Unit,
    onNavigateToCoffee: () -> Unit,
    onNavigateToBabyCare: () -> Unit,
) {
    val context = LocalContext.current
    var showSnackBar by remember { mutableStateOf(false) }
    LaunchedEffect(
        key1 = showSnackBar, block = {
            if (showSnackBar) {
                onShowSnackBar.invoke("String", null)
            }
        }
    )
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .background(color = MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(vertical = 24.dp, horizontal = 16.dp),
    ) {
        Text(
            text = "Welcome to the Hub",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onNavigateToCoffee,
            ) {
                Text("Coffee Section")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                modifier = Modifier.weight(1f),
                onClick = onNavigateToBabyCare,
            ) {
                Text("Baby Care Section")
            }
        }

        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )

        viewData.sortedBy { it.index }.forEach {
            RenderUI(
                item = it,
                context = context,
                onNavigationClick = onClick,
                onChipClick = {},
                onSwitchClick = {},
                onClick = {_, _ -> }
            )
        }
    }
}
