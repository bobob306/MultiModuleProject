package com.bsdevs.uicomponents.animation

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// Represents a group of flying emojis bursting from a single impact point
data class ScatteredTurdSplodge(
    val centerPositionPercent: Offset,
    val maxScale: Float,
    val items: List<TurdItem>
)

// Represents an individual emoji particle inside a cluster
data class TurdItem(
    val angle: Double,
    val maxDistance: Float,
    val startSizePx: Float,
    val rotationDirection: Float
)

@Composable
fun TurdSplodgeAnimation(
    onAnimationEnd: () -> Unit
) {
    // 1. Unified 1.5-second animation progress timeline
    val progress = remember { Animatable(0f) }

    // 2. Generate 5 distinct burst points using pure Math.random() expressions
    val clusters = remember {
        List(5) {
            // Safe float calculations ranging across different screen boundaries
            val randomXPercent = 0.15f + (Math.random().toFloat() * (0.85f - 0.15f))
            val randomYPercent = 0.20f + (Math.random().toFloat() * (0.80f - 0.20f))

            // Generate a random particle count between 8 and 12 safely
            val emojiCount = 8 + (Math.random() * 5).toInt()
            val maxScaleFactor = 1.0f + (Math.random().toFloat() * 0.6f)

            ScatteredTurdSplodge(
                centerPositionPercent = Offset(randomXPercent, randomYPercent),
                maxScale = maxScaleFactor,
                items = List(emojiCount) {
                    TurdItem(
                        angle = Math.random() * 2 * Math.PI,
                        // Random flight distance between 60px and 160px
                        maxDistance = 60f + (Math.random().toFloat() * 100f),
                        // Random starting font size pixels between 24px and 48px
                        startSizePx = 24f + (Math.random().toFloat() * 24f),
                        rotationDirection = if (Math.random() > 0.5) 1f else -1f
                    )
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
        )
        onAnimationEnd()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val currentProgress = progress.value

        // Emojis fade out completely near the end of their lifespan
        val alpha = ((1f - currentProgress) * 255).toInt().coerceIn(0, 255)

        // Native Android Paint configured to render emoji fonts and handle fading
        val emojiPaint = Paint().apply {
            textSize = 32.sp.toPx()
            isAntiAlias = true
            this.alpha = alpha
        }

        // Loop through each separate burst zone
        clusters.forEach { cluster ->
            val exactCenter = Offset(
                x = size.width * cluster.centerPositionPercent.x,
                y = size.height * cluster.centerPositionPercent.y
            )

            cluster.items.forEach { item ->
                // Smooth physical brake slowdown effect using sine wave mapping
                val easeOutProgress = sin(currentProgress * Math.PI / 2).toFloat()
                val currentDistance = item.maxDistance * easeOutProgress

                // Project flight vectors
                val targetX = exactCenter.x + (currentDistance * cos(item.angle)).toFloat()
                val targetY = exactCenter.y + (currentDistance * sin(item.angle)).toFloat()

                // Emojis shrink as they fly away
                val dynamicScale = (1f - currentProgress) * cluster.maxScale

                if (dynamicScale > 0.1f) {
                    emojiPaint.textSize = item.startSizePx * dynamicScale

                    // Rotate the emoji based on current progression splits
                    val currentRotation = currentProgress * 360f * item.rotationDirection

                    withTransform({
                        rotate(currentRotation, Offset(targetX, targetY))
                    }) {
                        // Draw the canonical poop emoji string onto the system canvas lip
                        drawContext.canvas.nativeCanvas.drawText(
                            "💩",
                            targetX - (emojiPaint.textSize / 2f),
                            targetY + (emojiPaint.textSize / 2f),
                            emojiPaint
                        )
                    }
                }
            }
        }
    }
}
