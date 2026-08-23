package com.bsdevs.babycare.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BabyGraphRoute(
    onShowSnackBar: suspend (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: BabyGraphViewModel = hiltViewModel()
) {
    val viewData by viewModel.uiState.collectAsStateWithLifecycle()

    BabyFeedingGraphScreen(
        uiState = viewData,
        onBackClick = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BabyFeedingGraphScreen(
    uiState: FeedingGraphUiState,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feeding Routine") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
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
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (uiState.totalFeedsInCache == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No feeding logs available yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // --- CHART 1: HOURLY FREQUENCY ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .horizontalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp)
                ) {
                    FeedingHourCanvas(
                        hourlyCounts = uiState.hourlyCounts,
                        barColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                }

                // --- CHART 2: DAILY AVERAGE GAP (NEW) ---
                Text(
                    text = "Average Gap Between Feeds Each Day",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
                )

                if (uiState.dailyAverageGaps.isEmpty()) {
                    Text(
                        text = "Need consecutive feeds tracked on the same day to calculate average gaps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .horizontalScroll(rememberScrollState())
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 16.dp)
                    ) {
                        DailyAverageGapCanvas(
                            dailyGaps = uiState.dailyAverageGaps,
                            lineColor = MaterialTheme.colorScheme.tertiary,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            uiState.analysisResult?.let {
                BucketedAnalysisInsightCard(analysis = uiState.analysisResult)
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
    val stepWidth = 72.dp
    val bottomLabelSpace = 32.dp
    val topPaddingSpace = 24.dp

    // Safely parse max Y axis ceiling bounds encompassing both datasets smoothly
    val maxGapValue = dailyGaps.flatMap {
        listOfNotNull(it.averageGapMinutes, it.rolling14DayAverageMinutes)
    }.maxOrNull()?.coerceAtLeast(60) ?: 60

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(stepWidth * dailyGaps.size.coerceAtLeast(1))
    ) {
        val canvasHeight = size.height
        val chartHeight = canvasHeight - bottomLabelSpace.toPx() - topPaddingSpace.toPx()

        val textPaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        val dailyPoints = mutableListOf<Offset>()
        val rollingPoints = mutableListOf<Offset>()

        // 1. Plot coordinates for both lines mapping concurrently
        dailyGaps.forEachIndexed { index, item ->
            val x = (index * stepWidth.toPx()) + (stepWidth.toPx() / 2f)

            // Map Daily Line
            val dailyRatio = item.averageGapMinutes.toFloat() / maxGapValue
            val dailyY = topPaddingSpace.toPx() + (chartHeight - (chartHeight * dailyRatio))
            dailyPoints.add(Offset(x, dailyY))

            // Map Rolling Line (if data point available)
            item.rolling14DayAverageMinutes?.let { rollingAvg ->
                val rollingRatio = rollingAvg.toFloat() / maxGapValue
                val rollingY = topPaddingSpace.toPx() + (chartHeight - (chartHeight * rollingRatio))
                rollingPoints.add(Offset(x, rollingY))
            }

            // Simple Date Truncation (Format helper block shortcut matching your previous implementation)
            val displayDate = try {
                val parts = item.dateString.split("-")
                if (parts.size >= 3) "${parts[2]} ${
                    when (parts[1]) {
                        "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"; "05" -> "May"; "06" -> "Jun";
                        "07" -> "Jul"; "08" -> "Aug"; "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; "12" -> "Dec";
                        else -> parts[1]
                    }
                }" else item.dateString
            } catch (e: Exception) { item.dateString }

            // Draw x-axis date indicator labels
            drawContext.canvas.nativeCanvas.drawText(displayDate, x, canvasHeight - 8.dp.toPx(), textPaint)

            // Draw primary single-day absolute values text label above data point
            drawContext.canvas.nativeCanvas.drawText("${item.averageGapMinutes}m", x, dailyY - 8.dp.toPx(), textPaint)
        }

        // 2. Draw Line 1: Connect standard daily averages (Solid Line)
        if (dailyPoints.size > 1) {
            for (i in 0 until dailyPoints.size - 1) {
                drawLine(
                    color = lineColor,
                    start = dailyPoints[i],
                    end = dailyPoints[i + 1],
                    strokeWidth = 3.dp.toPx()
                )
            }
        }

        // 3. Draw Line 2: Connect rolling averages (Dashed Line)
        if (rollingPoints.size > 1) {
            // Find where the index gap offsets align relative to data array starts
            val shiftOffset = dailyPoints.size - rollingPoints.size
            for (i in 0 until rollingPoints.size - 1) {
                drawLine(
                    color = lineColor.copy(alpha = 0.6f), // Slightly muted color variant
                    start = rollingPoints[i],
                    end = rollingPoints[i + 1],
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        intervals = floatArrayOf(10f, 10f), // 10px dash, 10px gap pattern
                        phase = 0f
                    )
                )
            }
        }

        // 4. Render Circular Anchor Nodes (On daily points only to avoid screen clutter)
        dailyPoints.forEach { offset ->
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = offset)
            drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = offset)
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
    // Canvas layout dimensions
    val barWidth = 44.dp
    val barSpacing = 16.dp
    val bottomLabelSpace = 32.dp

    // Find highest count to scale graph peaks proportionally
    val maxFeedCount = hourlyCounts.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width((barWidth + barSpacing) * hourlyCounts.size)
    ) {
        val canvasHeight = size.height
        val chartHeight = canvasHeight - bottomLabelSpace.toPx()

        // Native Paint setup for text rendering
        val textPaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = 11.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        hourlyCounts.forEachIndexed { index, item ->
            // Calculate absolute horizontal coordinates for this segment
            val leftX = index * (barWidth.toPx() + barSpacing.toPx())

            // Calculate relative vertical heights
            val barHeightRatio = item.count.toFloat() / maxFeedCount
            val barTopY = chartHeight - (chartHeight * barHeightRatio)

            // Only draw visual bars if data exists for this specific hour
            if (item.count > 0) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x = leftX, y = barTopY),
                    size = Size(width = barWidth.toPx(), height = chartHeight - barTopY),
                    cornerRadius = CornerRadius(x = 6.dp.toPx(), y = 6.dp.toPx())
                )

                // Draw raw count number above the bar peak
                drawContext.canvas.nativeCanvas.drawText(
                    item.count.toString(),
                    leftX + (barWidth.toPx() / 2f),
                    barTopY - 8.dp.toPx(),
                    textPaint
                )
            }

            // Draw hour timeline label along the bottom axis
            drawContext.canvas.nativeCanvas.drawText(
                item.displayLabel,
                leftX + (barWidth.toPx() / 2f),
                canvasHeight - 8.dp.toPx(),
                textPaint
            )
        }
    }
}

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

            // Render the 4 columns or rows dynamically
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
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
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