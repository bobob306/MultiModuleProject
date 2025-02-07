package com.bsdevs.homescreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.common.result.Result
import com.bsdevs.network.ScreenDto
import com.bsdevs.network.SpacerType

@Composable
fun HomeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    viewModel: HomeScreenViewModel = hiltViewModel(),
) {
    val viewData = viewModel.viewData.collectAsStateWithLifecycle()
    when (viewData.value) {
        is Result.Success -> {
            HomeScreen(
                onShowSnackBar = onShowSnackBar,
                onLoadData = viewModel::getScreen,
                viewData = (viewData.value as Result.Success<List<ScreenDto>>).data
            )
        }

        is Result.Error -> ErrorScreen()
        is Result.Loading -> LoadingScreen(viewModel::getScreen)
    }
}

@Composable
internal fun ErrorScreen() {
    Text("Error")
}

@Composable
internal fun LoadingScreen(getScreen: () -> Unit) {
    getScreen()
    Text("Loading")
}

@Composable
internal fun HomeScreen(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onLoadData: () -> Unit,
    viewData: List<ScreenDto>,
) {
    var showSnackBar by remember { mutableStateOf(false) }
    LaunchedEffect(
        key1 = showSnackBar, block = {
            if (showSnackBar) {
                onShowSnackBar.invoke("String", null)
            }
        }
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        viewData.sortedBy { it.index }.forEach {
            when (it) {
                is ScreenDto.TitleDto -> Text(it.content)
                is ScreenDto.SubtitleDto -> Text(it.content)
                is ScreenDto.SpacerDto -> {
                    if (it.size.type == SpacerType.WEIGHT) {
                        Spacer(modifier = Modifier.weight(it.size.weight!!))
                    } else {
                        Spacer(modifier = Modifier.size(it.size.size!!.dp))
                    }
                }

                is ScreenDto.Unknown -> Text("Unknown")
            }
        }
        Button(
            onClick = {
                onLoadData()
                showSnackBar = !showSnackBar
            }
        ) {
            Text("Button $showSnackBar")
        }
    }

}