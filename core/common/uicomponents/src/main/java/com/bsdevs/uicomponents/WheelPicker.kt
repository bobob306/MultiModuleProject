package com.bsdevs.uicomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

@Composable
fun HorizontalWheelPicker(
    isDecimal: Boolean,
    modifier: Modifier = Modifier,
    wheelPickerWidth: Dp? = null,
    startNumber: Int,
    endNumber: Int,
    initialSelectedItem: Int,
    lineWidth: Dp = 2.dp,
    selectedLineHeight: Dp = 64.dp,
    multipleOfOneLineHeight: Dp = 35.dp,
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
    onItemSelected: (Int) -> Unit,
) {
    val isDecimal = isDecimal
    val screenWidthDp = LocalContext.current.resources.displayMetrics.run {
        widthPixels / density
    }.dp
    val totalItems = (endNumber - startNumber)
    val effectiveWidth = wheelPickerWidth ?: screenWidthDp
    // Adjust initialSelectedItem to be 0-based index for LazyListState
    val initialScrollIndex = (initialSelectedItem - startNumber)
    // Correctly initialize scrollState with the 0-based index
    var currentSelectedItem by remember { mutableIntStateOf(initialSelectedItem) }
    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)
    val visibleItemsInfo by remember { derivedStateOf { scrollState.layoutInfo.visibleItemsInfo } }
    val firstVisibleItemIndex = visibleItemsInfo.firstOrNull()?.index ?: -1
    val lastVisibleItemIndex = visibleItemsInfo.lastOrNull()?.index ?: -1
    val totalVisibleItems =
        if (firstVisibleItemIndex != -1) lastVisibleItemIndex - firstVisibleItemIndex + 1 else 0
    val middleIndex = firstVisibleItemIndex + totalVisibleItems / 2
    val bufferIndices = totalVisibleItems / 2

    LaunchedEffect(initialScrollIndex, currentSelectedItem) {
        onItemSelected(currentSelectedItem)
    }
    Row(modifier = modifier.fillMaxWidth()) {
        IconButton(
            modifier = Modifier.weight(0.1f), onClick = {
                if (currentSelectedItem > startNumber) {
                    currentSelectedItem--
                    scrollState.requestScrollToItem(currentSelectedItem - startNumber)
                }
            }) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Decrease")
        }
        LazyRow(
            modifier = modifier.weight(0.8f),
            state = scrollState,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(totalItems + totalVisibleItems) { index ->
                // Calculate the actual number value based on the index and startNumber
                val actualNumberValue = startNumber + (index - bufferIndices)

                if (index == middleIndex) {
                    currentSelectedItem = actualNumberValue
                }

                // Check divisibility by 5 for the integer part of the number
                val lineHeight = when {
                    index == middleIndex -> selectedLineHeight
                    actualNumberValue % 10 == 0 -> multipleOfFiveLineHeight

                    actualNumberValue % 5 == 0 -> multipleOfOneLineHeight

                    else -> normalLineHeight
                }

                val paddingBottom = when {
                    index == middleIndex -> selectedMultipleOfFiveLinePaddingBottom
                    actualNumberValue % 10 == 0 -> normalMultipleOfFiveLinePaddingBottom

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
                    bottomPadding = paddingBottom,
                    cornerRadius = lineRoundedCorners,
                    isCentre = index == middleIndex,
                    transparency = lineTransparency,
                    focusedLineColour = selectedLineColor,
                    defaultLineColour = unselectedLineColor
                )

                Spacer(modifier = Modifier.width(lineSpacing))
            }
        }
        IconButton(
            modifier = Modifier.weight(0.1f), onClick = {
                if (currentSelectedItem < endNumber) {
                    currentSelectedItem++
                    scrollState.requestScrollToItem(currentSelectedItem - startNumber)
                }
            }) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Increase")
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
 * @param bottomPadding The padding applied to the bottom of the line.
 * @param cornerRadius The corner radius applied to the line, creating rounded corners.
 * @param isCentre A boolean flag indicating if the line is at the center (selected item).
 * @param transparency The transparency level applied to the line.
 * @param focusedLineColour The color of the line if it is the selected item.
 * @param defaultLineColour The color of the line if it is not the selected item.
 *
 */
