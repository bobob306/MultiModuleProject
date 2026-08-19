package com.bsdevs.babycare.presentation

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedingScreen(
    modifier: Modifier = Modifier,
    uiState: FeedingUiState,
    onToggleLeft: () -> Unit,
    onToggleRight: () -> Unit,
    onStartTimeSelected: (Int, Int) -> Unit,
    onLeftDurationChanged: (Long) -> Unit,
    onRightDurationChanged: (Long) -> Unit,
    onUpdateBottleAmount: (Int?) -> Unit,
    onSave: () -> Unit
) {
    var showBottleDialog by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDurationDialogForSide by rememberSaveable { mutableStateOf<String?>(null) }

    // 🔄 1. Detect screen orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()

    // 📱 OPTION A: LANDSCAPE MODE (Two-Column Side-by-Side Layout)
    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Column: Scrollable Inputs & Timer Buttons
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Start Time Field
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.startTime,
                        onValueChange = {},
                        label = { Text("Start Time") },
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

                // Row of Timer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val showEdit = uiState.id != null
                    FeedingTimerButton(
                        label = "Left",
                        content = formatDuration(uiState.leftDuration),
                        isSelected = uiState.isLeftRunning,
                        onClick = onToggleLeft,
                        onLongClick = { if (!uiState.isLeftRunning) showDurationDialogForSide = "Left" },
                        showEditButton = showEdit,
                        onEditClick = { if (!uiState.isLeftRunning) showDurationDialogForSide = "Left" }
                    )
                    FeedingTimerButton(
                        label = "Right",
                        content = formatDuration(uiState.rightDuration),
                        isSelected = uiState.isRightRunning,
                        onClick = onToggleRight,
                        onLongClick = { if (!uiState.isRightRunning) showDurationDialogForSide = "Right" },
                        showEditButton = showEdit,
                        onEditClick = { if (!uiState.isRightRunning) showDurationDialogForSide = "Right" }
                    )
                    FeedingTimerButton(
                        label = "Bottle",
                        content = if (uiState.bottleAmountMl != null) "${uiState.bottleAmountMl}ml" else "Add",
                        isSelected = uiState.bottleAmountMl != null,
                        onClick = { showBottleDialog = true },
                        onLongClick = {}
                    )
                }
            }

            // Right Column: Summary Panel & Active Action Button Layout
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Track Session Live",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                    Text("Save Feeding Session")
                }
            }
        }
    }
    // 📱 OPTION B: PORTRAIT MODE (Original Single Stack Column)
    else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                OutlinedTextField(
                    value = uiState.startTime,
                    onValueChange = {},
                    label = { Text("Start Time") },
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val showEdit = uiState.id != null
                FeedingTimerButton(
                    label = "Left",
                    content = formatDuration(uiState.leftDuration),
                    isSelected = uiState.isLeftRunning,
                    onClick = onToggleLeft,
                    onLongClick = { if (!uiState.isLeftRunning) showDurationDialogForSide = "Left" },
                    showEditButton = showEdit,
                    onEditClick = { if (!uiState.isLeftRunning) showDurationDialogForSide = "Left" }
                )
                FeedingTimerButton(
                    label = "Right",
                    content = formatDuration(uiState.rightDuration),
                    isSelected = uiState.isRightRunning,
                    onClick = onToggleRight,
                    onLongClick = { if (!uiState.isRightRunning) showDurationDialogForSide = "Right" },
                    showEditButton = showEdit,
                    onEditClick = { if (!uiState.isRightRunning) showDurationDialogForSide = "Right" }
                )
                FeedingTimerButton(
                    label = "Bottle",
                    content = if (uiState.bottleAmountMl != null) "${uiState.bottleAmountMl}ml" else "Add",
                    isSelected = uiState.bottleAmountMl != null,
                    onClick = { showBottleDialog = true },
                    onLongClick = {}
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

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
                Text("Save Feeding Session")
            }
        }
    }

    // 🍼 Bottle Entry Dialog
    if (showBottleDialog) {
        var amountText by rememberSaveable { mutableStateOf(uiState.bottleAmountMl?.toString() ?: "") }

        AlertDialog(
            onDismissRequest = { showBottleDialog = false },
            title = { Text("Bottle Amount (ml)") },
            text = {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.toInt() <= 999)) {
                            amountText = it
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Amount in ml") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateBottleAmount(amountText.toIntOrNull())
                        showBottleDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onUpdateBottleAmount(null)
                        showBottleDialog = false
                    }
                ) {
                    Text("Clear")
                }
            }
        )
    }

    if (showTimePicker) {
        val initialTime = try {
            LocalTime.parse(uiState.startTime)
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
                        onStartTimeSelected(timePickerState.hour, timePickerState.minute)
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
                    // 🔄 Dynamic toggle: Uses smaller text fields in landscape so it doesn't break the layout
                    if (isLandscape) {
                        Text(
                            text = "Enter time",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        TimeInput(state = timePickerState)
                    } else {
                        TimePicker(state = timePickerState)
                    }
                }
            }
        )
    }

    if (showDurationDialogForSide != null) {
        val side = showDurationDialogForSide!!
        val currentDuration = if (side == "Left") uiState.leftDuration else uiState.rightDuration
        var minutesText by remember { mutableStateOf((currentDuration / 60).toString()) }
        var secondsText by remember { mutableStateOf((currentDuration % 60).toString()) }

        AlertDialog(
            onDismissRequest = { showDurationDialogForSide = null },
            title = { Text("Edit $side Duration") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = {
                            if (it.isEmpty() || (it.all { c -> c.isDigit() } && it.toLong() <= 99)) {
                                minutesText = it
                            }
                        },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(":", modifier = Modifier.padding(horizontal = 8.dp))
                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = {
                            if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.toLong() <= 59)) {
                                secondsText = it
                            }
                        },
                        label = { Text("Sec") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                // 🛠️ FIXED: Re-added the complete missing action block cleanly here
                TextButton(
                    onClick = {
                        val mins = minutesText.toLongOrNull() ?: 0L
                        val secs = secondsText.toLongOrNull() ?: 0L
                        val total = (mins * 60) + secs
                        if (side == "Left") onLeftDurationChanged(total) else onRightDurationChanged(total)
                        showDurationDialogForSide = null
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDurationDialogForSide = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedingTimerButton(
    label: String,
    content: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showEditButton: Boolean = false,
    onEditClick: () -> Unit = {}
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = contentColor
            )
        }
        if (showEditButton) {
            TextButton(
                onClick = onEditClick,
                enabled = !isSelected,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Edit", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
