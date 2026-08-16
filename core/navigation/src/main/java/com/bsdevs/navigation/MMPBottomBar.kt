package com.bsdevs.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bsdevs.babycare.navigation.BabyCareBaseRoute
import com.bsdevs.babycare.navigation.BabyCareHomeRoute
import com.bsdevs.coffeescreen.navigation.CoffeeHomeScreenRoute
import com.bsdevs.coffeescreen.navigation.CoffeeScreenBaseRoute
import com.bsdevs.homescreen.navigation.HomeScreenBaseRoute
import com.bsdevs.homescreen.navigation.HomeScreenRoute
import kotlin.reflect.KClass

sealed class BottomNavItem(
    val route: Any,
    val baseRoute: KClass<*>,
    val icon: Any,
    val label: String,
) {
    object Home : BottomNavItem(HomeScreenRoute, HomeScreenBaseRoute::class, Icons.Default.Home, "Home")
    object Coffee : BottomNavItem(CoffeeHomeScreenRoute, CoffeeScreenBaseRoute::class, R.drawable.ic_coffee_bean, "Coffee")
    object Baby : BottomNavItem(BabyCareHomeRoute, BabyCareBaseRoute::class, Icons.Default.Face, "Baby")
}

@OptIn(ExperimentalMaterial3Api::class)
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
                icon = {
                    val tooltipState = rememberTooltipState()
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text(item.label)
                            }
                        },
                        state = tooltipState
                    ) {
                        when (val icon = item.icon) {
                            is ImageVector -> Icon(
                                imageVector = icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(32.dp)
                            )

                            is Int -> Icon(
                                painter = painterResource(id = icon),
                                contentDescription = item.label,
                                modifier = Modifier.size(32.dp)
                            )

                            else -> Unit
                        }
                    }
                },
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
