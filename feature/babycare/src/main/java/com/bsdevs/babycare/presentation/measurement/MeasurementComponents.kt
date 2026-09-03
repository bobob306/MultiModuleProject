package com.bsdevs.babycare.presentation.measurement

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsdevs.babycare.network.MeasurementDto
import com.bsdevs.uicomponents.DeleteConfirmationDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun GrowthChartComponent(
    title: String,
    dataType: String,
    measurements: List<MeasurementDto>
) {
    val valueSelector: (MeasurementDto) -> Double? = when (dataType) {
        "WEIGHT" -> { { it.weight } }
        "HEIGHT" -> { { it.height } }
        "HEAD" -> { { it.headCircumference } }
        else -> { { null } }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        GrowthChartSection(
            title = title,
            data = measurements,
            valueSelector = valueSelector,
            dotColorMedical = Color(0xFFEF5350), // Distinct Red 400
            dotColorSelf = Color(0xFF42A5F5)    // Distinct Blue 400
        )
    }
}

@Composable
fun MeasurementHistoryComponent(
    measurements: List<MeasurementDto>,
    showMedicalOnly: Boolean,
    onMedicalOnlyChange: (Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String, String) -> Unit
) {
    var itemToDelete by remember { mutableStateOf<MeasurementDto?>(null) }
    
    // ...

    if (itemToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                itemToDelete?.let { item ->
                    val id = item.id
                    val date = item.date
                    if (id != null && date != null) {
                        onDelete(id, date)
                    }
                }
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Measurement History",
                style = MaterialTheme.typography.titleMedium,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable { onMedicalOnlyChange(!showMedicalOnly) }
            ) {
                Text(
                    text = "Medical Only",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (showMedicalOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = showMedicalOnly,
                    onCheckedChange = onMedicalOnlyChange,
                    modifier = Modifier.scale(0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (measurements.isEmpty()) {
            Text("No measurements found.", modifier = Modifier.padding(horizontal = 8.dp))
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                measurements.forEach { item ->
                    MeasurementHistoryItem(
                        measurement = item,
                        onEdit = { item.id?.let { onEdit(it) } },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }
    }
}

@Composable
fun GrowthChartSection(
    title: String,
    data: List<MeasurementDto>,
    valueSelector: (MeasurementDto) -> Double?,
    dotColorMedical: Color,
    dotColorSelf: Color
) {
    val labelColor = MaterialTheme.colorScheme.onSurface
    val gridColor = labelColor.copy(alpha = 0.1f)
    val axisLabelColor = labelColor.copy(alpha = 0.6f)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Legend
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                LegendItem("Medical", dotColorMedical)
                LegendItem("Self", dotColorSelf)
            }
        }

        val validData = remember(data, valueSelector) {
            data.filter { valueSelector(it) != null }
                .mapNotNull { dto ->
                    val date = try {
                        LocalDate.parse(dto.date)
                    } catch (_: Exception) {
                        null
                    }
                    if (date != null) dto to date else null
                }
                .sortedBy { it.second }
        }

        if (validData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Not enough data for chart")
            }
        } else {
            val minDate = validData.first().second
            val maxDate = validData.last().second
            val totalDaysSpan = ChronoUnit.DAYS.between(minDate, maxDate).coerceAtLeast(1L)

            val bottomAxisSpace = 32.dp
            val topPadding = 24.dp
            val leftAxisLabelSpace = 50.dp
            val baseWidthPerDay = 48.dp

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.medium
                    )
            ) {
                val availableWidth = maxWidth - leftAxisLabelSpace - 32.dp
                val scaleToFit = (availableWidth / (baseWidthPerDay * totalDaysSpan.toFloat())).coerceAtMost(1.0f)
                val minScale = minOf(scaleToFit, 0.05f)

                var scaleFactor by rememberSaveable { mutableFloatStateOf(1.0f) }

                LaunchedEffect(scaleToFit) {
                    if (scaleFactor < minScale) scaleFactor = minScale
                }

                val transformModifier = Modifier.pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        scaleFactor = (scaleFactor * zoom).coerceIn(minScale, 15f)
                    }
                }

                val values = validData.map { valueSelector(it.first)!! }
                val rawMin = values.minOrNull() ?: 0.0
                val rawMax = values.maxOrNull() ?: 1.0

                val yMin = (rawMin * 0.95).coerceAtLeast(0.0)
                val yMax = (rawMax * 1.05)
                val yRange = (yMax - yMin).coerceAtLeast(0.1)

                val contentWidth = (baseWidthPerDay * totalDaysSpan.toFloat() * scaleFactor)
                val chartTotalWidth = maxOf(maxWidth - leftAxisLabelSpace, contentWidth + 32.dp)

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(transformModifier)
                ) {
                // Y-Axis
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(leftAxisLabelSpace)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.2f))
                ) {
                    val chartHeight = size.height - bottomAxisSpace.toPx() - topPadding.toPx()
                    val axisPaint = Paint().apply {
                        color = axisLabelColor.toArgb()
                        textSize = 10.sp.toPx()
                        textAlign = Paint.Align.RIGHT
                        isAntiAlias = true
                    }

                    val steps = 5
                    for (i in 0..steps) {
                        val ratio = i.toFloat() / steps
                        val value = yMin + (ratio * yRange)
                        val y = size.height - bottomAxisSpace.toPx() - (ratio * chartHeight)

                        drawContext.canvas.nativeCanvas.drawText(
                            String.format(Locale.getDefault(), "%.1f", value),
                            size.width - 8.dp.toPx(),
                            y + 4.dp.toPx(),
                            axisPaint
                        )
                    }
                }

                // Chart Content
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Canvas(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(chartTotalWidth)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val chartWidth = canvasWidth - 32.dp.toPx()
                        val chartHeight = canvasHeight - bottomAxisSpace.toPx() - topPadding.toPx()
                        val startX = 16.dp.toPx()

                        val datePaint = Paint().apply {
                            color = axisLabelColor.toArgb()
                            textSize = 9.sp.toPx()
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                        }

                        // Grid lines (horizontal)
                        val steps = 5
                        for (i in 0..steps) {
                            val ratio = i.toFloat() / steps
                            val y = size.height - bottomAxisSpace.toPx() - (ratio * chartHeight)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Dates and Plotting
                        val points = validData.map { (dto, date) ->
                            val daysFromStart = ChronoUnit.DAYS.between(minDate, date)
                            val x = startX + (daysFromStart.toFloat() / totalDaysSpan.toFloat() * chartWidth)
                            val value = valueSelector(dto)!!
                            val y = size.height - bottomAxisSpace.toPx() - (((value - yMin) / yRange) * chartHeight).toFloat()
                            
                            // Draw date label if appropriate
                            if (scaleFactor > 1.5f || daysFromStart % (totalDaysSpan / 4 + 1) == 0L) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    date.format(DateTimeFormatter.ofPattern("dd/MM")),
                                    x,
                                    canvasHeight - 8.dp.toPx(),
                                    datePaint
                                )
                            }
                            
                            Offset(x, y) to dto.isMedical
                        }

                        if (points.size > 1) {
                            val path = Path().apply {
                                moveTo(points[0].first.x, points[0].first.y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].first.x, points[i].first.y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = primaryColor.copy(alpha = 0.3f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        points.forEach { (point, isMedical) ->
                            drawCircle(
                                color = if (isMedical) dotColorMedical else dotColorSelf,
                                radius = 5.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = point
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementHistoryItem(
    measurement: MeasurementDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onDelete()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> {
                onEdit()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.Settled -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            val bgColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
            }
            val swipeIcon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Edit
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(bgColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = swipeIcon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (direction == SwipeToDismissBoxValue.StartToEnd)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onEdit,
                    onLongClick = onEdit
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconBgColor = if (measurement.isMedical) Color(0xFFFFEBEE) else Color(0xFFE3F2FD)
                val iconTintColor = if (measurement.isMedical) Color(0xFFEF5350) else Color(0xFF42A5F5)

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBgColor, CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (measurement.isMedical) Icons.Default.MedicalServices else Icons.Default.AutoGraph,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = iconTintColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val weightStr = measurement.weight?.let { "Weight: " + String.format(Locale.getDefault(), "%.2fkg", it) } ?: ""
                    val heightStr = measurement.height?.let { "Height: " + String.format(Locale.getDefault(), "%.1fcm", it) } ?: ""
                    val headStr = measurement.headCircumference?.let { "Head Circ.: " + String.format(Locale.getDefault(), "%.1fcm", it) } ?: ""
                    val typeStr = if (measurement.isMedical) " (Medical)" else " (Self)"
                    val title = "${listOf(weightStr, heightStr, headStr).filter { it.isNotEmpty() }.joinToString(", ")}$typeStr"

                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    if (!measurement.comment.isNullOrEmpty()) {
                        Text(
                            text = measurement.comment,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val displayDate = remember(measurement.date) {
                    try {
                        val date = LocalDate.parse(measurement.date)
                        date.format(DateTimeFormatter.ofPattern("dd MMM"))
                    } catch (_: Exception) {
                        measurement.date ?: ""
                    }
                }

                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
