package com.bsdevs.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavOptions

@Composable
fun LoginScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToCoffeeHome: (navOptions: NavOptions?) -> Unit,
    viewModel: LoginScreenViewModel = hiltViewModel()
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Login Screen")
    }
}