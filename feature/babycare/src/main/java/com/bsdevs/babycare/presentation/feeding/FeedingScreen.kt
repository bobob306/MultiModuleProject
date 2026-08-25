package com.bsdevs.babycare.presentation.feeding

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.babycare.presentation.animation.MilkSplodgeAnimation
import com.bsdevs.uicomponents.LogCommentInput
import com.bsdevs.uicomponents.MMPClickableTextField
import com.bsdevs.uicomponents.MMPScaffold
import com.bsdevs.uicomponents.MMPTimePickerDialog
import java.time.LocalTime

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
                is FeedingEvent.DeleteSuccess -> {
                    onShowSnackBar("Feeding session deleted", null)
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
            onCommentChanged = viewModel::onCommentChanged,
            onSave = viewModel::submitFeeding,
            onDelete = viewModel::deleteFeeding
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
internal fun FeedingScreen(
    modifier: Modifier = Modifier,
    uiState: FeedingUiState,
    onToggleLeft: () -> Unit,
    onToggleRight: () -> Unit,
    onStartTimeSelected: (Int, Int) -> Unit,
    onLeftDurationChanged: (Long) -> Unit,
    onRightDurationChanged: (Long) -> Unit,
    onUpdateBottleAmount: (Int?) -> Unit,
    onCommentChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    var showBottleDialog by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDurationDialogForSide by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // 🔄 1. Detect screen orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()
    var isPlayingSplodge by remember { mutableStateOf(false) }

    // 📱 OPTION A: LANDSCAPE MODE (Two-Column Side-by-Side Layout)
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            Row(
                modifier = modifier
                    .fillMaxSize(),
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
                    MMPClickableTextField(
                        value = uiState.startTime,
                        label = "Start Time",
                        onClick = { showTimePicker = true },
                        enabled = !uiState.isLoading,
                        trailingIcon = Icons.Default.DateRange,
                        contentDescription = "Select Time"
                    )

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
                            onLongClick = {
                                if (!uiState.isLeftRunning) showDurationDialogForSide = "Left"
                            },
                            showEditButton = showEdit,
                            onEditClick = {
                                if (!uiState.isLeftRunning) showDurationDialogForSide = "Left"
                            }
                        )
                        FeedingTimerButton(
                            label = "Right",
                            content = formatDuration(uiState.rightDuration),
                            isSelected = uiState.isRightRunning,
                            onClick = onToggleRight,
                            onLongClick = {
                                if (!uiState.isRightRunning) showDurationDialogForSide = "Right"
                            },
                            showEditButton = showEdit,
                            onEditClick = {
                                if (!uiState.isRightRunning) showDurationDialogForSide = "Right"
                            }
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
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LogCommentInput(uiState.comment, onCommentChanged)
                    Spacer(modifier = Modifier.height(16.dp))
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
                        onClick = {
                            // 2. Instead of navigating away instantly, trigger the animation flag first!
                            isPlayingSplodge = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && !isPlayingSplodge // Disable if loading or animating
                    ) {
                        Text("Save Feeding Session")
                    }

                    if (uiState.id != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showDeleteConfirmation = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            enabled = !uiState.isLoading
                        ) {
                            Text("Delete Record")
                        }
                    }
                }
            }
        }
        // 📱 OPTION B: PORTRAIT MODE (Original Single Stack Column)
        else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                MMPClickableTextField(
                    value = uiState.startTime,
                    label = "Start Time",
                    onClick = { showTimePicker = true },
                    enabled = !uiState.isLoading,
                    trailingIcon = Icons.Default.DateRange,
                    contentDescription = "Select Time",
                    modifier = Modifier.padding(bottom = 24.dp)
                )

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
                        onLongClick = {
                            if (!uiState.isLeftRunning) showDurationDialogForSide = "Left"
                        },
                        showEditButton = showEdit,
                        onEditClick = {
                            if (!uiState.isLeftRunning) showDurationDialogForSide = "Left"
                        }
                    )
                    FeedingTimerButton(
                        label = "Right",
                        content = formatDuration(uiState.rightDuration),
                        isSelected = uiState.isRightRunning,
                        onClick = onToggleRight,
                        onLongClick = {
                            if (!uiState.isRightRunning) showDurationDialogForSide = "Right"
                        },
                        showEditButton = showEdit,
                        onEditClick = {
                            if (!uiState.isRightRunning) showDurationDialogForSide = "Right"
                        }
                    )
                    FeedingTimerButton(
                        label = "Bottle",
                        content = if (uiState.bottleAmountMl != null) "${uiState.bottleAmountMl}ml" else "Add",
                        isSelected = uiState.bottleAmountMl != null,
                        onClick = { showBottleDialog = true },
                        onLongClick = {}
                    )
                }

                LogCommentInput(
                    uiState.comment,
                    onCommentChanged,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

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
                    onClick = {
                        // 2. Instead of navigating away instantly, trigger the animation flag first!
                        isPlayingSplodge = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && !isPlayingSplodge // Disable if loading or animating
                ) {
                    Text("Save Feeding Session")
                }

                if (uiState.id != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        enabled = !uiState.isLoading
                    ) {
                        Text("Delete Record")
                    }
                }
            }
        }

        // 🍼 Bottle Entry Dialog
        if (showBottleDialog) {
            var amountText by rememberSaveable {
                mutableStateOf(
                    uiState.bottleAmountMl?.toString() ?: ""
                )
            }

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

            MMPTimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                initialTime = initialTime,
                onTimeSelected = onStartTimeSelected
            )
        }

        if (showDurationDialogForSide != null) {
            val side = showDurationDialogForSide!!
            val currentDuration =
                if (side == "Left") uiState.leftDuration else uiState.rightDuration
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
                            if (side == "Left") onLeftDurationChanged(total) else onRightDurationChanged(
                                total
                            )
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

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Feeding Session") },
                text = { Text("Are you sure you want to delete this record?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDelete()
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (isPlayingSplodge) {
            MilkSplodgeAnimation(
                onAnimationEnd = {
                    isPlayingSplodge = false // 🌟 Reset flag and proceed/navigate back
                    onSave()
                }
            )
        }
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
    val backgroundColor =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
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
