package com.bsdevs.babycare.presentation.nappy

import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.babycare.presentation.animation.TurdSplodgeAnimation
import com.bsdevs.uicomponents.LogCommentInput
import com.bsdevs.uicomponents.MMPClickableTextField
import com.bsdevs.uicomponents.MMPScaffold
import com.bsdevs.uicomponents.MMPTimePickerDialog
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NappyChangeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
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
                is NappyChangeEvent.DeleteSuccess -> {
                    onShowSnackBar("Nappy change deleted", null)
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
        scrollBehavior = scrollBehavior,
    ) { padding ->

        NappyChangeScreen(
            modifier = Modifier
                .padding(padding)
                .padding(start = horizontalPadding, end = horizontalPadding, bottom = 16.dp),
            uiState = uiState,
            onTimeSelected = { hour, minute -> viewModel.onTimeSelected(hour, minute) },
            onTypeChanged = { type -> viewModel.onTypeChanged(type) },
            onCommentChanged = viewModel::onCommentChanged,
            onSave = { viewModel.submitNappyChange() },
            onDelete = { viewModel.deleteNappyChange() },
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun NappyChangeScreen(
    modifier: Modifier = Modifier,
    uiState: NappyChangeUiState,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onTypeChanged: (String) -> Unit,
    onCommentChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // 🔄 Detect device screen orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isPlayingTurdAnimation by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        with(sharedTransitionScope) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "tile_nappy_tile"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            ) {
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
                    MMPClickableTextField(
                        value = uiState.time,
                        label = "Time",
                        onClick = { showTimePicker = true },
                        enabled = !uiState.isLoading,
                        trailingIcon = Icons.Default.DateRange,
                        contentDescription = "Select Time"
                    )

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
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LogCommentInput(uiState.comment, onCommentChanged)
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
                            // Trigger the poop animation burst overlay instead of navigating away instantly
                            isPlayingTurdAnimation = true
                        },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        enabled = !uiState.isLoading && !isPlayingTurdAnimation
                    ) {
                        Text("Save Nappy Change")
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
        // 📱 2. PORTRAIT MODE: Clean Single Column Layout
        else {
            val portraitScrollState = rememberScrollState()
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(portraitScrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = uiState.date,
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                MMPClickableTextField(
                    value = uiState.time,
                    label = "Time",
                    onClick = { showTimePicker = true },
                    enabled = !uiState.isLoading,
                    trailingIcon = Icons.Default.DateRange,
                    contentDescription = "Select Time"
                )

                Text("Type:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
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

                LogCommentInput(
                    uiState.comment,
                    onCommentChanged,
                    modifier = Modifier.padding(8.dp)
                )

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
                    onClick = {
                        // Trigger the poop animation burst overlay instead of navigating away instantly
                        isPlayingTurdAnimation = true
                    },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    enabled = !uiState.isLoading && !isPlayingTurdAnimation
                ) {
                    Text("Save Nappy Change")
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

        // ⏰ Material 3 Smart Orientation-Aware Time Picker Dialog Box Control
        if (showTimePicker) {
            val initialTime = try {
                LocalTime.parse(uiState.time)
            } catch (e: Exception) {
                LocalTime.now()
            }

            MMPTimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                initialTime = initialTime,
                onTimeSelected = onTimeSelected
            )
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Nappy Change") },
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

        if (isPlayingTurdAnimation) {
            TurdSplodgeAnimation(
                onAnimationEnd = {
                    isPlayingTurdAnimation = false
                    onSave() // Fire your Firestore update and navigate away
                }
            )
        }
        }
    }
}
}
