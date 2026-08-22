package com.bsdevs.babycare.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
                .padding(innerPadding)
                .padding(16.dp),
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
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (uiState.totalFeedsInCache == 0) {
                // Empty state handler
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
                // Scrollable container for the graph canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
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
