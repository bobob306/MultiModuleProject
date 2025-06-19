package com.bsdevs.uicomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HorizontalWheelPicker(
    modifier: Modifier = Modifier,
    wheelPickerWidth: Dp? = null,
    startNumber: Int,
    endNumber: Int,
    initialSelectedItem: Int,
    lineWidth: Dp = 2.dp,
    selectedLineHeight: Dp = 64.dp,
    multipleOfFiveLineHeight: Dp = 40.dp,
    normalLineHeight: Dp = 30.dp,
    selectedMultipleOfFiveLinePaddingBottom: Dp = 0.dp,
    normalMultipleOfFiveLinePaddingBottom: Dp = 6.dp,
    normalLinePaddingBottom: Dp = 8.dp,
    lineSpacing: Dp = 8.dp,
    lineRoundedCorners: Dp = 2.dp,
    selectedLineColor: Color = Color(0xFF00D1FF),
    unselectedLineColor: Color = Color.LightGray,
    fadeOutLinesCount: Int = 4,
    maxFadeTransparency: Float = 0.7f,
    onItemSelected: (Int) -> Unit
) {
    val screenWidthDp = LocalContext.current.resources.displayMetrics.run {
        widthPixels / density
    }.dp
    val totalItems = endNumber - startNumber
    val effectiveWidth = wheelPickerWidth ?: screenWidthDp
    // Adjust initialSelectedItem to be 0-based index for LazyListState
    val initialScrollIndex = (initialSelectedItem - startNumber)
    // Correctly initialize scrollState with the 0-based index
    var currentSelectedItem by remember { mutableIntStateOf(initialSelectedItem) }
    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)
    val visibleItemsInfo by remember { derivedStateOf { scrollState.layoutInfo.visibleItemsInfo } }
    val firstVisibleItemIndex = visibleItemsInfo.firstOrNull()?.index ?: -1
    val lastVisibleItemIndex = visibleItemsInfo.lastOrNull()?.index ?: -1
    val totalVisibleItems = lastVisibleItemIndex - firstVisibleItemIndex + 1
    val middleIndex = firstVisibleItemIndex + totalVisibleItems / 2
    val bufferIndices = totalVisibleItems / 2

    LaunchedEffect(initialScrollIndex, currentSelectedItem) {
        onItemSelected(currentSelectedItem)
    }

    LazyRow(
        modifier = modifier.width(effectiveWidth),
        state = scrollState,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(totalItems + totalVisibleItems) { index ->
            // Calculate the actual number value based on the index and startNumber
            val actualNumber = index - bufferIndices + startNumber

            if (index == middleIndex) {
                currentSelectedItem = actualNumber
            }

            val lineHeight = when {
                index == middleIndex -> selectedLineHeight
                actualNumber % 5 == 0 -> multipleOfFiveLineHeight
                else -> normalLineHeight
            }

            val paddingBottom = when {
                index == middleIndex -> selectedMultipleOfFiveLinePaddingBottom
                actualNumber % 5 == 0 -> normalMultipleOfFiveLinePaddingBottom
                else -> normalLinePaddingBottom
            }

            val lineTransparency = calculateLineTransparency(
                index,
                totalItems,
                bufferIndices, // This buffer is for visual spacing, not data indexing
                firstVisibleItemIndex,
                lastVisibleItemIndex,
                fadeOutLinesCount,
                maxFadeTransparency
            )

            VerticalLine(
                lineWidth = lineWidth,
                lineHeight = lineHeight,
                paddingBottom = paddingBottom,
                roundedCorners = lineRoundedCorners,
                indexAtCenter = index == middleIndex,
                lineTransparency = lineTransparency,
                selectedLineColor = selectedLineColor,
                unselectedLineColor = unselectedLineColor
            )

            Spacer(modifier = Modifier.width(lineSpacing))
        }
    }
}

/**
 * A composable function that renders a single vertical line in the `WheelPicker`.
 *
 * The `VerticalLine` component is used within the `WheelPicker` to represent each
 * selectable item as a vertical line. The line's appearance can be customized with
 * different heights, padding, rounded corners, colors, and transparency effects.
 *
 * @param lineWidth The width of the vertical line.
 * @param lineHeight The height of the vertical line.
 * @param paddingBottom The padding applied to the bottom of the line.
 * @param roundedCorners The corner radius applied to the line, creating rounded corners.
 * @param indexAtCenter A boolean flag indicating if the line is at the center (selected item).
 * @param lineTransparency The transparency level applied to the line.
 * @param selectedLineColor The color of the line if it is the selected item.
 * @param unselectedLineColor The color of the line if it is not the selected item.
 *
 */
@Composable
private fun VerticalLine(
    lineWidth: Dp,
    lineHeight: Dp,
    paddingBottom: Dp,
    roundedCorners: Dp,
    indexAtCenter: Boolean,
    lineTransparency: Float,
    selectedLineColor: Color,
    unselectedLineColor: Color
) {
    Box(
        modifier = Modifier
            .width(lineWidth)
            .height(lineHeight)
            .clip(RoundedCornerShape(roundedCorners))
            .alpha(lineTransparency)
            .background(if (indexAtCenter) selectedLineColor else unselectedLineColor)
            .padding(bottom = paddingBottom)
    )
}

/**
 * Calculates the transparency level for a line based on its position within the `WheelPicker`.
 *
 * This function determines the transparency level for a line in the `WheelPicker` based on
 * its index and its position relative to the visible items in the list. The transparency
 * gradually increases towards the edges of the picker, creating a fade-out effect.
 *
 * @param lineIndex The index of the current line being rendered.
 * @param totalLines The total number of lines in the picker.
 * @param bufferIndices The number of extra indices used for rendering outside the visible area.
 * @param firstVisibleItemIndex The index of the first visible item in the list.
 * @param lastVisibleItemIndex The index of the last visible item in the list.
 * @param fadeOutLinesCount The number of lines that should gradually fade out at the edges.
 * @param maxFadeTransparency The maximum transparency level to apply during the fade-out effect.
 * @return A `Float` value representing the calculated transparency level for the line.
 *
 */
private fun calculateLineTransparency(
    lineIndex: Int,
    totalLines: Int,
    bufferIndices: Int,
    firstVisibleItemIndex: Int,
    lastVisibleItemIndex: Int,
    fadeOutLinesCount: Int,
    maxFadeTransparency: Float
): Float {
    val actualCount = fadeOutLinesCount + 1
    val transparencyStep = maxFadeTransparency / actualCount

    return when {
        lineIndex < bufferIndices || lineIndex > (totalLines + bufferIndices) -> 0.0f
        lineIndex in firstVisibleItemIndex until firstVisibleItemIndex + fadeOutLinesCount -> {
            transparencyStep * (lineIndex - firstVisibleItemIndex + 1)
        }

        lineIndex in (lastVisibleItemIndex - fadeOutLinesCount + 1)..lastVisibleItemIndex -> {
            transparencyStep * (lastVisibleItemIndex - lineIndex + 1)
        }

        else -> 1.0f
    }
}

@Preview(showBackground = true)
@Composable
private fun VerticalLinePreview() {
    MaterialTheme {
        Column(
            Modifier.wrapContentSize(), Arrangement.Center, Alignment.CenterHorizontally
        ) {
            var selectedItem by remember { mutableIntStateOf(27) }
            Text(text = selectedItem.toString(), fontSize = 46.sp)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalWheelPicker(
                startNumber = 20,
                endNumber = 60,
                initialSelectedItem = selectedItem,
                onItemSelected = { item ->
                    selectedItem = item
                }
            )
        }
    }
}