package com.bsdevs.coffeescreen.screens.detailscreen

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.inputscreen.ErrorScreen
import com.bsdevs.coffeescreen.screens.inputscreen.LoadingScreen
import com.bsdevs.common.result.Result
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun CoffeeDetailScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    navigateToCoffeeHome: (navOptions: NavOptions?) -> Unit,
    viewModel: CoffeeDetailsViewModel = hiltViewModel()
) {
    val viewData = viewModel.viewData.collectAsStateWithLifecycle()
    Surface(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (viewData.value) {
            is Result.Loading -> {
                LoadingScreen()
            }

            is Result.Error -> {
                ErrorScreen()
            }

            is Result.Success<CoffeeDetailsViewData> -> {
                CoffeeDetailContent(
                    onShowSnackBar = onShowSnackBar,
                    coffeeDetailsViewData = (viewData.value as Result.Success<CoffeeDetailsViewData>).data,
                    onIntent = viewModel::processIntent
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                NavigationEvent.NavigateHome -> navigateToCoffeeHome(null)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeeDetailContent(
    onShowSnackBar: suspend (String, String?) -> Unit,
    coffeeDetailsViewData: CoffeeDetailsViewData,
    onIntent: (CoffeeDetailsIntent) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // Good for input forms
    )
    var showSheet by remember { mutableStateOf(false) }
    var currentShotDetails by remember { mutableStateOf<EspressoShotDetails?>(null) }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            // You can customize windowInsets, dragHandle, etc.
        ) {
            EspressoShotInputSheetContent(
                onSave = { details ->
                    onIntent(CoffeeDetailsIntent.SubmitShot(details))
                    showSheet = false // Dismiss the sheet
                },
                onDismiss = {
                    showSheet = false
                }
            )
        }
    }

    val contentModifier = if (isLandscape) {
        Modifier
            // Add padding around the content area in landscape
            .fillMaxHeight() // Card column will fill height
            .fillMaxWidth(0.5f) // Card column takes left half
    } else {
        Modifier
            // Add padding around the content area in portrait
            .fillMaxWidth() // Card column takes full width
            .wrapContentHeight() // Height wraps content
    }
    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = spacedBy(16.dp)) {
            // Left half: Coffee Details Card
            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f), // This now applies .fillMaxWidth(0.5f) from above
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) { // Pass showSheet state and its updater
                CoffeeDetailsScrollableColumn(coffeeDetailsViewData.coffeeDto, isLandscape)
            }
            // Right half: Empty or for other content
            SecondHalfContent(
                onAddShotClicked = { showSheet = true },
                shotList = coffeeDetailsViewData.shotList
            )
        }
    } else {
        // Portrait mode: Card takes full width
        Column(
            modifier = Modifier.fillMaxSize(), // Box to center the card if it's not filling size
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(), // This applies .fillMaxWidth() from above
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                CoffeeDetailsScrollableColumn(coffeeDetailsViewData.coffeeDto, isLandscape)
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(), // This applies .fillMaxWidth() from above
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                SecondHalfContent(
                    onAddShotClicked = { showSheet = true },
                    shotList = coffeeDetailsViewData.shotList
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EspressoShotInputSheetContent(
    initialDetails: EspressoShotDetails = EspressoShotDetails(),
    onSave: (EspressoShotDetails) -> Unit,
    onDismiss: () -> Unit
) {
    var timeInSeconds by remember { mutableIntStateOf(initialDetails.timeInSeconds) }
    var weightInGrams by remember { mutableDoubleStateOf(initialDetails.weightInGrams) }
    var weightOutGrams by remember { mutableDoubleStateOf(initialDetails.weightOutGrams) }
    var shotDate by remember { mutableStateOf(initialDetails.date) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Main container for the sheet content
    Column(
        modifier = Modifier
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
                        onShotDateChange = { shotDate = it }
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
                    onShotDateChange = { shotDate = it }
                )
                Spacer(modifier = Modifier.height(24.dp))
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
                    isLandscape = false
                )
            }
        }
    }
}

