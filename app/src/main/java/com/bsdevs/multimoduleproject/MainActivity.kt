package com.bsdevs.multimoduleproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bsdevs.babycare.navigation.BabyCareBaseRoute
import com.bsdevs.babycare.navigation.BabyCareHomeRoute
import com.bsdevs.coffeescreen.navigation.CoffeeHomeScreenRoute
import com.bsdevs.coffeescreen.navigation.CoffeeScreenBaseRoute
import com.bsdevs.firstscreen.navigation.SplashScreenBaseRoute
import com.bsdevs.homescreen.navigation.HomeScreenBaseRoute
import com.bsdevs.homescreen.navigation.HomeScreenRoute
import com.bsdevs.navigation.MMPNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val otherPadding = WindowInsets.systemBars.asPaddingValues()

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val shouldShowBottomBar = currentDestination?.hierarchy?.any {
                it.hasRoute(SplashScreenBaseRoute::class)
            } == false

            Scaffold(
                modifier = Modifier.fillMaxSize().padding(otherPadding),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (shouldShowBottomBar) {
                        MMPBottomBar(navController)
                    }
                }
            ) { innerPadding ->
                MMPNavHost(
                    navController = navController,
                    onShowSnackBar = { message, action ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = message,
                                actionLabel = action,
                                duration = Short
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )
            }
        }
    }
}

sealed class BottomNavItem(
    val route: Any,
    val baseRoute: KClass<*>,
    val icon: ImageVector,
    val label: String,
) {
    object Home : BottomNavItem(HomeScreenRoute, HomeScreenBaseRoute::class, Icons.Default.Home, "Home")
    object Coffee : BottomNavItem(CoffeeHomeScreenRoute, CoffeeScreenBaseRoute::class, Icons.Default.ShoppingCart, "Coffee")
    object Baby : BottomNavItem(BabyCareHomeRoute, BabyCareBaseRoute::class, Icons.Default.Face, "Baby")
}

@Composable
fun MMPBottomBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Coffee,
        BottomNavItem.Baby
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentDestination?.hierarchy?.any { it.hasRoute(item.baseRoute) } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
