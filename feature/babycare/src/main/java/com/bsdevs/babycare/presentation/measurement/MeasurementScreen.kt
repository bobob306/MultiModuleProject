package com.bsdevs.babycare.presentation.measurement

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.uicomponents.LogCommentInput
import com.bsdevs.uicomponents.MMPClickableTextField
import com.bsdevs.uicomponents.MMPScaffold
import com.bsdevs.uicomponents.MMPTimePickerDialog
import com.bsdevs.uicomponents.WheelInput
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MeasurementScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MeasurementScreen(
        uiState = uiState,
        events = viewModel.events,
        onShowSnackBar = onShowSnackBar,
        onNavigateBack = onNavigateBack,
        onDateSelected = viewModel::onDateSelected,
        onTimeSelected = viewModel::onTimeSelected,
        onHeightChanged = viewModel::onHeightChanged,
        onWeightChanged = viewModel::onWeightChanged,
        onIsMedicalChanged = viewModel::onIsMedicalChanged,
        onToggleRecordHeight = viewModel::toggleRecordHeight,
        onToggleRecordWeight = viewModel::toggleRecordWeight,
        onCommentChanged = viewModel::onCommentChanged,
        onSave = viewModel::submitMeasurement,
        onDelete = viewModel::deleteMeasurement,
        onToggleMedicalOnly = viewModel::toggleMedicalOnly,
        onResetForm = viewModel::resetForm,
        onEditItem = viewModel::onEditMeasurement,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun MeasurementScreen(
    uiState: MeasurementUiState,
    events: kotlinx.coroutines.flow.Flow<MeasurementEvent>,
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateBack: () -> Unit,
    onDateSelected: (String) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onHeightChanged: (Int) -> Unit,
    onWeightChanged: (Int) -> Unit,
    onIsMedicalChanged: (Boolean) -> Unit,
    onToggleRecordHeight: (Boolean) -> Unit,
    onToggleRecordWeight: (Boolean) -> Unit,
    onCommentChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onToggleMedicalOnly: (Boolean) -> Unit,
    onResetForm: () -> Unit,
    onEditItem: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by rememberSaveable { mutableStateOf(uiState.id != null) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                is MeasurementEvent.SaveSuccess -> {
                    onShowSnackBar("Measurement saved", null)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) showSheet = false
                    }
                }
                is MeasurementEvent.DeleteSuccess -> {
                    onShowSnackBar("Measurement deleted", null)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) showSheet = false
                    }
                }
                is MeasurementEvent.SaveError -> {
                    onShowSnackBar("Error saving measurement: ${event.message}", null)
                }
            }
        }
    }

    // Re-open sheet if id is set (e.g. from navigation)
    LaunchedEffect(uiState.id) {
        if (uiState.id != null) {
            showSheet = true
        }
    }

    val filteredMeasurements = remember(uiState.allMeasurements, uiState.showMedicalOnly) {
        if (uiState.showMedicalOnly) {
            uiState.allMeasurements.filter { it.isMedical }
        } else {
            uiState.allMeasurements
        }
    }

    MMPScaffold(
        title = "Growth Charts",
        onBackClick = onNavigateBack,
        scrollBehavior = scrollBehavior,
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                onResetForm()
                showSheet = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Measurement")
            }
        }
    ) { innerPadding ->
        with(sharedTransitionScope) {
            Box(modifier = Modifier
                .fillMaxSize()
                .sharedElement(
                    sharedContentState = rememberSharedContentState(key = "tile_measurement_tile"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Medical Only", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = uiState.showMedicalOnly,
                                onCheckedChange = onToggleMedicalOnly
                            )
                        }
                    }

                    if (filteredMeasurements.isEmpty()) {
                        item {
                            Box(modifier = Modifier.height(200.dp), contentAlignment = Alignment.Center) {
                                Text("No measurements to display.")
                            }
                        }
                    } else {
                        item {
                            GrowthChartSection(
                                title = "Weight Trend (kg)",
                                data = filteredMeasurements,
                                valueSelector = { it.weight },
                                dotColorMedical = MaterialTheme.colorScheme.primary,
                                dotColorSelf = MaterialTheme.colorScheme.secondary,
                                isWeight = true
                            )
                        }

                        item {
                            GrowthChartSection(
                                title = "Height Trend (cm)",
                                data = filteredMeasurements,
                                valueSelector = { it.height },
                                dotColorMedical = MaterialTheme.colorScheme.tertiary,
                                dotColorSelf = MaterialTheme.colorScheme.outline,
                                isWeight = false
                            )
                        }
                        
                        item {
                            Text(
                                text = "History",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }

                        items(filteredMeasurements) { measurement ->
                            MeasurementHistoryItem(
                                measurement = measurement,
                                onClick = {
                                    onEditItem(measurement.id ?: "")
                                    showSheet = true
                                }
                            )
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }

                if (uiState.isLoading && !showSheet) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showSheet = false 
                },
                sheetState = sheetState
            ) {
                MeasurementForm(
                    uiState = uiState,
                    onHeightChanged = onHeightChanged,
                    onWeightChanged = onWeightChanged,
                    onIsMedicalChanged = onIsMedicalChanged,
                    onToggleRecordHeight = onToggleRecordHeight,
                    onToggleRecordWeight = onToggleRecordWeight,
                    onCommentChanged = onCommentChanged,
                    onDateSelected = { showDatePicker = true },
                    onTimeSelected = { showTimePicker = true },
                    onSave = {
                        onSave()
                    },
                    onDelete = { showDeleteConfirmation = true }
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                LocalDate.parse(uiState.date)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (e: Exception) {
                Instant.now().toEpochMilli()
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val selectedDate = Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(selectedDate.toString())
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initialTime = try {
            LocalTime.parse(uiState.time)
        } catch (e: Exception) {
            LocalTime.now()
        }

        MMPTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            initialTime = initialTime,
            onTimeSelected = { h, m ->
                onTimeSelected(h, m)
                showTimePicker = false
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Measurement") },
            text = { Text("Are you sure you want to delete this record?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showSheet = false
                        }
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
}

@Composable
fun MeasurementHistoryItem(
    measurement: com.bsdevs.babycare.network.MeasurementDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MonitorWeight,
                contentDescription = null,
                tint = if (measurement.isMedical) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                val weightStr = measurement.weight?.let { String.format(Locale.getDefault(), "%.2fkg", it) } ?: ""
                val heightStr = measurement.height?.let { String.format(Locale.getDefault(), "%.1fcm", it) } ?: ""
                Text(
                    text = "$weightStr $heightStr".trim(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (measurement.isMedical) "Medical check-up" else "Self measurement",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val displayDate = try {
                    val date = measurement.date ?: ""
                    val parts = date.split("-")
                    if (parts.size >= 3) {
                        "${parts[2]} ${parts[1]} ${parts[0].substring(2)}"
                    } else date
                } catch (e: Exception) { measurement.date ?: "" }
                Text(text = displayDate, style = MaterialTheme.typography.bodyMedium)
                Text(text = measurement.time ?: "", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun GrowthChartSection(
    title: String,
    data: List<com.bsdevs.babycare.network.MeasurementDto>,
    valueSelector: (com.bsdevs.babycare.network.MeasurementDto) -> Double?,
    dotColorMedical: Color,
    dotColorSelf: Color,
    isWeight: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            MeasurementLineChart(
                measurements = data,
                valueSelector = valueSelector,
                dotColorMedical = dotColorMedical,
                dotColorSelf = dotColorSelf,
                isWeight = isWeight
            )
        }
    }
}

@Composable
fun MeasurementLineChart(
    measurements: List<com.bsdevs.babycare.network.MeasurementDto>,
    valueSelector: (com.bsdevs.babycare.network.MeasurementDto) -> Double?,
    dotColorMedical: Color,
    dotColorSelf: Color,
    isWeight: Boolean
) {
    val sortedData = remember(measurements) {
        measurements.filter { valueSelector(it) != null }.sortedBy { it.dateTime }
    }

    if (sortedData.isEmpty()) return

    var scaleFactor by remember { mutableFloatStateOf(1.0f) }
    val baseStepWidth = 80.dp
    
    // Support Pinch Zoom + One Finger Pan (via horizontalScroll)
    val transformModifier = Modifier.pointerInput(Unit) {
        detectTransformGestures { _, _, zoom, _ ->
            scaleFactor = (scaleFactor * zoom).coerceIn(0.1f, 10.0f)
        }
    }

    val values = sortedData.mapNotNull { valueSelector(it) }
    val maxVal = values.maxOrNull() ?: 1.0
    val minVal = values.minOrNull() ?: 0.0
    val range = (maxVal - minVal).coerceAtLeast(0.1)
    
    val yPadding = 40.dp
    val xLabelSpace = 40.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(transformModifier)
            .horizontalScroll(rememberScrollState())
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(xLabelSpace + (baseStepWidth * scaleFactor * (sortedData.size - 1).coerceAtLeast(1)))
        ) {
            val chartHeight = size.height - yPadding.toPx() * 2
            val startX = xLabelSpace.toPx()
            val stepWidth = baseStepWidth.toPx() * scaleFactor

            // Draw grid lines
            val gridColor = Color.Gray.copy(alpha = 0.2f)
            drawLine(
                color = gridColor,
                start = Offset(startX, yPadding.toPx()),
                end = Offset(startX, size.height - yPadding.toPx()),
                strokeWidth = 1.dp.toPx()
            )

            // Y-Axis labels
            val textPaint = android.graphics.Paint().apply {
                color = Color.Gray.toArgb()
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            
            drawContext.canvas.nativeCanvas.drawText(
                String.format(Locale.getDefault(), if (isWeight) "%.1f" else "%.0f", maxVal),
                startX - 8.dp.toPx(),
                yPadding.toPx() + 4.dp.toPx(),
                textPaint
            )
            drawContext.canvas.nativeCanvas.drawText(
                String.format(Locale.getDefault(), if (isWeight) "%.1f" else "%.0f", minVal),
                startX - 8.dp.toPx(),
                size.height - yPadding.toPx() + 4.dp.toPx(),
                textPaint
            )

            val points = mutableListOf<Offset>()
            sortedData.forEachIndexed { index, item ->
                val value = valueSelector(item) ?: return@forEachIndexed
                val x = startX + index * stepWidth
                val ratio = ((value - minVal) / range).toFloat()
                val y = size.height - yPadding.toPx() - (ratio * chartHeight)
                points.add(Offset(x, y))
            }

            // Draw line
            if (points.size > 1) {
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // Draw dots
            points.forEachIndexed { index, offset ->
                val isMedical = sortedData[index].isMedical
                val color = if (isMedical) dotColorMedical else dotColorSelf
                
                drawCircle(
                    color = color,
                    radius = 5.dp.toPx(),
                    center = offset
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = offset
                )
                
                // Date labels if zoomed in enough
                if (scaleFactor > 0.6f) {
                    val dateStr = try {
                        val date = sortedData[index].date ?: ""
                        val parts = date.split("-")
                        if (parts.size >= 3) {
                            "${parts[2]} ${parts[1]} ${parts[0].substring(2)}"
                        } else date
                    } catch (e: Exception) { "" }
                    drawContext.canvas.nativeCanvas.drawText(
                        dateStr,
                        offset.x,
                        size.height - 8.dp.toPx(),
                        android.graphics.Paint().apply {
                            this.color = Color.Gray.toArgb()
                            this.textSize = 9.sp.toPx()
                            this.textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MeasurementForm(
    uiState: MeasurementUiState,
    onHeightChanged: (Int) -> Unit,
    onWeightChanged: (Int) -> Unit,
    onIsMedicalChanged: (Boolean) -> Unit,
    onToggleRecordHeight: (Boolean) -> Unit,
    onToggleRecordWeight: (Boolean) -> Unit,
    onCommentChanged: (String) -> Unit,
    onDateSelected: () -> Unit,
    onTimeSelected: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .imePadding() // Keyboard support
            .verticalScroll(rememberScrollState()), // Ensure form is scrollable if keyboard takes space
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (uiState.id == null) "Add Measurement" else "Edit Measurement",
            style = MaterialTheme.typography.headlineSmall
        )
        
        HorizontalDivider()

        val displayDate = try {
            val parts = uiState.date.split("-")
            if (parts.size >= 3) {
                "${parts[2]} ${parts[1]} ${parts[0].substring(2)}"
            } else uiState.date
        } catch (e: Exception) { uiState.date }

        MMPClickableTextField(
            value = displayDate,
            label = "Date",
            onClick = onDateSelected,
            enabled = !uiState.isLoading,
            trailingIcon = Icons.Default.DateRange,
            contentDescription = "Select Date"
        )

        MMPClickableTextField(
            value = uiState.time,
            label = "Time",
            onClick = onTimeSelected,
            enabled = !uiState.isLoading,
            trailingIcon = Icons.Default.DateRange,
            contentDescription = "Select Time"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Medical Recording?", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = uiState.isMedical,
                onCheckedChange = onIsMedicalChanged,
                enabled = !uiState.isLoading
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Height Toggle & Input
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Record Height", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = uiState.recordHeight, onCheckedChange = onToggleRecordHeight, enabled = !uiState.isLoading)
            }
            
            if (uiState.recordHeight) {
                Spacer(modifier = Modifier.height(8.dp))
                WheelInput(
                    decimalPlaces = 1,
                    startNumber = 300,
                    endNumber = 1200,
                    initialSelectedItem = ((uiState.height ?: 50.0) * 10).toInt(),
                    onItemSelected = onHeightChanged,
                    label = "Height (cm)"
                )
            }
        }

        // Weight Toggle & Input
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Record Weight", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = uiState.recordWeight, onCheckedChange = onToggleRecordWeight, enabled = !uiState.isLoading)
            }
            
            if (uiState.recordWeight) {
                Spacer(modifier = Modifier.height(8.dp))
                WheelInput(
                    decimalPlaces = 2,
                    startNumber = 200,
                    endNumber = 2000,
                    initialSelectedItem = ((uiState.weight ?: 3.5) * 100).toInt(),
                    onItemSelected = onWeightChanged,
                    label = "Weight (kg)"
                )
            }
        }

        LogCommentInput(
            comment = uiState.comment,
            onCommentChange = onCommentChanged,
            modifier = Modifier.fillMaxWidth()
        )

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
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(if (uiState.id == null) "Save Measurement" else "Update Measurement")
            }
        }

        if (uiState.id != null) {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                enabled = !uiState.isLoading
            ) {
                Text("Delete Record")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
