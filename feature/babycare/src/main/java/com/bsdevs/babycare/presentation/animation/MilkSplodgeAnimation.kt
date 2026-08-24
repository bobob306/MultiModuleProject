package com.bsdevs.babycare.presentation.animation

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
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

// Represents an individual splodge group somewhere on the screen
data class ScatteredSplodge(
    val centerPositionPercent: Offset, // Relative X/Y (0f to 1f) so it fits any screen size
    val baseRadius: Float,
    val dropletCount: Int,
    val dropletList: List<RoundedDroplet>
)

// Represents the individual round beads flying out from an impact center
data class RoundedDroplet(
    val angle: Double,
    val maxDistance: Float,
    val radius: Float
)

@Composable
fun MilkSplodgeAnimation(
    onAnimationEnd: () -> Unit
) {
    // 1. Unified animation progress timeline (1.5 seconds)
    val progress = remember { Animatable(0f) }

    // 2. Generate 6 distinct splodge impact clusters all over the viewport space
    val splodgeClusters = remember {
        List(6) {
            // Pick a completely random position anywhere on the screen
            val randomXPercent = (10..90).random().toFloat() / 100f
            val randomYPercent = (15..85).random().toFloat() / 100f

            val maxDroplets = (12..18).random()

            ScatteredSplodge(
                centerPositionPercent = Offset(randomXPercent, randomYPercent),
                baseRadius = (35..75).random().toFloat(),
                dropletCount = maxDroplets,
                dropletList = List(maxDroplets) {
                    RoundedDroplet(
                        angle = Math.random() * 2 * Math.PI,
                        maxDistance = (40..130).random().toFloat(),
                        radius = (4..12).random().toFloat()
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
        val alpha = (1f - currentProgress).coerceAtLeast(0f)
        val milkColor = Color(0xFFFDFBF7).copy(alpha = alpha) // Milky fluid hue

        // Loop through each separate impact zone scattered on the canvas
        splodgeClusters.forEach { splodge ->
            // Convert relative percentages to absolute pixel offsets
            val exactCenter = Offset(
                x = size.width * splodge.centerPositionPercent.x,
                y = size.height * splodge.centerPositionPercent.y
            )

            // --- 3. DRAW THE ORGANIC ROUNDED MAIN BLOB ---
            if (currentProgress < 0.85f) {
                val liveRadius = splodge.baseRadius * (1f - currentProgress)

                // Draw a core primary circle
                drawCircle(
                    color = milkColor,
                    radius = liveRadius,
                    center = exactCenter
                )

                // Overlap three small secondary satellite circles slightly off-center
                // to make the base shape look organic and liquid instead of a perfect mathematical ring
                val shiftDistance = liveRadius * 0.3f
                drawCircle(color = milkColor, radius = liveRadius * 0.8f, center = exactCenter + Offset(shiftDistance, -shiftDistance))
                drawCircle(color = milkColor, radius = liveRadius * 0.7f, center = exactCenter + Offset(-shiftDistance, shiftDistance * 0.5f))
            }

            // --- 4. DRAW OUTWARD FLYING ROUND WATER-DROPLETS ---
            splodge.dropletList.forEach { droplet ->
                // Smooth physical brake slowdown effect using sine wave mapping
                val easeOutProgress = sin(currentProgress * Math.PI / 2).toFloat()
                val currentDistance = droplet.maxDistance * easeOutProgress

                // Project droplet location vectors
                val dropletX = exactCenter.x + (currentDistance * cos(droplet.angle)).toFloat()
                val dropletY = exactCenter.y + (currentDistance * sin(droplet.angle)).toFloat()

                // Smoothly taper droplet sizing to zero down the lifecycle pipeline
                val dynamicRadius = droplet.radius * (1f - currentProgress)

                if (dynamicRadius > 1f) {
                    drawCircle(
                        color = milkColor,
                        radius = dynamicRadius,
                        center = Offset(dropletX, dropletY)
                    )
                }
            }
        }
    }
}
