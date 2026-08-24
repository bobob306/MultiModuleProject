package com.bsdevs.babycare.presentation

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalTime

import com.bsdevs.uicomponents.MMPScaffold

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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val horizontalPadding = if (isLandscape) 8.dp else 16.dp

    MMPScaffold(
        title = "Nappy Change",
        onBackClick = onNavigateBack,
        scrollBehavior = scrollBehavior
    ) { padding ->

        NappyChangeScreen(
            modifier = Modifier
                .padding(padding)
                .padding(start = horizontalPadding, end = horizontalPadding, bottom = 16.dp),
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
internal fun NappyChangeScreen(
    modifier: Modifier = Modifier,
    uiState: NappyChangeUiState,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onTypeChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    // 🔄 Detect device screen orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 📱 1. LANDSCAPE MODE: Two-Column Side-by-Side Layout
    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Column: Scrollable Inputs
            val leftScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(leftScrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date Field
                OutlinedTextField(
                    value = uiState.date,
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                // Time Field with click layer
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.time,
                        onValueChange = {},
                        label = { Text("Time") },
                        readOnly = true,
                        enabled = !uiState.isLoading,
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Time")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(enabled = !uiState.isLoading) { showTimePicker = true }
                    )
                }

                // Segmented Nappy Category Chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type:", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Wet", "Dirty", "Both").forEach { type ->
                            FilterChip(
                                selected = uiState.type == type,
                                onClick = { onTypeChanged(type) },
                                label = { Text(type) },
                                enabled = !uiState.isLoading
                            )
                        }
                    }
                }
            }

            // Right Column: Summary Panel & Actions Sticky on screen
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (uiState.error != null) {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(0.8f), // Keep button neatly sized
                    enabled = !uiState.isLoading
                ) {
                    Text("Save Nappy Change")
                }
            }
        }
    }
    // 📱 2. PORTRAIT MODE: Clean Single Column Layout
    else {
        val portraitScrollState = rememberScrollState()
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(portraitScrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.date,
                onValueChange = {},
                label = { Text("Date") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.time,
                    onValueChange = {},
                    label = { Text("Time") },
                    readOnly = true,
                    enabled = !uiState.isLoading,
                    trailingIcon = {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Time")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(enabled = !uiState.isLoading) { showTimePicker = true }
                )
            }

            Text("Type:", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Wet", "Dirty", "Both").forEach { type ->
                    FilterChip(
                        selected = uiState.type == type,
                        onClick = { onTypeChanged(type) },
                        label = { Text(type) },
                        enabled = !uiState.isLoading
                    )
                }
            }

            // Fixed spacer height to prevent layout calculation infinite loop bugs
            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.error != null) {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Save Nappy Change")
            }
        }
    }

    // ⏰ Material 3 Smart Orientation-Aware Time Picker Dialog Box Control
    if (showTimePicker) {
        val initialTime = try {
            LocalTime.parse(uiState.time)
        } catch (e: Exception) {
            LocalTime.now()
        }

        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 🔄 Smart UI Swap based on orientation
                    if (isLandscape) {
                        Text(
                            text = "Enter time",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        // 🎹 Sleek horizontal text boxes for Landscape
                        TimeInput(state = timePickerState)
                    } else {
                        // 🕒 Classic clock wheel for Portrait
                        TimePicker(state = timePickerState)
                    }
                }
            }
        )
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val horizontalPadding = if (isLandscape) 8.dp else 16.dp

    MMPScaffold(
        title = "Feeding Session",
        onBackClick = onNavigateBack,
        scrollBehavior = scrollBehavior
    ) { padding ->
        FeedingScreen(
            modifier = Modifier
                .padding(padding)
                .padding(start = horizontalPadding, end = horizontalPadding, bottom = 16.dp),
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
