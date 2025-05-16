package com.bsdevs.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.bsdevs.coffeescreen.navigation.CoffeeScreenBaseRoute
import com.bsdevs.coffeescreen.navigation.CoffeeScreenRoute
import com.bsdevs.coffeescreen.navigation.coffeeScreenSection
import com.bsdevs.coffeescreen.navigation.navigateToCoffee
import com.bsdevs.homescreen.navigation.HomeScreenBaseRoute
import com.bsdevs.homescreen.navigation.homeScreenSection

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MMPNavHost(
    onShowSnackBar: suspend (String, String?) -> Unit,
    modifier: Modifier,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = CoffeeScreenBaseRoute,
    ) {
        homeScreenSection(
            onShowSnackBar,
            navController::navigateToCoffee
        )
        coffeeScreenSection(
            onShowSnackBar
        )
    }
}