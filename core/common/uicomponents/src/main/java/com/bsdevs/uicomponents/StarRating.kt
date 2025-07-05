package com.bsdevs.uicomponents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun StarRating(
    modifier: Modifier = Modifier,
    initialRating: Int = 0,
    starColor: Color = Color.Yellow,
    onRatingChanged: (Int) -> Unit = {}
) {
    var currentRating by remember { mutableStateOf(initialRating.coerceIn(0, 5)) }

    Row(modifier = modifier) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= currentRating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = if (i <= currentRating) "Filled Star $i" else "Empty Star $i",
                tint = if (i <= currentRating) starColor else Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.clickable {
                    currentRating = i
                    onRatingChanged(currentRating)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StarRatingPreview() {
    StarRating(initialRating = 0)
}
