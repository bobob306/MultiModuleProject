package com.bsdevs.homescreen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.common.result.Result
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.renderer.RenderUI

import com.bsdevs.uicomponents.MMPScaffold

@Composable
fun HomeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToCoffee: () -> Unit,
    onNavigateToBabyCare: () -> Unit,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onLoadData: () -> Unit,
    viewData: List<NetworkScreenData>,
    onClick: (String, String) -> Unit,
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val horizontalPadding = if (isLandscape) 8.dp else 16.dp

    MMPScaffold(
        title = "Welcome to the Hub",
        scrollBehavior = scrollBehavior
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = horizontalPadding, end = horizontalPadding, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

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
                            onClick = onClick,
                            onChipClick = {},
                            onSwitchClick = {},
                        )
                    }
                }
            }
        }
    }
}
