package com.bsdevs.babycare.presentation.temperature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.util.Locale

@Composable
fun TemperatureChart(
    readings: List<TemperatureItem>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurface
) {
    if (readings.isEmpty()) return

    // 1. Constants & Base Metrics
    val leftAxisSpace = 44.dp
    val bottomAxisSpace = 32.dp
    val topPadding = 16.dp
    val baseHourWidth = 80.dp // Reference width for 1 hour at scale 1.0
    
    val yMin = 35f
    val yMax = 42f
    val yRange = yMax - yMin

    BoxWithConstraints(
        modifier = modifier
            .height(260.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        val screenWidth = maxWidth
        
        // Calculate the scale needed to fit exactly 24 hours on screen
        val availableWidth = screenWidth - leftAxisSpace
        val scaleToFit = (availableWidth / (baseHourWidth * 24)).coerceAtLeast(0.1f)
        
        // State for zooming
        var scaleFactorX by rememberSaveable { mutableFloatStateOf(scaleToFit) }
        var scaleFactorY by rememberSaveable { mutableFloatStateOf(1.0f) }
        var pinchWeights by remember { mutableStateOf(Offset(1f, 1f)) }
        
        // Initial set or screen size change
        LaunchedEffect(scaleToFit) {
            if (scaleFactorX < scaleToFit) {
                scaleFactorX = scaleToFit
            }
        }

        val minScale = scaleToFit
        val maxScale = 5.0f

        val transformState = rememberTransformableState { zoomChange, _, _ ->
            scaleFactorX = (scaleFactorX * (1f + (zoomChange - 1f) * pinchWeights.x)).coerceIn(minScale, maxScale)
            // Make Y zoom responsive if pinched vertically, but dampened (40% sensitivity)
            val yZoom = 1f + (zoomChange - 1f) * pinchWeights.y * 0.4f
            scaleFactorY = (scaleFactorY * yZoom).coerceIn(1.0f, 3.0f)
        }

        val totalWidth = baseHourWidth * scaleFactorX * 24
        val totalHeight = maxHeight * scaleFactorY
        
        val horizontalScrollState = rememberScrollState()
        val verticalScrollState = rememberScrollState()
        
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
                // Fixed Y-Axis
                Canvas(
                    modifier = Modifier
                        .width(leftAxisSpace)
                        .fillMaxHeight()
                        .verticalScroll(verticalScrollState)
                        .height(totalHeight)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.2f))
                ) {
                    val chartHeight = size.height - topPadding.toPx()
                    val axisPaint = android.graphics.Paint().apply {
                        color = labelColor.copy(alpha = 0.6f).toArgb()
                        textSize = 9.sp.toPx()
                        textAlign = android.graphics.Paint.Align.RIGHT
                        isAntiAlias = true
                    }

                    for (temp in 35..42) {
                        val ratio = (temp - yMin) / yRange
                        val y = topPadding.toPx() + (chartHeight * (1 - ratio))
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            "${temp}°",
                            size.width - 6.dp.toPx(),
                            y + 4.dp.toPx(),
                            axisPaint
                        )
                    }
                }

                // Scrollable Chart Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    Canvas(
                        modifier = Modifier
                            .width(totalWidth)
                            .height(totalHeight)
                    ) {
                        val canvasHeight = size.height
                        val canvasWidth = size.width
                        val chartHeight = canvasHeight - topPadding.toPx()
                        val startX = 0f // Now relative to this canvas

                        // Grid lines (horizontal)
                        val ySteps = (7 * scaleFactorY).toInt().coerceAtLeast(7)
                        for (i in 0..ySteps) {
                            val ratio = i.toFloat() / ySteps
                            val y = topPadding.toPx() + (chartHeight * (1 - ratio))
                            drawLine(
                                color = labelColor.copy(alpha = 0.1f),
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Axis Line
                        drawLine(
                            color = labelColor.copy(alpha = 0.4f),
                            start = Offset(0f, topPadding.toPx() + chartHeight),
                            end = Offset(canvasWidth, topPadding.toPx() + chartHeight),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Draw X-Axis guidelines (Vertical)
                        val hourWidthPx = baseHourWidth.toPx() * scaleFactorX
                        for (hour in 0..24 step 2) {
                            val x = startX + (hour * hourWidthPx)
                            
                            // Vertical grid lines
                            drawLine(
                                color = labelColor.copy(alpha = 0.15f),
                                start = Offset(x, topPadding.toPx()),
                                end = Offset(x, topPadding.toPx() + chartHeight),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Sort readings by time for plotting
                        val sortedReadings = readings.sortedBy { it.time }

                        // Plot Points and Lines
                        val points = sortedReadings.mapNotNull { item ->
                            try {
                                val time = LocalTime.parse(item.time)
                                val hourFraction = time.hour + (time.minute / 60f)
                                val x = startX + (hourFraction * hourWidthPx)
                                
                                val tempRatio = (item.temperature.toFloat() - yMin) / yRange
                                val y = topPadding.toPx() + (chartHeight * (1 - tempRatio.coerceIn(0f, 1f)))
                                
                                Offset(x, y)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (points.size > 1) {
                            for (i in 0 until points.size - 1) {
                                drawLine(
                                    color = lineColor,
                                    start = points[i],
                                    end = points[i + 1],
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                        }

                        points.forEachIndexed { index, offset ->
                            drawCircle(
                                color = lineColor,
                                radius = 4.dp.toPx(),
                                center = offset
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = offset
                            )
                            
                            // Show value above point if zoomed in
                            if (scaleFactorX > scaleToFit * 2f) {
                                val textPaint = android.graphics.Paint().apply {
                                    color = labelColor.toArgb()
                                    textSize = 10.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                                drawContext.canvas.nativeCanvas.drawText(
                                    "${sortedReadings[index].temperature}",
                                    offset.x,
                                    offset.y - 12.dp.toPx(),
                                    textPaint
                                )
                            }
                        }
                    }
                }
            }
            // Bottom X-Axis labels
            Row(modifier = Modifier.height(bottomAxisSpace).fillMaxWidth()) {
                Spacer(modifier = Modifier.width(leftAxisSpace))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().horizontalScroll(horizontalScrollState)) {
                    Canvas(modifier = Modifier.width(totalWidth).fillMaxHeight()) {
                        val hourWidthPx = baseHourWidth.toPx() * scaleFactorX
                        val textPaint = android.graphics.Paint().apply {
                            color = labelColor.toArgb()
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        for (hour in 0..24 step 2) {
                            val x = hour * hourWidthPx
                            if (hour % 4 == 0 || scaleFactorX > scaleToFit * 1.5f) {
                                // Tick mark
                                drawLine(
                                    color = labelColor.copy(alpha = 0.6f),
                                    start = Offset(x, 0f),
                                    end = Offset(x, 8.dp.toPx()),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                                drawContext.canvas.nativeCanvas.drawText(
                                    String.format(Locale.getDefault(), "%02d:00", hour),
                                    x,
                                    24.dp.toPx(),
                                    textPaint.apply { alpha = 255 }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
