package com.bsdevs.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.bsdevs.homescreen.navigation.HomeScreenBaseRoute
import com.bsdevs.homescreen.navigation.homeScreenSection

@Composable
fun MMPNavHost(
    onShowSnackBar: suspend (String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HomeScreenBaseRoute,
        modifier = modifier,
    ) {
        homeScreenSection()
    }
}