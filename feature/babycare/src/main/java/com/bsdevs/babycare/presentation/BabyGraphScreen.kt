package com.bsdevs.babycare.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.babycare.presentation.navigation.BabyGraphRoute

@Composable
fun BabyGraphRoute(
    onShowSnackBar: suspend (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: BabyGraphViewModel = hiltViewModel()
) {

}