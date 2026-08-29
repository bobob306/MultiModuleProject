package com.bsdevs.babycare.presentation.graph

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.uicomponents.MMPScaffold

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BabyGraphRoute(
    onShowSnackBar: suspend (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: BabyGraphViewModel = hiltViewModel()
) {
    val viewData by viewModel.uiState.collectAsStateWithLifecycle()

    BabyFeedingGraphScreen(
        uiState = viewData,
        onBackClick = onNavigateBack,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun BabyFeedingGraphScreen(
    uiState: FeedingGraphUiState,
    onBackClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val horizontalPadding = if (isLandscape) 8.dp else 16.dp

    MMPScaffold(
        title = "Feeding Routine",
        onBackClick = onBackClick,
        scrollBehavior = scrollBehavior
    ) { innerPadding ->
        with(sharedTransitionScope) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "tile_graph_tile"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            bottom = 16.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        Column(
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
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.3f
                                            )
                                        )
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
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.3f
                                                )
                                            )
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
    // 1. Establish trackable scaling variables
    var scaleFactor by remember { mutableFloatStateOf(1.0f) }

    // 🌟 THE FIX: Lower minScale to 0.2f to allow zooming out much further
    val minScale = 0.2f
    val maxScale = 3.0f

    val transformState = rememberTransformableState { _, zoom, _, _ ->
        scaleFactor = (scaleFactor * zoom).coerceIn(minScale, maxScale)
    }

    // 2. Base layout static metrics
    val baseStepWidth = 72.dp
    val bottomLabelSpace = 32.dp
    val topPaddingSpace = 24.dp
    val leftAxisLabelSpace = 44.dp

    val rawValues = dailyGaps.flatMap {
        listOfNotNull(it.averageGapMinutes, it.rolling14DayAverageMinutes)
    }
    val rawMax = rawValues.maxOrNull() ?: 60
    val rawMin = rawValues.minOrNull() ?: 30

    val yAxisMin = (rawMin - 10).coerceAtLeast(0)
    val yAxisMax = rawMax + 10
    val yAxisRange = (yAxisMax - yAxisMin).coerceAtLeast(1)

    // 3. Wrap everything inside a Transformable Box container layout
    Box(
        modifier = modifier
            .fillMaxHeight()
            .transformable(state = transformState) // Captures multi-touch gestures safely
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                // 🌟 THE FIX: Multiply stepWidth by scaleFactor to widen/shrink graph lines dynamically
                .width(
                    leftAxisLabelSpace + ((baseStepWidth * scaleFactor) * dailyGaps.size.coerceAtLeast(
                        1
                    ))
                )
        ) {
            val canvasHeight = size.height
            val canvasWidth = size.width
            val chartHeight = canvasHeight - bottomLabelSpace.toPx() - topPaddingSpace.toPx()
            val startX = leftAxisLabelSpace.toPx()

            val textPaint = android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }

            val axisTextPaint = android.graphics.Paint().apply {
                color = labelColor.copy(alpha = 0.6f).toArgb()
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
                isAntiAlias = true
            }

            // Draw Y-Axis Line & Labels
            drawLine(
                color = lineColor.copy(alpha = 0.3f),
                start = Offset(startX, topPaddingSpace.toPx()),
                end = Offset(startX, topPaddingSpace.toPx() + chartHeight),
                strokeWidth = 1.dp.toPx()
            )

            val yLabels = listOf(yAxisMin, yAxisMin + (yAxisRange / 2), yAxisMax)
            yLabels.forEach { value ->
                val ratio = (value - yAxisMin).toFloat() / yAxisRange
                val labelY = topPaddingSpace.toPx() + (chartHeight - (chartHeight * ratio))

                drawContext.canvas.nativeCanvas.drawText(
                    "${value}m",
                    startX - 6.dp.toPx(),
                    labelY + 3.dp.toPx(),
                    axisTextPaint
                )

                drawLine(
                    color = lineColor.copy(alpha = 0.08f),
                    start = Offset(startX, labelY),
                    end = Offset(canvasWidth, labelY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Plot Graph lines and nodes
            val dailyPoints = mutableListOf<Offset>()
            val rollingPoints = mutableListOf<Offset>()

            dailyGaps.forEachIndexed { index, item ->
                // Calculate the scaled layout track offsets dynamically
                val scaledStepWidth = baseStepWidth.toPx() * scaleFactor
                val x = startX + (index * scaledStepWidth) + (scaledStepWidth / 2f)

                val dailyRatio = (item.averageGapMinutes - yAxisMin).toFloat() / yAxisRange
                val dailyY = topPaddingSpace.toPx() + (chartHeight - (chartHeight * dailyRatio))
                dailyPoints.add(Offset(x, dailyY))

                item.rolling14DayAverageMinutes?.let { rollingAvg ->
                    val rollingRatio = (rollingAvg - yAxisMin).toFloat() / yAxisRange
                    val rollingY =
                        topPaddingSpace.toPx() + (chartHeight - (chartHeight * rollingRatio))
                    rollingPoints.add(Offset(x, rollingY))
                }

                // --- 🌟 CONDITIONAL TEXT LOGIC STARTS HERE ---
                if (scaleFactor > 0.5f) {
                    // 1. Zoomed In: Show complete detailed labels normally
                    val displayDate = try {
                        val parts = item.dateString.split("-")
                        if (parts.size >= 3) {
                            "${parts[2]} ${parts[1]} ${parts[0].substring(2)}"
                        } else item.dateString
                    } catch (e: Exception) {
                        item.dateString
                    }

                    // Draw standard date text along the x-axis
                    drawContext.canvas.nativeCanvas.drawText(
                        displayDate,
                        x,
                        canvasHeight - 8.dp.toPx(),
                        textPaint
                    )

                    // Draw the exact minute reading above each node dot
                    drawContext.canvas.nativeCanvas.drawText(
                        "${item.averageGapMinutes}m",
                        x,
                        dailyY - 8.dp.toPx(),
                        textPaint
                    )
                } else {
                    // 2. Zoomed Out: De-clutter layout by hiding text and showing clean day indices
                    val minimalDateMarker = try {
                        val parts = item.dateString.split("-")
                        parts.getOrNull(2) ?: "" // Show just the day number (e.g., "16")
                    } catch (e: Exception) {
                        ""
                    }

                    // Only draw every second or third day number text label to prevent horizontal collisions
                    if (index % 3 == 0) {
                        drawContext.canvas.nativeCanvas.drawText(
                            minimalDateMarker,
                            x,
                            canvasHeight - 8.dp.toPx(),
                            textPaint
                        )
                    }

                    // (Note: The raw "${item.averageGapMinutes}m" text is omitted here to completely un-clutter the canvas background)
                }
            }

            // Draw Paths
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

            if (rollingPoints.size > 1) {
                for (i in 0 until rollingPoints.size - 1) {
                    drawLine(
                        color = lineColor.copy(alpha = 0.6f),
                        start = rollingPoints[i],
                        end = rollingPoints[i + 1],
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            intervals = floatArrayOf(10f, 10f),
                            phase = 0f
                        )
                    )
                }
            }

            dailyPoints.forEach { offset ->
                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = offset)
                drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = offset)
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
