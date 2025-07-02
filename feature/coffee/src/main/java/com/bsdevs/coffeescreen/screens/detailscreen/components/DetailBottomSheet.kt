package com.bsdevs.coffeescreen.screens.detailscreen.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bsdevs.coffeescreen.screens.detailscreen.EspressoShotDetails
import com.bsdevs.uicomponents.StarRating
import com.bsdevs.uicomponents.WheelInput
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun SheetHeader() {
    Text(
        "Log Espresso Shot",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
internal fun EspressoShotInputSheetContent(
    initialDetails: EspressoShotDetails = EspressoShotDetails(),
    onSave: (EspressoShotDetails) -> Unit,
    onDismiss: () -> Unit
) {
    var timeInSeconds by remember { mutableIntStateOf(initialDetails.timeInSeconds) }
    var weightInGrams by remember { mutableIntStateOf(initialDetails.weightInGrams) }
    var weightOutGrams by remember { mutableIntStateOf(initialDetails.weightOutGrams) }
    var shotDate by remember { mutableStateOf(initialDetails.date) }
    var rating by remember { mutableIntStateOf(initialDetails.rating) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Main container for the sheet content
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp), // Vertical padding for the whole sheet
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // SheetHeader is always at the top and centered
        SheetHeader()

        Spacer(modifier = Modifier.height(16.dp)) // Space between header and the rest of the content

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp), // Horizontal padding for the content row
                verticalAlignment = Alignment.Top
            ) {
                // Input Fields Column
                Column(
                    modifier = Modifier
                        .weight(1f) // Takes available space
                        .padding(end = 8.dp), // Padding between inputs and buttons
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InputFields(
                        timeInSeconds = timeInSeconds,
                        onTimeChange = { timeInSeconds = it },
                        weightInGrams = weightInGrams,
                        onWeightInChange = { weightInGrams = it },
                        weightOutGrams = weightOutGrams,
                        onWeightOutChange = { weightOutGrams = it },
                        shotDate = shotDate,
                        onShotDateChange = { shotDate = it },
                        rating = rating,
                        onRatingChange = { rating = it }
                    )
                }

                // Action Buttons Column
                Column(
                    modifier = Modifier
                        .padding(start = 8.dp) // Padding between inputs and buttons
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ActionButtons(
                        onSave = {
                            onSave(
                                EspressoShotDetails(
                                    timeInSeconds = timeInSeconds,
                                    weightInGrams = weightInGrams,
                                    weightOutGrams = weightOutGrams,
                                    date = shotDate
                                )
                            )
                        },
                        onDismiss = onDismiss,
                        isLandscape = true
                    )
                }
            }
        } else { // Portrait Mode
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp) // Horizontal padding for the content column
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                InputFields(
                    timeInSeconds = timeInSeconds,
                    onTimeChange = { timeInSeconds = it },
                    weightInGrams = weightInGrams,
                    onWeightInChange = { weightInGrams = it },
                    weightOutGrams = weightOutGrams,
                    onWeightOutChange = { weightOutGrams = it },
                    shotDate = shotDate,
                    onShotDateChange = { shotDate = it },
                    rating = rating,
                    onRatingChange = { rating = it }
                )
                Spacer(modifier = Modifier.height(24.dp))
                ActionButtons(
                    onSave = {
                        onSave(
                            EspressoShotDetails(
                                timeInSeconds = timeInSeconds,
                                weightInGrams = weightInGrams,
                                weightOutGrams = weightOutGrams,
                                date = shotDate,
                                rating = rating,
                            )
                        )
                    },
                    onDismiss = onDismiss,
                    isLandscape = false
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    isLandscape: Boolean
) {
    if (isLandscape) {
        // Buttons stacked vertically in landscape for the side column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = spacedBy(8.dp)
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier.wrapContentWidth()
            ) { // Fill width of their column
                Text("Save")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.wrapContentWidth()) {
                Text("Cancel")
            }
        }
    } else {
        // Buttons with their original modifiers for portrait
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Shot")
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}

@Composable
private fun InputFields(
    timeInSeconds: Int,
    onTimeChange: (Int) -> Unit,
    weightInGrams: Int,
    onWeightInChange: (Int) -> Unit,
    weightOutGrams: Int,
    onWeightOutChange: (Int) -> Unit,
    shotDate: LocalDate,
    onShotDateChange: (LocalDate) -> Unit,
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    val decimalFormat = remember { DecimalFormat("#.#") }

    WheelInputRow(
        label = "Time (seconds)",
        initialNumber = timeInSeconds,
        rangeStart = 10,
        rangeEnd = 60,
        onValueChange = { onTimeChange.invoke(it) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    WheelInputRow(
        label = "Weight In (grams)",
        initialNumber = weightInGrams,
        rangeStart = 70,
        rangeEnd = 250,
        onValueChange = { onWeightInChange.invoke(it) },
        isDecimal = true,
    )
    Spacer(modifier = Modifier.height(16.dp))
    WheelInputRow(
        label = "Weight Out (grams)",
        initialNumber = weightOutGrams,
        rangeStart = 140,
        rangeEnd = 650,
        onValueChange = { onWeightOutChange.invoke(it) },
        isDecimal = true,
    )
    Spacer(modifier = Modifier.height(16.dp))
    DateInputRow(
        label = "Date",
        date = shotDate,
        onDecrement = { onShotDateChange(shotDate.minusDays(1)) },
        onIncrement = { onShotDateChange(shotDate.plusDays(1)) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    StarRating(initialRating = rating, onRatingChanged = { onRatingChange(it) })
}

@Composable
private fun WheelInputRow(
    label: String,
    initialNumber: Int,
    isDecimal: Boolean? = null,
    rangeStart: Int,
    rangeEnd: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        WheelInput(
            isDecimal = isDecimal ?: false,
            initialSelectedItem = initialNumber,
            startNumber = rangeStart,
            endNumber = rangeEnd,
            onItemSelected = onValueChange,
            label = label,
        )
    }
}

@Composable
private fun DateInputRow(
    label: String,
    date: LocalDate,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            modifier = modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Decrement $label")
            }
            Text(
                text = date.format(dateFormatter),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = onIncrement) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Increment $label")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWheelInputRow() {
    MaterialTheme() {
        Column {
            WheelInputRow(
                label = "Weight In (grams)",
                initialNumber = 175,
                rangeStart = 70,
                rangeEnd = 250,
                onValueChange = {},
                isDecimal = true,
            )
            WheelInputRow(
                label = "Weight Out (grams)",
                initialNumber = 360,
                rangeStart = 140,
                rangeEnd = 650,
                onValueChange = {},
                isDecimal = true,
            )
            WheelInputRow(
                label = "Time (seconds)",
                initialNumber = 25,
                rangeStart = 10,
                rangeEnd = 60,
                onValueChange = {},
            )
            DateInputRow(
                label = "Date",
                date = LocalDate.now(),
                onDecrement = {},
                onIncrement = {}
            )
        }
    }
}

// Unused components
@Composable
fun NumberInputRow(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    isDecimal: Boolean? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Decrement $label")
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 60.dp) // Ensure some space for the number
            )
            IconButton(onClick = onIncrement) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Increment $label")
            }
        }
    }
}