@Composable
private fun SheetHeader() {
    Text(
        "Log Espresso Shot",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun InputFields(
    timeInSeconds: Int,
    onTimeChange: (Int) -> Unit,
    weightInGrams: Double,
    onWeightInChange: (Double) -> Unit,
    weightOutGrams: Double,
    onWeightOutChange: (Double) -> Unit,
    shotDate: LocalDate,
    onShotDateChange: (LocalDate) -> Unit
) {
    val decimalFormat = remember { DecimalFormat("#.#") }

    NumberInputRow(
        label = "Time (seconds)",
        value = timeInSeconds.toString(),
        onDecrement = { if (timeInSeconds > 0) onTimeChange(timeInSeconds - 1) },
        onIncrement = { onTimeChange(timeInSeconds + 1) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    NumberInputRow(
        label = "Weight In (grams)",
        value = decimalFormat.format(weightInGrams),
        onDecrement = { if (weightInGrams > 0.1) onWeightInChange((weightInGrams - 0.1).roundTo(1)) },
        onIncrement = { onWeightInChange((weightInGrams + 0.1).roundTo(1)) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    NumberInputRow(
        label = "Weight Out (grams)",
        value = decimalFormat.format(weightOutGrams),
        onDecrement = { if (weightOutGrams > 0.1) onWeightOutChange((weightOutGrams - 0.1).roundTo(1)) },
        onIncrement = { onWeightOutChange((weightOutGrams + 0.1).roundTo(1)) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    DateInputRow(
        label = "Date",
        date = shotDate,
        onDecrement = { onShotDateChange(shotDate.minusDays(1)) },
        onIncrement = { onShotDateChange(shotDate.plusDays(1)) }
    )
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
fun NumberInputRow(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateInputRow(
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
        Text(label, style = MaterialTheme.typography.bodyLarge)
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

// Helper function to round doubles to a specific number of decimal places
fun Double.roundTo(decimalPlaces: Int): Double {
    val factor = Math.pow(10.0, decimalPlaces.toDouble())
    return (this * factor).let {
        kotlin.math.round(it) / factor
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun SecondHalfContent(onAddShotClicked: () -> Unit, shotList: List<ShotDto>?) {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                "Shot recordings",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            shotList?.let {
                LazyVerticalGrid(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    columns = GridCells.Fixed(1)
                )
                {
                    items(count = it.size, itemContent = { index ->
                        ShotCard(shot = it[index])
                    }
                    )
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShotCard(shot: ShotDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "${shot.date}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.wrapContentWidth()
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
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

@Composable
private fun CoffeeDetailsScrollableColumn(coffeeDto: CoffeeDto, isLandscape: Boolean) {
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .padding(16.dp)
            .wrapContentHeight() // Ensure the height wraps content
        // Apply verticalScroll only to the Column, not the Card directly
        // to ensure the Card itself doesn't try to scroll if its content is fixed height.
        // Allow column to take available height within the Card
    ) {
        Text(
            text = coffeeDto.label ?: "Unnamed Coffee",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyVerticalGrid(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth(),
            columns = GridCells.Fixed(2)
        ) {
            item {
                coffeeDto.roastDate?.takeIf { it.isNotBlank() }?.let {
                    CoffeeDetailItem(label = "Roast Date", value = it)
                }
            }
            item {
                coffeeDto.roaster?.takeIf { it.isNotBlank() }?.let {
                    CoffeeDetailItem(label = "Roaster", value = it)
                }
            }
            item {
                coffeeDto.beanTypes?.takeIf { it.isNotEmpty() }?.let {
                    CoffeeDetailItem(label = "Bean Types", value = it.joinToString(", "))
                }
            }
            item {
                coffeeDto.originCountries?.takeIf { it.isNotEmpty() }?.let {
                    CoffeeDetailItem(label = "Origin Countries", value = it.joinToString(", "))
                }
            }
            item {
                coffeeDto.tastingNotes?.takeIf { it.isNotEmpty() }?.let {
                    CoffeeDetailItem(label = "Tasting Notes", value = it.joinToString(", "))
                }
            }
            item {
                coffeeDto.beanPreparationMethod?.takeIf { it.isNotEmpty() }?.let {
                    CoffeeDetailItem(label = "Preparation Method", value = it.joinToString(", "))
                }
            }
            item {
                coffeeDto.isDecaf?.let {
                    CoffeeDetailItem(label = "Decaf", value = if (it) "Yes" else "No")
                }
            }
        }
    }
}

@Composable
fun CoffeeDetailItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .padding(bottom = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, device = "spec:parent=pixel_2")
@Preview(showBackground = true, device = "spec:parent=pixel_2", fontScale = 2f)
@Preview(showBackground = true, device = "spec:parent=pixel_2,orientation=landscape")
@Composable
private fun CoffeeDetailContentPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            CoffeeDetailContent(
                onShowSnackBar = { _, _ -> },
                CoffeeDetailsViewData(
                    coffeeDto = CoffeeDto(
                        label = "Ethiopian Yirgacheffe",
                        roastDate = "2023-10-26",
                        roaster = "Artisan Coffee Roasters",
                        beanTypes = listOf("Arabica"),
                        originCountries = listOf("Ethiopia"),
                        tastingNotes = listOf("Floral", "Citrus", "Berry"),
                        beanPreparationMethod = listOf("Washed"),
                        isDecaf = false
                    ),
                    null
                ),
                onIntent = {}
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
data class EspressoShotDetails(
    val id: String = UUID.randomUUID().toString(),
    val timeInSeconds: Int = 27,
    val weightInGrams: Double = 17.5,
    val weightOutGrams: Double = 36.0,
    @SuppressLint("NewApi") val date: LocalDate = LocalDate.now()
)