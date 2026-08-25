package com.bsdevs.babycare.presentation.temperature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        val screenWidth = maxWidth
        
        // Calculate the scale needed to fit exactly 24 hours on screen
        val availableWidth = screenWidth - leftAxisSpace
        val scaleToFit = (availableWidth / (baseHourWidth * 24)).coerceAtLeast(0.1f)
        
        // State for zooming
        var scaleFactor by remember { mutableFloatStateOf(scaleToFit) }
        
        // Initial set or screen size change
        LaunchedEffect(scaleToFit) {
            if (scaleFactor < scaleToFit) {
                scaleFactor = scaleToFit
            }
        }

        val minScale = scaleToFit
        val maxScale = 5.0f

        val transformState = rememberTransformableState { zoomChange, _, _ ->
            scaleFactor = (scaleFactor * zoomChange).coerceIn(minScale, maxScale)
        }

        val totalWidth = leftAxisSpace + (baseHourWidth * scaleFactor * 24)
        val scrollState = rememberScrollState()
        val view = LocalView.current
        
        // Standard horizontalScroll for the "good" swipe feeling
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
                .pointerInput(scaleFactor) {
                    // Only intercept if we are zoomed in enough to scroll
                    if (scaleFactor > scaleToFit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            // Force parent Pager to ignore this gesture if we can scroll
                            val canScrollLeft = dragAmount > 0 && scrollState.value > 0
                            val canScrollRight = dragAmount < 0 && scrollState.value < scrollState.maxValue
                            
                            if (canScrollLeft || canScrollRight) {
                                view.parent.requestDisallowInterceptTouchEvent(true)
                            }
                            change.consume()
                        }
                    }
                }
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(totalWidth)
            ) {
                val canvasHeight = size.height
                val canvasWidth = size.width
                val chartHeight = canvasHeight - bottomAxisSpace.toPx() - topPadding.toPx()
                val startX = leftAxisSpace.toPx()

                val textPaint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }

                val axisPaint = android.graphics.Paint().apply {
                    color = labelColor.copy(alpha = 0.6f).toArgb()
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }

                // Draw Y-Axis (35-42)
                for (temp in 35..42) {
                    val ratio = (temp - yMin) / yRange
                    val y = topPadding.toPx() + (chartHeight * (1 - ratio))
                    
                    // Grid lines
                    drawLine(
                        color = labelColor.copy(alpha = 0.1f),
                        start = Offset(startX, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    // Labels
                    drawContext.canvas.nativeCanvas.drawText(
                        "${temp}°",
                        startX - 6.dp.toPx(),
                        y + 4.dp.toPx(),
                        axisPaint
                    )
                }

                // Draw X-Axis (Time 00:00 - 24:00)
                val hourWidthPx = baseHourWidth.toPx() * scaleFactor
                for (hour in 0..24 step 2) {
                    val x = startX + (hour * hourWidthPx)
                    
                    // Vertical grid lines
                    drawLine(
                        color = labelColor.copy(alpha = 0.05f),
                        start = Offset(x, topPadding.toPx()),
                        end = Offset(x, topPadding.toPx() + chartHeight),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw labels
                    if (hour % 4 == 0 || scaleFactor > scaleToFit * 1.5f) {
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format(Locale.getDefault(), "%02d:00", hour),
                            x,
                            canvasHeight - 8.dp.toPx(),
                            textPaint
                        )
                    }
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
                    if (scaleFactor > scaleToFit * 2f) {
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
}
