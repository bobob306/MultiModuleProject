package com.bsdevs.babycare.presentation.babyactivities

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.babycare.presentation.babyactivities.feeding.FeedingEvent
import com.bsdevs.babycare.presentation.babyactivities.feeding.FeedingScreen
import com.bsdevs.babycare.presentation.babyactivities.feeding.FeedingSide
import com.bsdevs.babycare.presentation.babyactivities.feeding.FeedingViewModel
import com.bsdevs.babycare.presentation.babyactivities.nappychange.NappyChangeEvent
import com.bsdevs.babycare.presentation.babyactivities.nappychange.NappyChangeScreen
import com.bsdevs.babycare.presentation.babyactivities.nappychange.NappyChangeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NappyChangeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: NappyChangeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NappyChangeEvent.SaveSuccess -> {
                    onShowSnackBar("Nappy change saved", null)
                    onNavigateBack()
                }
                is NappyChangeEvent.SaveError -> {
                    onShowSnackBar("Error saving nappy change: ${event.message}", null)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nappy Change", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->

        NappyChangeScreen(
            modifier = Modifier.padding(padding),
            uiState = uiState,
            onTimeSelected = { hour, minute -> viewModel.onTimeSelected(hour, minute) },
            onTypeChanged = { type -> viewModel.onTypeChanged(type) },
            onSave = { viewModel.submitNappyChange() }
        )
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedingScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: FeedingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FeedingEvent.SaveSuccess -> {
                    onShowSnackBar("Feeding session saved", null)
                    onNavigateBack()
                }
                is FeedingEvent.SaveError -> {
                    onShowSnackBar("Error saving feeding: ${event.message}", null)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feeding Session", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        FeedingScreen(
            modifier = Modifier.padding(padding),
            uiState = uiState,
            // 🔄 Map your old explicit arguments to the commonised enum trigger function
            onToggleLeft = { viewModel.toggleTimer(FeedingSide.LEFT) },
            onToggleRight = { viewModel.toggleTimer(FeedingSide.RIGHT) },
            onStartTimeSelected = viewModel::onStartTimeSelected,
            onLeftDurationChanged = viewModel::onLeftDurationChanged,
            onRightDurationChanged = viewModel::onRightDurationChanged,
            onUpdateBottleAmount = viewModel::updateBottleAmount,
            onSave = viewModel::submitFeeding
        )
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
