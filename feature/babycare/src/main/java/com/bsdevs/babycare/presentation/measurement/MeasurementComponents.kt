package com.bsdevs.babycare.presentation.measurement

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bsdevs.babycare.network.MeasurementDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun GrowthChartComponent(
    title: String,
    dataType: String,
    measurements: List<MeasurementDto>,
    showWhoOverlay: Boolean = false,
    onWhoOverlayChange: (Boolean) -> Unit = {},
    birthDate: String? = null,
    gender: String? = null
) {
    var isFullScreen by rememberSaveable { mutableStateOf(false) }

    val valueSelector: (MeasurementDto) -> Double? = when (dataType) {
        "WEIGHT" -> { { it.weight } }
        "HEIGHT" -> { { it.height } }
        "HEAD" -> { { it.headCircumference } }
        else -> { { null } }
    }

    if (isFullScreen) {
        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = title, style = MaterialTheme.typography.titleLarge)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.clickable { onWhoOverlayChange(!showWhoOverlay) }
                            ) {
                                Text(
                                    text = "WHO Overlay",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (showWhoOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Switch(
                                    checked = showWhoOverlay,
                                    onCheckedChange = onWhoOverlayChange,
                                    modifier = Modifier.scale(0.7f)
                                )
                            }
                        }
                        IconButton(onClick = { isFullScreen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GrowthChartSection(
                            title = "",
                            dataType = dataType,
                            data = measurements,
                            valueSelector = valueSelector,
                            dotColorMedical = Color(0xFFEF5350),
                            dotColorSelf = Color(0xFF42A5F5),
                            showWhoOverlay = showWhoOverlay,
                            birthDate = birthDate,
                            gender = gender,
                            chartHeight = 0.dp // 0.dp will be interpreted as fillMaxHeight in our updated logic
                        )
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            GrowthChartSection(
                title = title,
                dataType = dataType,
                data = measurements,
                valueSelector = valueSelector,
                dotColorMedical = Color(0xFFEF5350), // Distinct Red 400
                dotColorSelf = Color(0xFF42A5F5),    // Distinct Blue 400
                showWhoOverlay = showWhoOverlay,
                birthDate = birthDate,
                gender = gender
            )
            IconButton(
                onClick = { isFullScreen = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Full Screen",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun LazyListScope.MeasurementHistoryItems(
    measurements: List<MeasurementDto>,
    showMedicalOnly: Boolean,
    onMedicalOnlyChange: (Boolean) -> Unit,
    showWhoOverlay: Boolean,
    onWhoOverlayChange: (Boolean) -> Unit,
    gender: String? = null,
    onEdit: (String) -> Unit,
    onDelete: (MeasurementDto) -> Unit
) {
    item(key = "measurement_history_header") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { onWhoOverlayChange(!showWhoOverlay) }
                ) {
                    val labelSuffix = when(gender?.lowercase()) {
                        "male" -> " (Boys)"
                        "female" -> " (Girls)"
                        else -> ""
                    }
                    Text(
                        text = "WHO Overlay$labelSuffix",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (showWhoOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = showWhoOverlay,
                        onCheckedChange = onWhoOverlayChange,
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }
        }
    }

    if (measurements.isEmpty()) {
        item(key = "measurement_history_empty") {
            Text("No measurements found.", modifier = Modifier.padding(16.dp))
        }
    } else {
        items(
            items = measurements,
            key = { it.id ?: "temp_${it.hashCode()}" }
        ) { item ->
            MeasurementHistoryItem(
                measurement = item,
                onEdit = { item.id?.let { onEdit(it) } },
                onDelete = { onDelete(item) }
            )
        }
    }
}

@Composable
fun GrowthChartSection(
    title: String,
    dataType: String,
    data: List<MeasurementDto>,
    valueSelector: (MeasurementDto) -> Double?,
    dotColorMedical: Color,
    dotColorSelf: Color,
    showWhoOverlay: Boolean = false,
    birthDate: String? = null,
    gender: String? = null,
    chartHeight: androidx.compose.ui.unit.Dp = 240.dp
) {
    val labelColor = MaterialTheme.colorScheme.onSurface
    val gridColor = labelColor.copy(alpha = 0.15f)
    val axisLabelColor = labelColor
    val primaryColor = MaterialTheme.colorScheme.primary
    val whoColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)

    val whoData = if (showWhoOverlay) {
        val isFemale = gender?.lowercase() == "female"
        when (dataType) {
            "WEIGHT" -> if (isFemale) WhoGrowthData.weightForAgeGirls else WhoGrowthData.weightForAgeBoys
            "HEIGHT" -> if (isFemale) WhoGrowthData.lengthForAgeGirls else WhoGrowthData.lengthForAgeBoys
            "HEAD" -> if (isFemale) WhoGrowthData.headCircumferenceForAgeGirls else WhoGrowthData.headCircumferenceForAgeBoys
            else -> emptyList()
        }
    } else {
        emptyList()
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Legend Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showWhoOverlay) {
                LegendItem("WHO Centiles", whoColor)
            }
            LegendItem("Medical", dotColorMedical)
            LegendItem("Self", dotColorSelf)
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
            val parsedBirthDate = remember(birthDate) {
                if (birthDate == null) return@remember null
                try {
                    LocalDate.parse(birthDate)
                } catch (_: Exception) {
                    try {
                        val formatter = DateTimeFormatter.ofPattern("dd MM yyyy")
                        LocalDate.parse(birthDate, formatter)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            
            val lastMeasurementDate = validData.last().second
            val ageAtLastMeasurementMonths = if (parsedBirthDate != null) {
                ChronoUnit.DAYS.between(parsedBirthDate, lastMeasurementDate) / 30.4375
            } else 0.0

            val relevantWhoData = if (showWhoOverlay && parsedBirthDate != null) {
                // Include WHO data points up to the current age plus a buffer.
                // We need at least two points to draw a line segment.
                val filtered = whoData.filter { it.month <= ageAtLastMeasurementMonths + 2 }
                if (filtered.size < 2 && whoData.size >= 2) {
                    whoData.take(2)
                } else {
                    filtered
                }
            } else emptyList()

            val minDate = if (showWhoOverlay && parsedBirthDate != null) {
                minOf(validData.first().second, parsedBirthDate)
            } else {
                validData.first().second
            }

            val maxDate = if (relevantWhoData.isNotEmpty() && parsedBirthDate != null) {
                val lastWhoMonth = relevantWhoData.last().month
                val lastWhoDate = parsedBirthDate.plusMonths(lastWhoMonth.toLong())
                maxOf(lastMeasurementDate, lastWhoDate)
            } else {
                lastMeasurementDate
            }

            val totalDaysSpan = ChronoUnit.DAYS.between(minDate, maxDate).coerceAtLeast(1L)

            val bottomAxisSpace = 32.dp
            val topPadding = 24.dp
            val leftAxisLabelSpace = 50.dp
            val baseWidthPerDay = 48.dp
            val rightPadding = 40.dp
            val startPadding = 16.dp

            val horizontalScrollState = rememberScrollState()
            val verticalScrollState = rememberScrollState()

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (chartHeight > 0.dp) Modifier.height(chartHeight) else Modifier.fillMaxHeight())
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.medium
                    )
            ) {
                val availableWidth = maxWidth - leftAxisLabelSpace - startPadding - rightPadding
                val scaleToFit = (availableWidth / (baseWidthPerDay * totalDaysSpan.toFloat())).coerceAtMost(1.0f)
                val minScale = minOf(scaleToFit, 0.05f)

                var scaleFactorX by rememberSaveable { mutableFloatStateOf(scaleToFit) }
                var scaleFactorY by rememberSaveable { mutableFloatStateOf(1.0f) }
                var pinchWeights by remember { mutableStateOf(Offset(1f, 1f)) }
                var selectedIndex by rememberSaveable { mutableStateOf<Int?>(null) }

                LaunchedEffect(scaleToFit) {
                    if (scaleFactorX < minScale) scaleFactorX = minScale
                }

                val transformState = rememberTransformableState { zoomChange, _, _ ->
                    scaleFactorX = (scaleFactorX * (1f + (zoomChange - 1f) * pinchWeights.x)).coerceIn(minScale, 15f)
                    val yZoom = 1f + (zoomChange - 1f) * pinchWeights.y * 0.5f
                    scaleFactorY = (scaleFactorY * yZoom).coerceIn(1.0f, 4f)
                }

                val userValues = validData.map { valueSelector(it.first)!! }
                val whoValues = if (showWhoOverlay && relevantWhoData.isNotEmpty()) {
                    relevantWhoData.flatMap { it.values }
                } else if (showWhoOverlay) {
                    whoData.flatMap { it.values }
                } else {
                    emptyList()
                }
                
                val allValues = userValues + whoValues
                val rawMin = allValues.minOrNull() ?: 0.0
                val rawMax = allValues.maxOrNull() ?: 1.0

                // Adaptive Y-axis: Use a tighter range to make the data more visible vertically
                val yMin = if (rawMin > 20.0) (rawMin * 0.98) else (rawMin * 0.9).coerceAtLeast(0.0)
                val yMax = (rawMax * 1.02)
                val yRange = (yMax - yMin).coerceAtLeast(0.1)

                val contentWidth = (baseWidthPerDay * totalDaysSpan.toFloat() * scaleFactorX)
                val chartTotalWidth = maxOf(maxWidth - leftAxisLabelSpace, contentWidth + startPadding + rightPadding)
                
                val dataViewportHeight = if (chartHeight > 0.dp) chartHeight - bottomAxisSpace else maxHeight - bottomAxisSpace
                val scrollableHeight = dataViewportHeight * scaleFactorY

                val viewportWidthPx = with(LocalDensity.current) { (maxWidth - leftAxisLabelSpace).toPx() }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.size >= 2) {
                                        val dx = Math.abs(event.changes[0].position.x - event.changes[1].position.x)
                                        val dy = Math.abs(event.changes[0].position.y - event.changes[1].position.y)
                                        val maxD = maxOf(dx, dy).coerceAtLeast(1f)
                                        pinchWeights = Offset(dx / maxD, dy / maxD)
                                    }
                                }
                            }
                        }
                        .transformable(state = transformState)
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        // Y-Axis
                        Canvas(
                            modifier = Modifier
                                .width(leftAxisLabelSpace)
                                .fillMaxHeight()
                                .verticalScroll(verticalScrollState)
                                .height(scrollableHeight)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f))
                        ) {
                            val chartHeight = size.height - topPadding.toPx()
                            val axisPaint = Paint().apply {
                                color = axisLabelColor.toArgb()
                                textSize = 10.sp.toPx()
                                textAlign = Paint.Align.RIGHT
                                isAntiAlias = true
                            }

                            val ySteps = (5 * scaleFactorY).toInt().coerceAtLeast(5)
                            for (i in 0..ySteps) {
                                val ratio = i.toFloat() / ySteps
                                val value = yMin + (ratio * yRange)
                                val y = size.height - (ratio * chartHeight)

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
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(verticalScrollState)
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .width(chartTotalWidth)
                                    .height(scrollableHeight)
                                    .pointerInput(validData, scaleFactorX, scaleFactorY) {
                                        detectTapGestures { tapOffset ->
                                            val chartHeight = size.height - topPadding.toPx()
                                            val startX = startPadding.toPx()
                                            val usableChartWidth = (baseWidthPerDay * totalDaysSpan.toFloat() * scaleFactorX).toPx()
                                            
                                            var bestIndex: Int? = null
                                            var minDistance = 32.dp.toPx()
                                            
                                            validData.forEachIndexed { index, (dto, date) ->
                                                val daysFromStart = ChronoUnit.DAYS.between(minDate, date)
                                                val x = startX + (daysFromStart.toFloat() / totalDaysSpan.toFloat() * usableChartWidth)
                                                val value = valueSelector(dto)!!
                                                val y = size.height - (((value - yMin) / yRange) * chartHeight).toFloat()
                                                
                                                val dist = (tapOffset - Offset(x, y)).getDistance()
                                                if (dist < minDistance) {
                                                    minDistance = dist
                                                    bestIndex = index
                                                }
                                            }
                                            selectedIndex = if (bestIndex == selectedIndex) null else bestIndex
                                        }
                                    }
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                val usableChartWidth = (baseWidthPerDay * totalDaysSpan.toFloat() * scaleFactorX).toPx()
                                val chartHeight = canvasHeight - topPadding.toPx()
                                val startX = startPadding.toPx()

                                // Grid lines (horizontal)
                                val ySteps = (5 * scaleFactorY).toInt().coerceAtLeast(5)
                                for (i in 0..ySteps) {
                                    val ratio = i.toFloat() / ySteps
                                    val y = canvasHeight - (ratio * chartHeight)
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(startX, y),
                                        end = Offset(canvasWidth, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // Grid lines (vertical - X axis guidelines)
                                val xSteps = (5 * scaleFactorX).toInt().coerceAtLeast(5)
                                for (i in 0..xSteps) {
                                    val ratio = i.toFloat() / xSteps
                                    val x = startX + (ratio * usableChartWidth)
                                    drawLine(
                                        color = gridColor.copy(alpha = 0.1f),
                                        start = Offset(x, topPadding.toPx()),
                                        end = Offset(x, canvasHeight),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // Axis Lines
                                drawLine(
                                    color = gridColor.copy(alpha = 0.4f),
                                    start = Offset(startX, topPadding.toPx()),
                                    end = Offset(startX, canvasHeight),
                                    strokeWidth = 2.dp.toPx()
                                )
                                drawLine(
                                    color = gridColor.copy(alpha = 0.4f),
                                    start = Offset(startX, canvasHeight),
                                    end = Offset(canvasWidth, canvasHeight),
                                    strokeWidth = 2.dp.toPx()
                                )

                                // WHO Overlay Plotting
                                if (showWhoOverlay && parsedBirthDate != null) {
                                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    val currentScroll = horizontalScrollState.value.toFloat()
                                    val stickyPadding = 24.dp.toPx()
                                    val labelRightLimit = (currentScroll + viewportWidthPx - stickyPadding)
                                    val desiredCentiles = listOf("25", "50", "75", "91")

                                    WhoGrowthData.centileLabels.forEachIndexed { centileIdx, label ->
                                        if (label !in desiredCentiles) return@forEachIndexed

                                        val points = relevantWhoData.map { point ->
                                            val dateAtMonth = parsedBirthDate.plusMonths(point.month.toLong())
                                            val daysFromStart = ChronoUnit.DAYS.between(minDate, dateAtMonth)
                                            val x = startX + (daysFromStart.toFloat() / totalDaysSpan.toFloat() * usableChartWidth)
                                            val value = point.values[centileIdx]
                                            val y = size.height - (((value - yMin) / yRange) * chartHeight).toFloat()
                                            Offset(x, y)
                                        }

                                        val whoPath = Path()
                                        points.forEachIndexed { pIdx, offset ->
                                            if (pIdx == 0) whoPath.moveTo(offset.x, offset.y) else whoPath.lineTo(offset.x, offset.y)
                                        }
                                        
                                        val isMedian = label == "50"
                                        drawPath(
                                            path = whoPath,
                                            color = whoColor,
                                            style = Stroke(
                                                width = if (isMedian) 2.dp.toPx() else 1.dp.toPx(),
                                                pathEffect = if (!isMedian) dashPathEffect else null
                                            )
                                        )

                                        val labelPaint = Paint().apply {
                                            color = whoColor.toArgb()
                                            textSize = 8.sp.toPx()
                                            textAlign = Paint.Align.RIGHT
                                            isAntiAlias = true
                                            isFakeBoldText = isMedian
                                        }
                                        
                                        val stickyX = labelRightLimit.coerceIn(points.first().x, points.last().x)
                                        var stickyY = points.last().y
                                        for (i in 0 until points.size - 1) {
                                            val p1 = points[i]
                                            val p2 = points[i+1]
                                            if (stickyX >= p1.x && stickyX <= p2.x) {
                                                val ratio = (stickyX - p1.x) / (p2.x - p1.x)
                                                stickyY = p1.y + ratio * (p2.y - p1.y)
                                                break
                                            }
                                        }

                                        drawContext.canvas.nativeCanvas.drawText(
                                            label,
                                            stickyX,
                                            stickyY + 3.dp.toPx(),
                                            labelPaint
                                        )
                                    }
                                }

                                // User Data Plotting
                                val points = validData.map { (dto, date) ->
                                    val daysFromStart = ChronoUnit.DAYS.between(minDate, date)
                                    val x = startX + (daysFromStart.toFloat() / totalDaysSpan.toFloat() * usableChartWidth)
                                    val value = valueSelector(dto)!!
                                    val y = size.height - (((value - yMin) / yRange) * chartHeight).toFloat()
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
                                        color = primaryColor,
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

                                // Tooltip
                                selectedIndex?.let { index ->
                                    val (dto, date) = validData[index]
                                    val point = points[index].first
                                    val value = valueSelector(dto)!!
                                    val unit = when (dataType) {
                                        "WEIGHT" -> "kg"
                                        "HEIGHT", "HEAD" -> "cm"
                                        else -> ""
                                    }
                                    val dateStr = date.format(DateTimeFormatter.ofPattern("dd/MM"))
                                    val valueStr = String.format(Locale.getDefault(), "%.1f%s", value, unit)
                                    
                                    var centileText = ""
                                    if (showWhoOverlay && parsedBirthDate != null) {
                                        val ageDays = ChronoUnit.DAYS.between(parsedBirthDate, date).toDouble()
                                        val ageMonths = ageDays / 30.4375
                                        val interpolatedWhoValues = if (whoData.isNotEmpty()) {
                                            val before = whoData.lastOrNull { it.month <= ageMonths }
                                            val after = whoData.firstOrNull { it.month > ageMonths }
                                            when {
                                                before != null && after != null -> {
                                                    val ratio = (ageMonths - before.month) / (after.month - before.month)
                                                    before.values.mapIndexed { idx, v1 -> v1 + ratio * (after.values[idx] - v1) }
                                                }
                                                before != null -> before.values
                                                after != null -> after.values
                                                else -> null
                                            }
                                        } else null

                                        if (interpolatedWhoValues != null) {
                                            val labels = WhoGrowthData.centileLabels.map { it.toDouble() }
                                            var estimatedPercentile: Double? = null
                                            when {
                                                value < interpolatedWhoValues[0] -> centileText = "\n< 0.4th centile"
                                                value > interpolatedWhoValues.last() -> centileText = "\n> 99.6th centile"
                                                else -> {
                                                    for (i in 0 until interpolatedWhoValues.size - 1) {
                                                        if (value >= interpolatedWhoValues[i] && value <= interpolatedWhoValues[i+1]) {
                                                            val ratio = (value - interpolatedWhoValues[i]) / (interpolatedWhoValues[i+1] - interpolatedWhoValues[i])
                                                            estimatedPercentile = labels[i] + (ratio * (labels[i+1] - labels[i]))
                                                            break
                                                        }
                                                    }
                                                }
                                            }
                                            estimatedPercentile?.let {
                                                centileText = String.format(Locale.getDefault(), "\n%.0fth centile", it)
                                            }
                                        }
                                    }
                                    
                                    val text = "$dateStr: $valueStr$centileText"
                                    val textPaint = Paint().apply {
                                        color = Color.White.toArgb()
                                        textSize = 10.sp.toPx()
                                        textAlign = Paint.Align.CENTER
                                        isFakeBoldText = true
                                    }
                                    val lines = text.split("\n")
                                    val textBounds = Rect()
                                    var maxW = 0
                                    var totalH = 0
                                    lines.forEach { line ->
                                        textPaint.getTextBounds(line, 0, line.length, textBounds)
                                        maxW = maxOf(maxW, textBounds.width())
                                        totalH += (textBounds.height() + 4.dp.toPx().toInt())
                                    }
                                    
                                    val hPadding = 8.dp.toPx()
                                    val vPadding = 4.dp.toPx()
                                    val bgWidth = maxW + hPadding * 2
                                    val bgHeight = totalH + vPadding * 2
                                    
                                    var tipX = point.x + 8.dp.toPx()
                                    if (tipX + bgWidth > canvasWidth - 8.dp.toPx()) tipX = point.x - bgWidth - 8.dp.toPx()
                                    
                                    val bgRect = RectF(tipX, point.y - bgHeight - 8.dp.toPx(), tipX + bgWidth, point.y - 8.dp.toPx())
                                    val bgPaint = Paint().apply {
                                        color = android.graphics.Color.BLACK
                                        alpha = 200
                                        style = Paint.Style.FILL
                                        isAntiAlias = true
                                    }
                                    drawContext.canvas.nativeCanvas.drawRoundRect(bgRect, 6.dp.toPx(), 6.dp.toPx(), bgPaint)
                                    
                                    var currentY = bgRect.top + vPadding + 10.sp.toPx()
                                    lines.forEach { line ->
                                        drawContext.canvas.nativeCanvas.drawText(line, bgRect.centerX(), currentY, textPaint)
                                        currentY += (10.sp.toPx() + 4.dp.toPx())
                                    }
                                }
                            }
                        }
                    }
                    // Bottom X-Axis labels (FIXED)
                    Row(
                        modifier = Modifier
                            .height(bottomAxisSpace)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f))
                    ) {
                        Spacer(modifier = Modifier.width(leftAxisLabelSpace))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().horizontalScroll(horizontalScrollState)) {
                            Canvas(modifier = Modifier.width(chartTotalWidth).fillMaxHeight()) {
                                val usableChartWidth = (baseWidthPerDay * totalDaysSpan.toFloat() * scaleFactorX).toPx()
                                val startX = startPadding.toPx()
                                val datePaint = Paint().apply {
                                    color = axisLabelColor.toArgb()
                                    textSize = 9.sp.toPx()
                                    textAlign = Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                                val xSteps = (5 * scaleFactorX).toInt().coerceAtLeast(5)
                                var lastLabelDate: String? = null
                                
                                for (i in 0..xSteps) {
                                    val ratio = i.toFloat() / xSteps
                                    val x = startX + (ratio * usableChartWidth)
                                    val date = minDate.plusDays((ratio * totalDaysSpan).toLong())
                                    val dateStr = date.format(DateTimeFormatter.ofPattern("dd/MM"))
                                    
                                    // Avoid overlapping labels on the same day
                                    if (dateStr != lastLabelDate) {
                                        drawLine(
                                            color = axisLabelColor.copy(alpha = 0.8f),
                                            start = Offset(x, 0f),
                                            end = Offset(x, 8.dp.toPx()),
                                            strokeWidth = 1.5.dp.toPx()
                                        )
                                        drawContext.canvas.nativeCanvas.drawText(
                                            dateStr,
                                            x,
                                            24.dp.toPx(),
                                            datePaint.apply { 
                                                color = axisLabelColor.toArgb()
                                                alpha = 255 
                                            }
                                        )
                                        lastLabelDate = dateStr
                                    }
                                }
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
