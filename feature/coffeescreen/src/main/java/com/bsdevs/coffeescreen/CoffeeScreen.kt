package com.bsdevs.coffeescreen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CoffeeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Text("Coffee Screen")
    }
}