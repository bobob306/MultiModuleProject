package com.bsdevs.uicomponents

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun LoadingScreen() {
    CircularProgressIndicator()
}

@Composable
fun ErrorScreen() {
    Text("Error has occurred please try again")
}