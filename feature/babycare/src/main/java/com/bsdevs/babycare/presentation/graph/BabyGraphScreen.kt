package com.bsdevs.babycare.presentation.graph

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.uicomponents.MMPScaffold
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BabyGraphRoute(
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
                        )
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
                                FeedingHourCanvas(
                                    hourlyCounts = uiState.hourlyCounts,
                                    barColor = MaterialTheme.colorScheme.primary,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.3f
                                            )
                                        )
                                        .padding(horizontal = 16.dp)
                                )

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
                                    DailyAverageGapCanvas(
                                        dailyGaps = uiState.dailyAverageGaps,
                                        lineColor = MaterialTheme.colorScheme.tertiary,
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(260.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.3f
                                                )
                                            )
                                            .padding(horizontal = 16.dp)
                                    )
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
    if (dailyGaps.isEmpty()) return

    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val sortedGaps = remember(dailyGaps) {
        dailyGaps.mapNotNull { gap ->
            try {
                gap to LocalDate.parse(gap.dateString, formatter)
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.second }
    }

    if (sortedGaps.isEmpty()) return

    val minDate = sortedGaps.first().second
    val maxDate = sortedGaps.last().second
    val totalDaysSpan = ChronoUnit.DAYS.between(minDate, maxDate).coerceAtLeast(1L)

    // 1. Establish trackable scaling variables
    var scaleFactor by rememberSaveable { mutableFloatStateOf(1.0f) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val minScale = 0.01f
    val maxScale = 30.0f

    val transformModifier = Modifier.pointerInput(Unit) {
        detectTransformGestures { _, _, zoom, _ ->
            scaleFactor = (scaleFactor * zoom).coerceIn(minScale, maxScale)
        }
    }

    // 2. Base layout static metrics
    val baseWidthPerDay = 48.dp
    val bottomLabelSpace = 32.dp
    val topPaddingSpace = 24.dp
    val leftAxisLabelSpace = 50.dp

    val rawValues = dailyGaps.flatMap {
        listOfNotNull(it.averageGapMinutes, it.rolling14DayAverageMinutes)
    }
    val rawMax = rawValues.maxOrNull() ?: 60
    val rawMin = rawValues.minOrNull() ?: 30

    val yAxisMin = (rawMin - 10).coerceAtLeast(0)
    val yAxisMax = rawMax + 10
    val yAxisRange = (yAxisMax - yAxisMin).coerceAtLeast(1)

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val hPadding = 32.dp
    val contentWidth = (baseWidthPerDay * totalDaysSpan.toFloat() * scaleFactor)
    val chartTotalWidth = maxOf(screenWidth - leftAxisLabelSpace, contentWidth + hPadding * 2)

    Row(
        modifier = modifier
            .fillMaxHeight()
            .then(transformModifier)
    ) {
        // 1. Fixed Y-Axis Labels
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(leftAxisLabelSpace)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            val chartHeight = size.height - bottomLabelSpace.toPx() - topPaddingSpace.toPx()
            val axisTextPaint = android.graphics.Paint().apply {
                this.color = labelColor.copy(alpha = 0.6f).toArgb()
                this.textSize = 9.sp.toPx()
                this.textAlign = android.graphics.Paint.Align.RIGHT
                this.isAntiAlias = true
            }

            val numYMarkers = 5
            for (i in 0 until numYMarkers) {
                val ratio = i.toFloat() / (numYMarkers - 1)
                val value = yAxisMin + (ratio * yAxisRange)
                val y = topPaddingSpace.toPx() + (chartHeight - (chartHeight * ratio))

                drawContext.canvas.nativeCanvas.drawText(
                    "${value}m",
                    size.width - 6.dp.toPx(),
                    y + 3.dp.toPx(),
                    axisTextPaint
                )
            }
        }

        // 2. Scrollable Chart Area
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .horizontalScroll(rememberScrollState())
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(chartTotalWidth)
                    .pointerInput(sortedGaps, scaleFactor) {
                        detectTapGestures { tapOffset ->
                            val startX = hPadding.toPx()
                            val chartHeight = size.height - bottomLabelSpace.toPx() - topPaddingSpace.toPx()
                            val availableWidth = size.width - hPadding.toPx() * 2
                            
                            var bestIndex: Int? = null
                            var minDistance = 32.dp.toPx()
                            
                            sortedGaps.forEachIndexed { index, (item, date) ->
                                val daysOffset = ChronoUnit.DAYS.between(minDate, date)
                                val xRatio = daysOffset.toFloat() / totalDaysSpan.toFloat()
                                val x = startX + (xRatio * availableWidth)
                                
                                val dailyRatio = (item.averageGapMinutes - yAxisMin).toFloat() / yAxisRange
                                val dailyY = topPaddingSpace.toPx() + (chartHeight - (chartHeight * dailyRatio))
                                
                                val dist = (tapOffset - Offset(x, dailyY)).getDistance()
                                if (dist < minDistance) {
                                    minDistance = dist
                                    bestIndex = index
                                }
                            }
                            selectedIndex = if (bestIndex == selectedIndex) null else bestIndex
                        }
                    }
            ) {
                val canvasHeight = size.height
                val canvasWidth = size.width
                val chartHeight = canvasHeight - bottomLabelSpace.toPx() - topPaddingSpace.toPx()
                val startX = hPadding.toPx()
                val availableWidth = canvasWidth - hPadding.toPx() * 2

                val textPaint = android.graphics.Paint().apply {
                    this.color = labelColor.toArgb()
                    this.textSize = 10.sp.toPx()
                    this.textAlign = android.graphics.Paint.Align.CENTER
                    this.isAntiAlias = true
                }

                // Draw horizontal grid lines
                val gridColor = lineColor.copy(alpha = 0.2f)
                val numYMarkers = 5
                for (i in 0 until numYMarkers) {
                    val ratio = i.toFloat() / (numYMarkers - 1)
                    val y = topPaddingSpace.toPx() + (chartHeight - (chartHeight * ratio))

                    drawLine(
                        color = gridColor.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Plot Graph lines and nodes
                val dailyPoints = mutableListOf<Offset>()
                val rollingPoints = mutableListOf<Offset>()

                sortedGaps.forEachIndexed { index, (item, date) ->
                    val daysOffset = ChronoUnit.DAYS.between(minDate, date)
                    val xRatio = daysOffset.toFloat() / totalDaysSpan.toFloat()
                    val x = startX + (xRatio * availableWidth)

                    val dailyRatio = (item.averageGapMinutes - yAxisMin).toFloat() / yAxisRange
                    val dailyY = topPaddingSpace.toPx() + (chartHeight - (chartHeight * dailyRatio))
                    dailyPoints.add(Offset(x, dailyY))

                    item.rolling14DayAverageMinutes?.let { rollingAvg ->
                        val rollingRatio = (rollingAvg - yAxisMin).toFloat() / yAxisRange
                        val rollingY = topPaddingSpace.toPx() + (chartHeight - (chartHeight * rollingRatio))
                        rollingPoints.add(Offset(x, rollingY))
                    }

                    // Date labels
                    val dateStr = try {
                        val parts = item.dateString.split("-")
                        if (parts.size >= 3) "${parts[2]}/${parts[1]}" else item.dateString
                    } catch (e: Exception) { "" }
                    
                    val shouldShowLabel = if (index == 0 || index == sortedGaps.size - 1) true
                    else if (sortedGaps.size < 15) index % 2 == 0
                    else scaleFactor > 1.5f && index % 2 == 0
                    
                    if (shouldShowLabel) {
                        drawContext.canvas.nativeCanvas.drawText(
                            dateStr,
                            x,
                            canvasHeight - 8.dp.toPx(),
                            textPaint
                        )
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
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                }

                dailyPoints.forEach { offset ->
                    drawCircle(color = lineColor, radius = 5.dp.toPx(), center = offset)
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = offset)
                }

                // Draw Tooltip
                selectedIndex?.let { index ->
                    if (index < dailyPoints.size) {
                        val offset = dailyPoints[index]
                        val item = sortedGaps[index].first
                        val text = "${item.averageGapMinutes}m (${item.dateString})"
                        
                        val tooltipPaint = android.graphics.Paint().apply {
                            this.color = android.graphics.Color.BLACK
                            this.alpha = (255 * 0.8f).toInt()
                            this.style = android.graphics.Paint.Style.FILL
                            this.isAntiAlias = true
                        }
                        
                        val textPaintTooltip = android.graphics.Paint().apply {
                            this.color = android.graphics.Color.WHITE
                            this.textSize = 12.sp.toPx()
                            this.textAlign = android.graphics.Paint.Align.CENTER
                            this.isFakeBoldText = true
                        }
                        
                        val textBounds = android.graphics.Rect()
                        textPaintTooltip.getTextBounds(text, 0, text.length, textBounds)
                        
                        val padding = 8.dp.toPx()
                        val tooltipWidth = textBounds.width() + padding * 2
                        val tooltipHeight = textBounds.height() + padding * 2
                        
                        val tooltipRect = android.graphics.RectF(
                            offset.x - tooltipWidth / 2,
                            offset.y - tooltipHeight - 12.dp.toPx(),
                            offset.x + tooltipWidth / 2,
                            offset.y - 12.dp.toPx()
                        )
                        
                        if (tooltipRect.left < 0) tooltipRect.offset(-tooltipRect.left, 0f)
                        else if (tooltipRect.right > canvasWidth) tooltipRect.offset(canvasWidth - tooltipRect.right, 0f)

                        drawContext.canvas.nativeCanvas.drawRoundRect(tooltipRect, 4.dp.toPx(), 4.dp.toPx(), tooltipPaint)
                        drawContext.canvas.nativeCanvas.drawText(
                            text,
                            tooltipRect.centerX(),
                            tooltipRect.centerY() + textBounds.height() / 2f,
                            textPaintTooltip
                        )
                        
                        drawLine(
                            color = labelColor.copy(alpha = 0.5f),
                            start = Offset(offset.x, topPaddingSpace.toPx()),
                            end = Offset(offset.x, canvasHeight - bottomLabelSpace.toPx()),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
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
    // Canvas layout dimensions
    val barWidth = 44.dp
    val barSpacing = 16.dp
    val bottomLabelSpace = 32.dp
    
    val totalWidth = (barWidth + barSpacing) * hourlyCounts.size

    // Find highest count to scale graph peaks proportionally
    val maxFeedCount = hourlyCounts.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Box(
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(totalWidth)
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
