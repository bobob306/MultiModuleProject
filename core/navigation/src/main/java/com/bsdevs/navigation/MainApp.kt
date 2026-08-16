package com.bsdevs.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bsdevs.uicomponents.theme.MultiModuleProjectTheme
import kotlinx.coroutines.launch

@Composable
fun MMPApp() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 🔄 1. Check if the device is currently in Landscape mode
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val shouldShowBottomBar: Boolean = when {
        currentDestination?.route?.contains("LoginScreen") == true -> false
        currentDestination?.route?.contains("RegisterScreen") == true -> false
        currentDestination?.route?.contains("SplashScreen") == true -> false
        else -> true
    }

    MultiModuleProjectTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // 📥 2. Only show the bottom bar if we are in portrait mode
            bottomBar = {
                if (shouldShowBottomBar && !isLandscape) {
                    MMPBottomBar(navController)
                }
            }
        ) { innerPadding ->
            // 🔀 3. Use a Row layout for landscape mode so the rail and content sit side-by-side
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 🛤️ 4. Show a Navigation Rail on the left side during landscape mode
                if (shouldShowBottomBar && isLandscape) {
                    MMPNavigationRail(navController) // You will need to create this composable!
                }

                // 📱 5. Your main app content goes here
                MMPNavHost(
                    navController = navController,
                    onShowSnackBar = { message, action ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = message,
                                actionLabel = action,
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