@Composable
private fun VerticalLine(
    lineHeight: Dp,
    lineWidth: Dp,
    bottomPadding: Dp,
    cornerRadius: Dp,
    isCentre: Boolean,
    transparency: Float,
    focusedLineColour: Color,
    defaultLineColour: Color
) {
    Box(
        modifier = Modifier
            .width(lineWidth)
            .height(lineHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .alpha(transparency)
            .background(if (isCentre) focusedLineColour else defaultLineColour)
            .padding(bottom = bottomPadding)
    )
}

/**
 * Calculates the transparency level for a line based on its position within the `WheelPicker`.
 *
 * This function determines the transparency level for a line in the `WheelPicker` based on
 * its index and its position relative to the visible items in the list. The transparency
 * gradually increases towards the edges of the picker, creating a fade-out effect.
 *
 * @param index The index of the current line being rendered.
 * @param total The total number of lines in the picker.
 * @param bufferedItemCount The number of extra indices used for rendering outside the visible area.
 * @param firstVisibleIndex The index of the first visible item in the list.
 * @param lastVisibleIndex The index of the last visible item in the list.
 * @param fadeCount The number of lines that should gradually fade out at the edges.
 * @param fadeTransparency The maximum transparency level to apply during the fade-out effect.
 * @return A `Float` value representing the calculated transparency level for the line.
 *
 */

private fun calculateLineTransparency(
    index: Int,
    total: Int,
    bufferedItemCount: Int,
    firstVisibleIndex: Int,
    lastVisibleIndex: Int,
    fadeCount: Int,
    fadeTransparency: Float
): Float {
    val actualCount = fadeCount + 1
    val transparencyStep = fadeTransparency / actualCount

    return when {
        index < bufferedItemCount || index > (total + bufferedItemCount) -> 0.0f
        index in firstVisibleIndex until firstVisibleIndex + fadeCount -> {
            transparencyStep * (index - firstVisibleIndex + 1)
        }

        index in (lastVisibleIndex - fadeCount + 1)..lastVisibleIndex -> {
            transparencyStep * (lastVisibleIndex - index + 1)
        }

        else -> 1.0f
    }
}

@Composable
fun HorizontalWheelPicker(
    modifier: Modifier = Modifier,
    pickerWidth: Dp? = null,
    startNumber: Int,
    endNumber: Int,
    initialSelectedItem: Int,
    lineThickness: Dp = 2.dp,
    focusedIndicatorHeight: Dp = 64.dp,
    ofFiveLineHeight: Dp = 40.dp,
    normalLineHeight: Dp = 30.dp,
    focusedMultipleOfFiveLinePaddingBottom: Dp = 0.dp,
    unfocusedMultipleOfFiveLinePaddingBottom: Dp = 6.dp,
    unfocusedLinePaddingBottom: Dp = 8.dp,
    interItemSpace: Dp = 8.dp,
    lineRoundedCorners: Dp = 2.dp,
    focusedLineColour: Color = Color(0xFF00D1FF),
    unfocusedLineColour: Color = Color.LightGray,
    fadeOutCount: Int = 4,
    maxFadeAlpha: Float = 0.7f,
    onItemSelected: (Int) -> Unit
) {
    val screenWidthDp = LocalContext.current.resources.displayMetrics.run {
        widthPixels / density
    }.dp
    val totalItems = endNumber - startNumber
    val effectiveWidth = pickerWidth ?: screenWidthDp
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
    val bufferedItemCount = totalVisibleItems / 2

    LaunchedEffect(initialScrollIndex, currentSelectedItem) {
        onItemSelected(currentSelectedItem)
    }

    Row(modifier = modifier.fillMaxWidth()) {
        IconButton(
            modifier = Modifier.weight(0.1f), onClick = {
                if (currentSelectedItem > startNumber) {
                    currentSelectedItem--
                    scrollState.requestScrollToItem(currentSelectedItem - startNumber)
                }
            }) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Decrease")
        }
        LazyRow(
            modifier = modifier.weight(0.8f),
            state = scrollState,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(totalItems + totalVisibleItems) { index ->
                // Calculate the actual number value based on the index and startNumber
                val actualNumber = index - bufferedItemCount + startNumber

                if (index == middleIndex) {
                    currentSelectedItem = actualNumber
                }

                val lineHeight = when {
                    index == middleIndex -> focusedIndicatorHeight
                    actualNumber % 5 == 0 -> ofFiveLineHeight
                    else -> normalLineHeight
                }

                val paddingBottom = when {
                    index == middleIndex -> focusedMultipleOfFiveLinePaddingBottom
                    actualNumber % 5 == 0 -> unfocusedMultipleOfFiveLinePaddingBottom
                    else -> unfocusedLinePaddingBottom
                }

                val lineTransparency = calculateLineTransparency(
                    index,
                    totalItems,
                    bufferedItemCount, // This buffer is for visual spacing, not data indexing
                    firstVisibleItemIndex,
                    lastVisibleItemIndex,
                    fadeOutCount,
                    maxFadeAlpha
                )

                VerticalLine(
                    lineWidth = lineThickness,
                    lineHeight = lineHeight,
                    bottomPadding = paddingBottom,
                    cornerRadius = lineRoundedCorners,
                    isCentre = index == middleIndex,
                    transparency = lineTransparency,
                    focusedLineColour = focusedLineColour,
                    defaultLineColour = unfocusedLineColour
                )

                Spacer(modifier = Modifier.width(interItemSpace))
            }
        }
        IconButton(
            modifier = Modifier.weight(0.1f), onClick = {
                if (currentSelectedItem < endNumber) {
                    currentSelectedItem++
                    scrollState.requestScrollToItem(currentSelectedItem - startNumber)
                }
            }) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Increase")
        }
    }
}

