package com.bsdevs.babycare.presentation.babyactivities.nappychange

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.bsdevs.renderer.components.BabyCareTimePickerDialog

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
                .fillMaxSize()
                .padding(horizontal = 16.dp),
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
                .padding(16.dp)
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
    BabyCareTimePickerDialog(
        showDialog = showTimePicker,
        rawTimeString = uiState.time,
        isLandscape = isLandscape,
        onDismissRequest = { showTimePicker = false },
        onTimeSelected = { hour, minute -> onTimeSelected(hour, minute) }
    )
}