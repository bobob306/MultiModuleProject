package com.bsdevs.homescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
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
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.renderer.RenderUI

@Composable
fun HomeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToCoffee: () -> Unit,
    viewModel: HomeScreenViewModel = hiltViewModel(),
) {
    val viewData = viewModel.viewData.collectAsStateWithLifecycle()
    when (viewData.value) {
        is Result.Success -> {
            HomeScreen(
                onShowSnackBar = onShowSnackBar,
                onLoadData = viewModel::getScreen,
                viewData = (viewData.value as Result.Success<List<NetworkScreenData>>).data,
                onClick = viewModel::click,
                onNavigationClick = {},
                onNavigateToCoffee = onNavigateToCoffee,
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
    onClick: (String, String) -> Unit,
    onNavigationClick: (String) -> Unit,
    onNavigateToCoffee: () -> Unit,
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
//    val onClick: (String, String) -> Unit = { location, destination ->
//        onClick(location, destination)
//        showSnackBar = true
//    }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .background(color = MaterialTheme.colorScheme.surfaceVariant)
            .fillMaxSize()
            .padding(vertical = 24.dp, horizontal = 16.dp),
    ) {
        Button(
            modifier = Modifier.wrapContentSize().padding(12.dp),
            onClick = onNavigateToCoffee,
        ) {
            Text("Navigate to coffee")
        }
        viewData.sortedBy { it.index }.forEach {
            RenderUI(
                item = it,
                context = context,
                onClick = onClick,
                onChipClick = {},
                onSwitchClick = {},
            )
        }
    }
}