@Composable
fun WheelInput(
    isDecimal: Boolean?,
    startNumber: Int,
    endNumber: Int,
    initialSelectedItem: Int,
    onItemSelected: (Int) -> Unit,
    label: String,
) {
    Column(Modifier
        .wrapContentHeight()
        .wrapContentWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        var selectedItem by remember { mutableIntStateOf(initialSelectedItem) }
        val df = DecimalFormat("#.#")
        val text = if (isDecimal == true) {
            df.format(selectedItem.toDouble() / 10.0)
        } else selectedItem.toString()
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, modifier = Modifier.padding(end = 8.dp))
            Text(text = text)
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalWheelPicker(
            startNumber = startNumber,
            endNumber = endNumber,
            initialSelectedItem = selectedItem,
            onItemSelected = { item: Int ->
                selectedItem = item
                onItemSelected(item)
            },
            isDecimal = isDecimal ?: false
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VerticalLinePreview() {
    MaterialTheme {
        Column(
            Modifier.wrapContentSize(), Arrangement.Center, Alignment.CenterHorizontally
        ) {
            var selectedItem by remember { mutableIntStateOf(330) }
            val df = DecimalFormat("#.#")
            Text(text = df.format(selectedItem.toDouble() / 10.0), fontSize = 46.sp)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalWheelPicker(
                isDecimal = true,
                startNumber = 300,
                endNumber = 400,
                initialSelectedItem = selectedItem,
                onItemSelected = { item: Int ->
                    selectedItem = item
                })
            var selectedItem2 by remember { mutableIntStateOf(27) }
            Text(text = selectedItem2.toString(), fontSize = 46.sp)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalWheelPicker(
                startNumber = 20,
                endNumber = 60,
                initialSelectedItem = selectedItem2,
                onItemSelected = { item: Int ->
                    selectedItem2 = item
                })
        }
    }
}