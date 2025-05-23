package com.bsdevs.coffeescreen.screens.homescreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.homescreen.viewdata.CoffeeHomeScreenViewData
import com.bsdevs.coffeescreen.screens.homescreen.viewdata.CoffeeHomeScreenViewDatas
import com.bsdevs.coffeescreen.screens.inputscreen.ErrorScreen
import com.bsdevs.coffeescreen.screens.inputscreen.LoadingScreen
import com.bsdevs.coffeescreen.screens.inputscreen.NavigationEvent
import com.bsdevs.common.result.Result

@Composable
fun CoffeeHomeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    navigateToCoffeeInput: () -> Unit,
    onNavigateToDetail: (CoffeeDto) -> Unit,
    viewModel: CoffeeHomeScreenViewModel = hiltViewModel(),
) {
    val viewData = viewModel.viewData.collectAsStateWithLifecycle()
    Surface(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
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
                is NavigationEvent.NavigateToDetail -> onNavigateToDetail(event.coffee)
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
    LazyColumn {
        item {
            Text("Coffee Home Screen")
            Button(
                onClick = { onIntent.invoke(CoffeeHomeScreenIntent.NavigateToInput) },
                modifier = Modifier.wrapContentSize()
            ) { Text("Click to navigate to coffee input") }
            Spacer(Modifier.height(16.dp))
        }
        val coffeeListItems: CoffeeHomeScreenViewDatas.CoffeeList =
            viewData.viewData.first { it is CoffeeHomeScreenViewDatas.CoffeeList }
                    as CoffeeHomeScreenViewDatas.CoffeeList
        items(count = coffeeListItems.coffeeList.size) { index ->
            CoffeeListItem(coffee = coffeeListItems.coffeeList[index], onIntent = onIntent)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun CoffeeListItem(coffee: CoffeeDto, onIntent: (CoffeeHomeScreenIntent) -> Unit) {
    Card(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .clickable {
                // Handle item click
                // Navigate to the Coffee Detail Screen
            }
    )
    {
        Text(
            "Coffee: ${coffee.label}",
            modifier = Modifier
                .padding(8.dp)
                .clickable {
                    onIntent.invoke(CoffeeHomeScreenIntent.NavigateToDetail(coffee))
                }
        )
    }
}