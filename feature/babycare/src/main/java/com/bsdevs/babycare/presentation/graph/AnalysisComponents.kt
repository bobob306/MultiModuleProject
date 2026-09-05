package com.bsdevs.babycare.presentation.graph

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun FeedingFrequencyChartComponent(
    uiState: FeedingGraphUiState
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Feeding Frequency by Hour",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = "Based on ${uiState.totalFeedsInCache} total recorded feeds",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (uiState.totalFeedsInCache == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No feeding logs available yet.")
            }
        } else {
            FeedingHourCanvas(
                hourlyCounts = uiState.hourlyCounts,
                barColor = MaterialTheme.colorScheme.primary,
                labelColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    .padding(horizontal = 8.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
fun FeedingGapChartComponent(
    uiState: FeedingGraphUiState
) {
    var isFullScreen by rememberSaveable { mutableStateOf(false) }

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
                        Text(text = "Average Gap Between Feeds", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { isFullScreen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DailyAverageGapSection(
                            dailyGaps = uiState.dailyAverageGaps,
                            chartHeight = 0.dp
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Average Gap Between Feeds Each Day",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp).align(Alignment.Center)
            )
            IconButton(
                onClick = { isFullScreen = true },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Full Screen",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (uiState.dailyAverageGaps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Need consecutive feeds tracked on the same day.")
            }
        } else {
            DailyAverageGapSection(
                dailyGaps = uiState.dailyAverageGaps,
                chartHeight = 260.dp
            )
        }
    }
}

@Composable
fun DailyAverageGapSection(
    dailyGaps: List<DailyAverageGap>,
    chartHeight: androidx.compose.ui.unit.Dp = 260.dp
) {
    if (dailyGaps.isEmpty()) return

    val labelColor = MaterialTheme.colorScheme.onSurface
    val gridColor = labelColor.copy(alpha = 0.15f)
    val axisLabelColor = labelColor
    val lineColor = MaterialTheme.colorScheme.tertiary

    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val sortedGaps = remember(dailyGaps) {
        dailyGaps.mapNotNull { gap ->
            try { gap to LocalDate.parse(gap.dateString, formatter) } catch (e: Exception) { null }
        }.sortedBy { it.second }
    }

    if (sortedGaps.isEmpty()) return

    val minDate = sortedGaps.first().second
    val maxDate = sortedGaps.last().second
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

        val transformState = rememberTransformableState { zoomChange, _, _ ->
            scaleFactorX = (scaleFactorX * (1f + (zoomChange - 1f) * pinchWeights.x)).coerceIn(minScale, 15f)
            val yZoom = 1f + (zoomChange - 1f) * pinchWeights.y * 0.5f
            scaleFactorY = (scaleFactorY * yZoom).coerceIn(1.0f, 4f)
        }

        val rawValues = dailyGaps.flatMap { listOfNotNull(it.averageGapMinutes.toFloat(), it.rolling14DayAverageMinutes?.toFloat()) }
        val rawMax = rawValues.maxOrNull() ?: 60f
        val rawMin = rawValues.minOrNull() ?: 30f
        
        val yMin = (rawMin * 0.9f).coerceAtLeast(0f)
        val yMax = (rawMax * 1.1f)
        val yRange = (yMax - yMin).coerceAtLeast(1f)

        val contentWidth = (baseWidthPerDay * totalDaysSpan.toFloat() * scaleFactorX)
        val chartTotalWidth = maxOf(maxWidth - leftAxisLabelSpace, contentWidth + startPadding + rightPadding)
        
        val dataViewportHeight = if (chartHeight > 0.dp) chartHeight - bottomAxisSpace else maxHeight - bottomAxisSpace
        val scrollableHeight = dataViewportHeight * scaleFactorY

        val viewportWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { (maxWidth - leftAxisLabelSpace).toPx() }

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
                    val actualChartHeight = size.height - topPadding.toPx()
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
                        val y = size.height - (ratio * actualChartHeight)

                        drawContext.canvas.nativeCanvas.drawText(
                            "${value.toInt()}m",
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
                            .pointerInput(sortedGaps, scaleFactorX, scaleFactorY) {
                                detectTapGestures { tapOffset ->
                                    val actualChartHeight = size.height - topPadding.toPx()
                                    val startX = startPadding.toPx()
                                    val usableChartWidth = (baseWidthPerDay * totalDaysSpan.toFloat() * scaleFactorX).toPx()
                                    
                                    var bestIndex: Int? = null
                                    var minDistance = 32.dp.toPx()
                                    
                                    sortedGaps.forEachIndexed { index, (item, date) ->
                                        val daysFromStart = ChronoUnit.DAYS.between(minDate, date)
                                        val x = startX + (daysFromStart.toFloat() / totalDaysSpan.toFloat() * usableChartWidth)
                                        val y = size.height - (((item.averageGapMinutes - yMin) / yRange) * actualChartHeight).toFloat()
                                        
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
                        val actualChartHeight = canvasHeight - topPadding.toPx()
                        val startX = startPadding.toPx()

                        // Grid lines (horizontal)
                        val ySteps = (5 * scaleFactorY).toInt().coerceAtLeast(5)
                        for (i in 0..ySteps) {
                            val ratio = i.toFloat() / ySteps
                            val y = canvasHeight - (ratio * actualChartHeight)
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

                        // Plot Daily Gaps (Solid Line)
                        val dailyPoints = sortedGaps.map { (item, date) ->
                            val daysFromStart = ChronoUnit.DAYS.between(minDate, date)
                            val x = startX + (daysFromStart.toFloat() / totalDaysSpan.toFloat() * usableChartWidth)
                            val y = canvasHeight - (((item.averageGapMinutes - yMin) / yRange) * actualChartHeight).toFloat()
                            Offset(x, y)
                        }

                        if (dailyPoints.size > 1) {
                            val path = Path().apply {
                                moveTo(dailyPoints[0].x, dailyPoints[0].y)
                                for (i in 1 until dailyPoints.size) {
                                    lineTo(dailyPoints[i].x, dailyPoints[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = lineColor,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        dailyPoints.forEach { point ->
                            drawCircle(color = lineColor, radius = 5.dp.toPx(), center = point)
                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = point)
                        }

                        // Plot Rolling Average (Dotted Line)
                        val rollingPoints = sortedGaps.mapNotNull { (item, date) ->
                            item.rolling14DayAverageMinutes?.let { rollingVal ->
                                val daysFromStart = ChronoUnit.DAYS.between(minDate, date)
                                val x = startX + (daysFromStart.toFloat() / totalDaysSpan.toFloat() * usableChartWidth)
                                val y = canvasHeight - (((rollingVal - yMin) / yRange) * actualChartHeight).toFloat()
                                Offset(x, y)
                            }
                        }

                        if (rollingPoints.size > 1) {
                            val path = Path().apply {
                                moveTo(rollingPoints[0].x, rollingPoints[0].y)
                                for (i in 1 until rollingPoints.size) {
                                    lineTo(rollingPoints[i].x, rollingPoints[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = lineColor,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            )
                        }

                        // Tooltip
                        selectedIndex?.let { index ->
                            val (item, _) = sortedGaps[index]
                            val point = dailyPoints[index]
                            val text = "${item.averageGapMinutes}m"
                            
                            val textPaint = Paint().apply {
                                color = Color.White.toArgb()
                                textSize = 10.sp.toPx()
                                textAlign = Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                            
                            val textBounds = Rect()
                            textPaint.getTextBounds(text, 0, text.length, textBounds)
                            
                            val hPadding = 8.dp.toPx()
                            val vPadding = 4.dp.toPx()
                            val bgWidth = textBounds.width() + hPadding * 2
                            val bgHeight = textBounds.height() + vPadding * 2
                            
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
                            
                            drawContext.canvas.nativeCanvas.drawText(
                                text,
                                bgRect.centerX(),
                                bgRect.centerY() + textBounds.height() / 2f,
                                textPaint
                            )
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
                                    datePaint.apply { alpha = 255 }
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

@Composable
fun FeedingInsightComponent(
    uiState: FeedingGraphUiState
) {
    uiState.analysisResult?.let { analysis ->
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            BucketedAnalysisInsightCard(analysis = analysis)
        }
    }
}

// --- Internal UI Components ---

@Composable
fun BucketedAnalysisInsightCard(analysis: FeedingAnalysisResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Interval Analysis by Feed Length",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Shows the average time before your baby wants to feed again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                analysis.bucketGaps.forEach { bucket ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bucket.rangeLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        if (bucket.totalCount == 0) {
                            Text(
                                text = "No data yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                    alpha = 0.5f
                                )
                            )
                        } else {
                            Text(
                                text = "${bucket.averageGapMinutes} min gap (${bucket.totalCount} feeds)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeedingHourCanvas(
    hourlyCounts: List<HourlyFeedingCount>,
    barColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    val barWidth = 44.dp
    val barSpacing = 16.dp
    val bottomLabelSpace = 32.dp
    val totalWidth = (barWidth + barSpacing) * hourlyCounts.size
    val maxFeedCount = hourlyCounts.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Box(modifier = modifier.horizontalScroll(rememberScrollState())) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(totalWidth)
        ) {
            val canvasHeight = size.height
            val chartHeight = canvasHeight - bottomLabelSpace.toPx()
            val textPaint = Paint().apply {
                color = labelColor.toArgb()
                textSize = 11.sp.toPx()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            hourlyCounts.forEachIndexed { index, item ->
                val leftX = index * (barWidth.toPx() + barSpacing.toPx())
                val barHeightRatio = item.count.toFloat() / maxFeedCount
                val barTopY = chartHeight - (chartHeight * barHeightRatio)

                if (item.count > 0) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x = leftX, y = barTopY),
                        size = Size(width = barWidth.toPx(), height = chartHeight - barTopY),
                        cornerRadius = CornerRadius(x = 6.dp.toPx(), y = 6.dp.toPx())
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        item.count.toString(),
                        leftX + (barWidth.toPx() / 2f),
                        barTopY - 8.dp.toPx(),
                        textPaint
                    )
                }

                drawContext.canvas.nativeCanvas.drawText(
                    item.displayLabel,
                    leftX + (barWidth.toPx() / 2f),
                    canvasHeight - 8.dp.toPx(),
                    textPaint
                )
            }
        }
    }
}

