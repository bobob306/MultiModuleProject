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
            bottomBar = {
                if (shouldShowBottomBar && !isLandscape) {
                    MMPBottomBar(navController)
                }
            }
        ) { innerPadding ->
            // 🌟 THE FIX: Removed .padding(innerPadding) from this Row container
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                if (shouldShowBottomBar && isLandscape) {
                    MMPNavigationRail(navController)
                }

                // Pass the root innerPadding directly into your NavHost
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
                    rootPadding = innerPadding // 🌟 Pass this down to handle system bars/bottom bars safely
                )
            }
        }
    }
}

