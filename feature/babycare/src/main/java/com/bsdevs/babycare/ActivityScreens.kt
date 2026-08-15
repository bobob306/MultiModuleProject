package com.bsdevs.babycare

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalTime

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
                title = { Text("Nappy Change") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
internal fun NappyChangeScreen(
    modifier: Modifier = Modifier,
    uiState: NappyChangeUiState,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onTypeChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Field (Read Only)
        OutlinedTextField(
            value = uiState.date,
            onValueChange = {},
            label = { Text("Date") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        )

        // 🌟 FIXED: Time Field with an absolute touch overlay layer
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.time,
                onValueChange = {},
                label = { Text("Time") },
                readOnly = true,
                enabled = !uiState.isLoading,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Time"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            // This transparent layer covers the text field completely and captures the click
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = !uiState.isLoading) {
                        showTimePicker = true
                    }
            )
        }

        // Segmented Nappy Category Chips
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

        Spacer(modifier = Modifier.weight(1f))

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

    // Material 3 Time Picker Dialog Window Controller
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
                    TimePicker(state = timePickerState)
                }
            }
        )
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
