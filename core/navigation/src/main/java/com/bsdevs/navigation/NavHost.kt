package com.bsdevs.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.bsdevs.coffeescreen.navigation.CoffeeScreenBaseRoute
import com.bsdevs.coffeescreen.navigation.coffeeScreenSection
import com.bsdevs.coffeescreen.navigation.navigateToCoffeeHome
import com.bsdevs.coffeescreen.navigation.navigateToCoffeeInput
import com.bsdevs.firstscreen.navigation.SplashScreenBaseRoute
import com.bsdevs.firstscreen.navigation.splashScreenSection
import com.bsdevs.homescreen.navigation.homeScreenSection
import com.bsdevs.login.loginScreenSection
import com.bsdevs.login.navigateToLoginScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MMPNavHost(
    onShowSnackBar: suspend (String, String?) -> Unit,
    modifier: Modifier,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = SplashScreenBaseRoute,
    ) {
        homeScreenSection(
            onShowSnackBar,
            navController::navigateToCoffeeHome,
        )
        coffeeScreenSection(
            onShowSnackBar,
            navigateToCoffeeInput = navController::navigateToCoffeeInput,
            navigateToCoffeeHome = navController::navigateToCoffeeHome,
//            navigateToCoffeeDetail = navController::navigateToCoffeeDetail,
        )
        loginScreenSection(
            onShowSnackBar,
            onNavigateToCoffeeHome = navController::navigateToCoffeeHome,
        )
        splashScreenSection(
            onShowSnackBar,
            onNavigateToCoffeeHome = navController::navigateToCoffeeHome,
            onNavigateToSignIn = navController::navigateToLoginScreen,
        )
    }
}