package com.bsdevs.uicomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat
import kotlin.math.pow

@Composable
fun HorizontalWheelPicker(
    startNumber: Int,
    endNumber: Int,
    initialSelectedItem: Int,
    modifier: Modifier = Modifier,
    lineThickness: Dp = 3.dp,
    focusedIndicatorHeight: Dp = 64.dp,
    ofFiveLineHeight: Dp = 40.dp,
    normalLineHeight: Dp = 30.dp,
    focusedPaddingBottom: Dp = 0.dp,
    unfocusedOfFivePaddingBottom: Dp = 6.dp,
    unfocusedLinePaddingBottom: Dp = 8.dp,
    interItemSpace: Dp = 8.dp,
    lineRoundedCorners: Dp = 2.dp,
    focusedLineColour: Color = MaterialTheme.colorScheme.primary,
    unfocusedLineColour: Color = MaterialTheme.colorScheme.outline,
    fadeOutCount: Int = 4,
    maxFadeAlpha: Float = 0.9f,
    onItemSelected: (Int) -> Unit,
) {
    val totalItems = (endNumber - startNumber)
    val initialScrollIndex = (initialSelectedItem - startNumber)
    
    var currentSelectedItem by remember { mutableIntStateOf(initialSelectedItem) }
    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)
    
    val visibleItemsInfo by remember { derivedStateOf { scrollState.layoutInfo.visibleItemsInfo } }
    val firstVisibleItemIndex = visibleItemsInfo.firstOrNull()?.index ?: -1
    val lastVisibleItemIndex = visibleItemsInfo.lastOrNull()?.index ?: -1
    
    val totalVisibleItems = if (firstVisibleItemIndex != -1) {
        lastVisibleItemIndex - firstVisibleItemIndex + 1
    } else 0
    
    val middleIndex = firstVisibleItemIndex + totalVisibleItems / 2
    val bufferedItemCount = totalVisibleItems / 2

    LaunchedEffect(currentSelectedItem) {
        onItemSelected(currentSelectedItem)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier.weight(0.1f),
            onClick = {
                if (currentSelectedItem > startNumber) {
                    currentSelectedItem--
                    val target = currentSelectedItem - startNumber
                    scrollState.requestScrollToItem(target)
                }
            }
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Decrease")
        }

        LazyRow(
            modifier = Modifier.weight(0.8f),
            state = scrollState,
            verticalAlignment = Alignment.CenterVertically,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = scrollState)
        ) {
            items(totalItems + totalVisibleItems) { index ->
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
                    index == middleIndex -> focusedPaddingBottom
                    actualNumber % 5 == 0 -> unfocusedOfFivePaddingBottom
                    else -> unfocusedLinePaddingBottom
                }

                val lineTransparency = calculateLineTransparency(
                    index,
                    totalItems,
                    bufferedItemCount,
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
            modifier = Modifier.weight(0.1f),
            onClick = {
                if (currentSelectedItem < endNumber) {
                    currentSelectedItem++
                    val target = currentSelectedItem - startNumber
                    scrollState.requestScrollToItem(target)
                }
            }
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Increase")
        }
    }
}

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
fun WheelInput(
    startNumber: Int,
    endNumber: Int,
    initialSelectedItem: Int,
    onItemSelected: (Int) -> Unit,
    label: String,
    decimalPlaces: Int = 0,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        var selectedItem by remember { mutableIntStateOf(initialSelectedItem) }
        
        val pattern = if (decimalPlaces > 0) "#." + "#".repeat(decimalPlaces) else "#"
        val df = remember(pattern) { DecimalFormat(pattern) }
        val divisor = 10.0.pow(decimalPlaces.toDouble())
        
        val displayText = if (decimalPlaces > 0) {
            df.format(selectedItem.toDouble() / divisor)
        } else selectedItem.toString()

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(text = displayText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        HorizontalWheelPicker(
            startNumber = startNumber,
            endNumber = endNumber,
            initialSelectedItem = selectedItem,
            onItemSelected = { item: Int ->
                selectedItem = item
                onItemSelected(item)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WheelInputPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WheelInput(
                label = "Temperature",
                decimalPlaces = 1,
                startNumber = 300,
                endNumber = 450,
                initialSelectedItem = 370,
                onItemSelected = {}
            )
            WheelInput(
                label = "Age",
                startNumber = 0,
                endNumber = 100,
                initialSelectedItem = 25,
                onItemSelected = {}
            )
        }
    }
}
