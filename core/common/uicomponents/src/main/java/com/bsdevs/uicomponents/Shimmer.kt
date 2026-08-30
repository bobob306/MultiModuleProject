package com.bsdevs.uicomponents

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    
    // Animate progress from 0 to 1 instead of pixel values
    // This allows us to use the actual size in the draw block without triggering re-composition
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    this.graphicsLayer {
        // Offload to hardware layer to solve jank and isolate invalidation
        clip = true
    }.drawBehind {
        val width = size.width
        val height = size.height
        
        // Calculate positions based on the progress and actual size
        val xOffset = progress * (width * 3) - width
        
        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(xOffset, 0f),
            end = Offset(xOffset + width, height)
        )
        
        drawRect(brush = brush)
    }
}
