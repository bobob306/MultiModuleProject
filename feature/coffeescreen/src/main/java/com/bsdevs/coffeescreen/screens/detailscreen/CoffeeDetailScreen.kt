package com.bsdevs.coffeescreen.screens.detailscreen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.bsdevs.coffeescreen.navigation.CoffeeDetailScreenRoute
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.inputscreen.ErrorScreen
import com.bsdevs.coffeescreen.screens.inputscreen.LoadingScreen
import com.bsdevs.common.result.Result

@Composable
fun CoffeeDetailScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    navigateToCoffeeHome: (navOptions: NavOptions?) -> Unit,
    viewModel: CoffeeDetailsViewModel = hiltViewModel()
) {
    val viewData = viewModel.viewData.collectAsStateWithLifecycle()
    Surface(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (viewData.value) {
            is Result.Loading -> {
                LoadingScreen()
            }
            is Result.Error -> {
                ErrorScreen()
            }
            is Result.Success<CoffeeDto> -> {
                Text((viewData.value as Result.Success<CoffeeDto>).data.label ?: "egg")
            }
        }
    }
}