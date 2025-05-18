package com.bsdevs.coffeescreen.screens.detailscreen

import androidx.compose.runtime.Composable
import androidx.navigation.NavOptions
import com.bsdevs.coffeescreen.navigation.CoffeeDetailScreenRoute

@Composable
fun CoffeeDetailScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    navigateToCoffeeHome: (navOptions: NavOptions?) -> Unit
) {

}