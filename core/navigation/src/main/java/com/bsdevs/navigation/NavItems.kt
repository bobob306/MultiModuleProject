package com.bsdevs.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import com.bsdevs.babycare.presentation.navigation.BabyCareBaseRoute
import com.bsdevs.babycare.presentation.navigation.BabyCareHomeRoute
import com.bsdevs.babycare.presentation.navigation.ShoppingListBaseRoute
import com.bsdevs.babycare.presentation.navigation.ShoppingListRoute
import com.bsdevs.coffeescreen.navigation.CoffeeHomeScreenRoute
import com.bsdevs.coffeescreen.navigation.CoffeeScreenBaseRoute
import com.bsdevs.homescreen.navigation.HomeScreenBaseRoute
import com.bsdevs.homescreen.navigation.HomeScreenRoute
import com.bsdevs.homescreen.navigation.SettingsBaseRoute
import com.bsdevs.homescreen.navigation.SettingsRoute
import com.bsdevs.navigation.R
import kotlin.reflect.KClass

sealed class BottomNavItem(
    val route: Any,
    val baseRoute: KClass<*>,
    val icon: Any,
    val label: String,
) {
    object Home : BottomNavItem(HomeScreenRoute, HomeScreenBaseRoute::class, Icons.Default.Home, "Home")
    object Coffee : BottomNavItem(CoffeeHomeScreenRoute, CoffeeScreenBaseRoute::class, R.drawable.ic_coffee_bean, "Coffee")
    object ShoppingList : BottomNavItem(ShoppingListRoute, ShoppingListBaseRoute::class, Icons.Default.ShoppingCart, "Shopping List")
    object Baby : BottomNavItem(BabyCareHomeRoute, BabyCareBaseRoute::class, Icons.Default.Face, "Baby")
    object Settings : BottomNavItem(SettingsRoute, SettingsBaseRoute::class, Icons.Default.Settings, "Settings")
}

fun getNavItems(userRoles: List<String>): List<BottomNavItem> {
    return listOfNotNull(
        BottomNavItem.Home,
        BottomNavItem.Coffee.takeIf { "coffee" in userRoles },
        BottomNavItem.ShoppingList.takeIf { "shopping_list" in userRoles },
        BottomNavItem.Baby.takeIf { "parent" in userRoles },
        BottomNavItem.Settings
    )
}
