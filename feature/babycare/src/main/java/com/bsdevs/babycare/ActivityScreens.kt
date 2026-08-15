package com.bsdevs.babycare

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun NappyChangeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Nappy Change Tracker (Placeholder)")
    }
}

@Composable
fun FeedingScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Feeding Tracker (Placeholder)")
    }
}
