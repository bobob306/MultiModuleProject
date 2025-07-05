package com.bsdevs.coffeescreen.screens.detailscreen.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.bsdevs.coffeescreen.screens.detailscreen.ShotDto

@Composable
internal fun SecondHalfContent(onAddShotClicked: () -> Unit, shotList: List<ShotDto>?) {

    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Shot recordings",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            shotList?.let {
                val scrollState = rememberScrollState()
                val isScrollable by remember(scrollState.maxValue) {
                    derivedStateOf {
                        scrollState.maxValue > 0
                    }
                }

                Box(
                    contentAlignment = Alignment.CenterEnd,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(
                            horizontal = 4.dp,
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .verticalScroll(scrollState)
                            .padding(end = if (isScrollable) 16.dp else 0.dp),
                    )
                    {
                        it.forEach { shot ->
                            ShotCard(shot = shot)
                        }
                    }
                    if (isScrollable) {
                        ScrollBar(scrollState)
                    }
                }
            }
        }
        IconButton(
            modifier = Modifier
                .padding(bottom = 4.dp, end = 4.dp)
                .align(Alignment.BottomEnd),
            onClick = { onAddShotClicked() }) {
            Icon(
                Icons.Default.AddCircle, contentDescription = "Add New Shot", Modifier.size(32.dp)
            )
        }
    }
}

@Composable
internal fun ScrollBar(scrollState: ScrollState) {
    val scrollProgressFromTop by remember(scrollState.value, scrollState.maxValue) {
        derivedStateOf {
            if (scrollState.maxValue > 0) {
                scrollState.value.toFloat() / scrollState.maxValue.toFloat()
            } else {
                0f
            }
        }
    }
    val density = LocalDensity.current
    val visiblePortionFraction by remember(
        scrollState.maxValue,
        scrollState.viewportSize
    ) {
        derivedStateOf {
            if (scrollState.maxValue > 0 && scrollState.viewportSize > 0) {
                val viewportSize = scrollState.viewportSize.toFloat()
                val totalContentHeight = viewportSize + scrollState.maxValue.toFloat()
                (viewportSize / totalContentHeight).coerceIn(
                    0.05f,
                    1f
                ) // Ensure min 5% height for thumb
            } else {
                1f // If not scrollable or viewport not determined, thumb is full height (won't be shown anyway)
            }
        }
    }
    Box( // Container for the scrollbar
        contentAlignment = Alignment.CenterEnd,
        modifier = Modifier
            .fillMaxHeight()
            .padding(
                8.dp,
            ) // Align with content padding
    ) {
        val scrollbarWidth = 8.dp
        val minThumbVisualHeightDp =
            20.dp // Minimum visible height for the thumb in Dp

        BoxWithConstraints { // Use BoxWithConstraints to get the actual track height
            val trackActualHeightDp = this.maxHeight
            val trackActualHeightPx =
                with(density) { trackActualHeightDp.toPx() }

            // Calculate thumb height in Dp, ensuring it's not smaller than minThumbVisualHeightDp
            val thumbHeightDp = (trackActualHeightDp * visiblePortionFraction)
                .coerceAtLeast(minThumbVisualHeightDp)
            val thumbHeightPx = with(density) { thumbHeightDp.toPx() }


            // Calculate the total movable range for the top of the thumb
            val movableRangePx = trackActualHeightPx - thumbHeightPx

            // Calculate the thumb's Y offset based on how much is scrolled from the top
            val thumbOffsetYPx =
                (movableRangePx * scrollProgressFromTop).coerceAtLeast(0f)
            val thumbOffsetYDp = with(density) { thumbOffsetYPx.toDp() }


            // Track
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(scrollbarWidth)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clip(RoundedCornerShape(4.dp)) // Clip background
            ) {
                // Thumb
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart) // Thumb starts at top of its calculated offset
                        .width(scrollbarWidth)
                        .height(thumbHeightDp) // Use calculated thumb height
                        .offset(y = thumbOffsetYDp) // Apply calculated offset
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clip(RoundedCornerShape(4.dp)) // Clip thumb itself
                )
            }
        }
    }
}

@Composable
internal fun ShotCard(shot: ShotDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${shot.date}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.wrapContentWidth()
                )
                shot.rating?.let {
                    Row {
                        Text(
                            text = "${shot.rating}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Rating",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Weight In: ${shot.weightIn} g",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Weight Out: ${shot.weightOut} g",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Time: ${shot.time} s",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}