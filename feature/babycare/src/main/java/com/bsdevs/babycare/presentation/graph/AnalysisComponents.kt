package com.bsdevs.babycare.presentation.graph

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                modifier = Modifier.fillMaxWidth().height(200.dp),
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
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun FeedingGapChartComponent(
    uiState: FeedingGraphUiState
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Average Gap Between Feeds Each Day",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (uiState.dailyAverageGaps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Need consecutive feeds tracked on the same day.")
            }
        } else {
            DailyAverageGapCanvas(
                dailyGaps = uiState.dailyAverageGaps,
                lineColor = MaterialTheme.colorScheme.tertiary,
                labelColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp)
            )
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

@Composable
fun DailyAverageGapCanvas(
    dailyGaps: List<DailyAverageGap>,
    lineColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    if (dailyGaps.isEmpty()) return

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
    var scaleFactor by rememberSaveable { mutableStateOf(1.0f) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val transformModifier = Modifier.pointerInput(Unit) {
        detectTransformGestures { _, _, zoom, _ ->
            scaleFactor = (scaleFactor * zoom).coerceIn(0.01f, 30.0f)
        }
    }

    val baseWidthPerDay = 48.dp
    val bottomLabelSpace = 32.dp
    val topPaddingSpace = 24.dp
    val leftAxisLabelSpace = 50.dp

    val rawValues = dailyGaps.flatMap { listOfNotNull(it.averageGapMinutes, it.rolling14DayAverageMinutes) }
    val rawMax = rawValues.maxOfOrNull { it } ?: 60
    val rawMin = rawValues.minOfOrNull { it } ?: 30
    val yAxisMin = (rawMin - 10).coerceAtLeast(0)
    val yAxisMax = rawMax + 10
    val yAxisRange = (yAxisMax - yAxisMin).coerceAtLeast(1)

    val contentWidth = (baseWidthPerDay * totalDaysSpan.toFloat() * scaleFactor)
    val chartTotalWidth = maxOf(400.dp, contentWidth + 64.dp)

    Row(modifier = modifier.fillMaxHeight().then(transformModifier)) {
        Canvas(modifier = Modifier.fillMaxHeight().width(leftAxisLabelSpace).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            val chartHeight = size.height - bottomLabelSpace.toPx() - topPaddingSpace.toPx()
            val axisTextPaint = Paint().apply {
                this.color = labelColor.copy(alpha = 0.6f).toArgb()
                this.textSize = 9.sp.toPx()
                this.textAlign = Paint.Align.RIGHT
                this.isAntiAlias = true
            }

            val numYMarkers = 5
            for (i in 0 until numYMarkers) {
                val ratio = i.toFloat() / (numYMarkers - 1)
                val value = yAxisMin + (ratio * yAxisRange)
                val y = topPaddingSpace.toPx() + (chartHeight - (chartHeight * ratio))
                drawContext.canvas.nativeCanvas.drawText("${value}m", size.width - 6.dp.toPx(), y + 3.dp.toPx(), axisTextPaint)
            }
        }

        Box(modifier = Modifier.fillMaxHeight().weight(1f).horizontalScroll(rememberScrollState())) {
            Canvas(
                modifier = Modifier.fillMaxHeight().width(chartTotalWidth)
                    .pointerInput(sortedGaps, scaleFactor) {
                        detectTapGestures { tapOffset ->
                            val startX = 32.dp.toPx()
                            val chartHeight = size.height - bottomLabelSpace.toPx() - topPaddingSpace.toPx()
                            val availableWidth = size.width - 64.dp.toPx()
                            var bestIndex: Int? = null
                            var minDistance = 32.dp.toPx()
                            sortedGaps.forEachIndexed { index, (_, date) ->
                                val daysOffset = ChronoUnit.DAYS.between(minDate, date)
                                val x = startX + (daysOffset.toFloat() / totalDaysSpan.toFloat() * availableWidth)
                                val itemValue = sortedGaps[index].first.averageGapMinutes
                                val dailyY = topPaddingSpace.toPx() + (chartHeight - (chartHeight * (itemValue - yAxisMin).toFloat() / yAxisRange))
                                val dist = (tapOffset - Offset(x, dailyY)).getDistance()
                                if (dist < minDistance) { minDistance = dist; bestIndex = index }
                            }
                            selectedIndex = if (bestIndex == selectedIndex) null else bestIndex
                        }
                    }
            ) {
                val chartHeight = size.height - bottomLabelSpace.toPx() - topPaddingSpace.toPx()
                val startX = 32.dp.toPx()
                val availableWidth = size.width - 64.dp.toPx()

                sortedGaps.forEachIndexed { index, (item, date) ->
                    val x = startX + (ChronoUnit.DAYS.between(minDate, date).toFloat() / totalDaysSpan.toFloat() * availableWidth)
                    val dateStr = try { val parts = item.dateString.split("-"); "${parts[2]}/${parts[1]}" } catch (e: Exception) { "" }
                    if (index == 0 || index == sortedGaps.size - 1 || scaleFactor > 1.5f) {
                        drawContext.canvas.nativeCanvas.drawText(dateStr, x, size.height - 8.dp.toPx(), Paint().apply { color = labelColor.toArgb(); textSize = 10.sp.toPx(); textAlign = Paint.Align.CENTER })
                    }
                }
                
                val points = sortedGaps.map { (item, date) ->
                    val x = startX + (ChronoUnit.DAYS.between(minDate, date).toFloat() / totalDaysSpan.toFloat() * availableWidth)
                    val y = topPaddingSpace.toPx() + (chartHeight - (chartHeight * (item.averageGapMinutes - yAxisMin).toFloat() / yAxisRange))
                    Offset(x, y)
                }

                if (points.size > 1) {
                    for (i in 0 until points.size - 1) {
                        drawLine(color = lineColor, start = points[i], end = points[i+1], strokeWidth = 3.dp.toPx())
                    }
                }
                points.forEach { drawCircle(color = lineColor, radius = 5.dp.toPx(), center = it) }
            }
        }
    }
}
