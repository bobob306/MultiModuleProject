package com.bsdevs.coffeescreen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.coffeescreen.viewdata.CoffeeScreenViewData
import com.bsdevs.coffeescreen.viewdata.InputViewData
import com.bsdevs.coffeescreen.viewdata.RadioInputsViewData
import com.bsdevs.common.result.Result
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.text.substringAfterLast

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CoffeeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    viewModel: CoffeeScreenViewModel = hiltViewModel(),
) {
    val viewData = viewModel.viewData.collectAsStateWithLifecycle()
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (viewData.value) {
            is Result.Loading -> LoadingScreen()
            is Result.Error -> ErrorScreen()
            is Result.Success -> CoffeeScreenContent(
                onShowSnackBar = onShowSnackBar,
                viewData = (viewData.value as Result.Success<CoffeeScreenViewData>).data,
                onToggleCoffeeTypeSelected = viewModel::onToggleCoffeeTypeSelected,
                onUpdateRoastDate = viewModel::onUpdateRoastData,
                onToggleCoffeeOriginSelected = viewModel::onToggleCoffeeOriginSelected,
                onToggleDecaf = viewModel::onToggleDecaf,
                onToggleCoffeeTasteSelected = viewModel::onToggleCoffeeTasteSelected,
                onCoffeeTasteType = viewModel::onCoffeeTasteType,
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Text("Loading")
}

@Composable
private fun ErrorScreen() {
    Text("Error")
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CoffeeScreenContent(
    onShowSnackBar: suspend (String, String?) -> Unit,
    viewData: CoffeeScreenViewData,
    onToggleCoffeeTypeSelected: (String) -> Unit,
    onUpdateRoastDate: (LocalDate) -> Unit,
    onToggleCoffeeOriginSelected: (String) -> Unit,
    onToggleDecaf: (Boolean) -> Unit,
    onToggleCoffeeTasteSelected: (String) -> Unit,
    onCoffeeTasteType: (String) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text("Coffee Screen")
        Spacer(modifier = Modifier.height(16.dp))
        CoffeeBeanSelection(
            viewData.coffeeTypes,
            viewData.selectedCoffeeTypes,
            onToggleCoffeeTypeSelected
        )
        Spacer(modifier = Modifier.height(16.dp))
        DatePickerSection(viewData.roastDate, onUpdateRoastDate)
        Spacer(modifier = Modifier.height(16.dp))
        CoffeeOriginSelection(
            viewData.originCountryOptions,
            viewData.selectedOriginCountries,
            onToggleCoffeeOriginSelected
        )
        Spacer(modifier = Modifier.height(16.dp))
        RadioInputRow(viewData.decafInput, onToggleDecaf = onToggleDecaf)
        Spacer(modifier = Modifier.height(16.dp))
//        CoffeeTasteSelection(
//            viewData.coffeeTastingNotes,
//            viewData.selectedTastingNotes,
//            onToggleCoffeeTasteSelected,
//        )
        InputSection(
            viewData.coffeeTastingNotesInput,
            onToggleCoffeeTasteSelected,
            onCoffeeTasteType,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputSection(
    coffeeTastingNotesInput: InputViewData,
    onToggleCoffeeTasteSelected: (String) -> Unit,
    onCoffeeTasteType: (String) -> Unit,
) {
    coffeeTastingNotesInput.run {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight()
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                    .fillMaxWidth(),
                readOnly = false,
                value = selectedSet.joinToString(", ") + " ${ searchText }",
                onValueChange = {
                    val searchText = it.substringAfterLast(" ")
                    onCoffeeTasteType(searchText)
                    expanded = true
                    TextRange(it.length)
                },
                label = { Text(coffeeTastingNotesInput.label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                Column {
                    val filteredList = if (!searchText.isNullOrEmpty()) {
                        inputList.filter { it.contains(searchText) }
                    } else inputList
                    filteredList.forEach { option ->
                        val isSelected = selectedSet.contains(option)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(option)
                                    // Show a checkmark if the item is selected
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected"
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onToggleCoffeeTasteSelected(option)
                                onCoffeeTasteType("")
                                expanded = true
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioInputRow(
    decafInput: RadioInputsViewData,
    onToggleDecaf: (Boolean) -> Unit,
) {
    Column {
        Text(decafInput.label)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            decafInput.option.forEach { option ->
                Text(option.label)
                Spacer(modifier = Modifier.width(8.dp))
                RadioButton(
                    selected = if (decafInput.isDecaf == option.isDecaf) true else false,
                    onClick = {
                        onToggleDecaf(option.isDecaf)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSection(roastDate: LocalDate?, onUpdateRoastDate: (LocalDate) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    Button(onClick = { showDatePicker = true }) {
        roastDate?.let {
            Text("Update Roast Date from $it")
        } ?: Text("Select Roast Date")
    }
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false }, // Dismiss dialog on request
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDateMillis = datePickerState.selectedDateMillis
                        if (selectedDateMillis != null) {
                            // Convert milliseconds to LocalDate and update ViewModel
                            val selectedLocalDate = Instant.ofEpochMilli(selectedDateMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onUpdateRoastDate(selectedLocalDate)
                        }
                        showDatePicker = false // Dismiss dialog after confirming
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false // Dismiss dialog on cancel
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoffeeBeanSelection(
    coffeeTypes: List<String>,
    selectedCoffeeTypes: Set<String>,
    onToggleCoffeeTypeSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            // Display the selected items joined by a comma, or a default message
            value = if (selectedCoffeeTypes.isEmpty()) "Select Coffee Type(s)" else selectedCoffeeTypes.joinToString(
                ", "
            ),
            onValueChange = {},
            label = { Text("Selected Coffee Types") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Column {
                coffeeTypes.sortedBy { it }.forEach { coffeeType ->
                    val isSelected = selectedCoffeeTypes.contains(coffeeType)
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(coffeeType)
                                // Show a checkmark if the item is selected
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected"
                                    )
                                }
                            }
                        },
                        onClick = {
                            onToggleCoffeeTypeSelected(coffeeType)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectFilterDropdown(
    label: String,
    originCountries: List<String>,
    selectedCountries: Set<String>,
    onToggleCoffeeOriginSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf("") }

    val filteredOptions = remember(originCountries, textFieldValue) {
        originCountries.filter { option ->
            option.contains(textFieldValue, ignoreCase = true)
        }.sortedBy { it }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = false,
            value = selectedCountries.joinToString(", "),
            onValueChange = {
                textFieldValue = it
                expanded = true // Expand on typing
            },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LazyColumn {
                items(filteredOptions) { option ->
                    val isSelected = selectedCountries.contains(option)
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(option)
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected"
                                    )
                                }
                            }
                        },
                        onClick = {
                            onToggleCoffeeOriginSelected(option)
                            // Keep the menu open for multi-selection
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoffeeOriginSelection(
    originCountries: List<String>,
    selectedOriginCountries: Set<String>,
    onToggleCoffeeOriginSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            // Display the selected items joined by a comma, or a default message
            value = if (selectedOriginCountries.isEmpty()) "Select Coffee Origin(s)" else selectedOriginCountries.joinToString(
                ", "
            ),
            onValueChange = {},
            label = { Text("Selected Coffee Origins") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Column {
                originCountries.sortedBy { it }.forEach { originCountry ->
                    val isSelected = selectedOriginCountries.contains(originCountry)
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(originCountry)
                                // Show a checkmark if the item is selected
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected"
                                    )
                                }
                            }
                        },
                        onClick = {
                            onToggleCoffeeOriginSelected(originCountry)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoffeeTasteSelection(
    tasteList: List<String>,
    selectedTastes: Set<String>,
    onToggleCoffeeTasteSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            // Display the selected items joined by a comma, or a default message
            value = if (selectedTastes.isEmpty()) "Select Coffee Taste(s)" else selectedTastes.joinToString(
                ", "
            ),
            onValueChange = {},
            label = { Text("Selected Coffee Tastes") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Column {
                tasteList.sortedBy { it }.forEach { taste ->
                    val isSelected = selectedTastes.contains(taste)
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(taste)
                                // Show a checkmark if the item is selected
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected"
                                    )
                                }
                            }
                        },
                        onClick = {
                            onToggleCoffeeTasteSelected(taste)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}
