package com.bsdevs.babycare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType

@Composable
internal fun FeedingScreen(
    modifier: Modifier = Modifier,
    uiState: FeedingUiState,
    onToggleLeft: () -> Unit,
    onToggleRight: () -> Unit,
    onUpdateBottleAmount: (Int?) -> Unit,
    onSave: () -> Unit
) {
    var showBottleDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FeedingTimerButton(
                label = "Left",
                content = formatDuration(uiState.leftDuration),
                isSelected = uiState.isLeftRunning,
                onClick = onToggleLeft
            )
            FeedingTimerButton(
                label = "Right",
                content = formatDuration(uiState.rightDuration),
                isSelected = uiState.isRightRunning,
                onClick = onToggleRight
            )
            FeedingTimerButton(
                label = "Bottle",
                content = if (uiState.bottleAmountMl != null) "${uiState.bottleAmountMl}ml" else "Add",
                isSelected = uiState.bottleAmountMl != null,
                onClick = { showBottleDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(64.dp))

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
}

@Composable
fun FeedingTimerButton(
    label: String,
    content: String,
    isSelected: Boolean,
    onClick: () -> Unit
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
                .clickable { onClick() },
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
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
