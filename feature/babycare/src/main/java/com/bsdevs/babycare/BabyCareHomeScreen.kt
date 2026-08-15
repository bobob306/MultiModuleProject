package com.bsdevs.babycare

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BabyCareHomeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
) {
    BabyCareHomeScreen()
}

@Composable
internal fun BabyCareHomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Baby Care Section",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Coming soon: Nappy changes and Feeding trackers.",
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
