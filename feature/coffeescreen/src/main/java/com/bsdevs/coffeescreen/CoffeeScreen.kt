package com.bsdevs.coffeescreen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.coffeescreen.viewdata.CoffeeScreenViewData
import com.bsdevs.coffeescreen.viewdata.InputType
import com.bsdevs.coffeescreen.viewdata.InputViewData
import com.bsdevs.common.result.Result
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
                onCoffeeTasteSearchText = viewModel::onCoffeeTasteType,
                onToggleProcessSelected = viewModel::onToggleProcessSelected,
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
    onCoffeeTasteSearchText: (String) -> Unit,
    onToggleProcessSelected: (String) -> Unit,
) {
    var showSnackBar by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = showSnackBar) {
        if (showSnackBar) {
            var inputsContent = viewData.inputs.joinToString{
                when (it) {
                    is InputViewData.InputVD -> {
                        (it.selectedSet.joinToString(", "))
                    }
                    is InputViewData.InputRadioVD -> {
                        (if (it.isDecaf) "Decaf" else "Caffeinated")
                    }
                }
            }
            val snackBarContent = "Coffee Details Entered" + "\n\n${inputsContent}" +
                    "\n\nRoast Date: ${viewData.roastDate}"
            onShowSnackBar.invoke(snackBarContent, null)
            showSnackBar = false
        }
    }
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        viewData.inputs.forEach { inputViewData ->
            when (inputViewData) {
                is InputViewData.InputVD -> {
                    when (inputViewData.inputType) {
                        InputType.BEANS -> {
                            InputSection(inputViewData, onToggleCoffeeTypeSelected) { }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        InputType.ORIGIN -> {
                            InputSection(inputViewData, onToggleCoffeeOriginSelected) { }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        InputType.TASTE -> {
                            InputSection(
                                inputViewData,
                                onToggleCoffeeTasteSelected
                            ) { onCoffeeTasteSearchText(it) }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        InputType.METHOD -> {
                            InputSection(inputViewData, onToggleProcessSelected) { }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                is InputViewData.InputRadioVD -> {
                    RadioInputRow(inputViewData, onToggleDecaf)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        DatePickerSection(viewData.roastDate, onUpdateRoastDate)
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { showSnackBar = true },
            enabled = viewData.isButtonEnabled,
            modifier = Modifier.wrapContentSize()
        )
        { Text("Enter coffee details", modifier = Modifier.wrapContentSize()) }
        Spacer(modifier = Modifier.weight(0.1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputSection(
    inputViewData: InputViewData.InputVD,
    onToggleSelected: (String) -> Unit,
    onSearchTextChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    inputViewData.run {
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
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused
                    }
                    .wrapContentHeight(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                readOnly = searchText?.let {
                    if (inputViewData.searchText == "" || !inputViewData.searchText?.isEmpty()!!) false else true
                } ?: true,
                value = if (expanded && isFocused) searchText
                    ?: "" else inputViewData.selectedSet.joinToString(", "),
                onValueChange = {
                    onSearchTextChange(it)
                    expanded = true
                },
                label = { Text(inputViewData.label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                suffix = {
                    if (isFocused && expanded)
                        Text(
                            selectedSet.joinToString(separator = ", "),
                            modifier = Modifier
                                .fillMaxWidth(if (expanded && searchText != null) 0.5f else 1f)
                                .padding(horizontal = if (!expanded || searchText == null) 16.dp else 0.dp)

                        )
                })
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                Column(modifier = Modifier.wrapContentHeight()) {
                    val filteredList = if (!searchText.isNullOrEmpty()) {
                        inputList.filter { it.contains(searchText, ignoreCase = true) }
                    } else inputList
                    filteredList.forEach { option ->
                        val isSelected = selectedSet.contains(option)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
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
                                onToggleSelected(option)
                                searchText?.let {
                                    onSearchTextChange("")
                                }
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
    decafInput: InputViewData.InputRadioVD,
    onToggleDecaf: (Boolean) -> Unit,
) {
    Column {
        Text(decafInput.label)
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            decafInput.option.forEach { option ->
                Text(option.label)
                Spacer(modifier = Modifier.width(8.dp))
                RadioButton(
                    selected = if (decafInput.isDecaf == option.isDecaf) true else false,
                    onClick = {
                        onToggleDecaf(option.isDecaf)
                    })
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
                                .atZone(ZoneId.systemDefault()).toLocalDate()
                            onUpdateRoastDate(selectedLocalDate)
                        }
                        showDatePicker = false // Dismiss dialog after confirming
                    }) {
                    Text("OK")
                }
            }, dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false // Dismiss dialog on cancel
                    }) {
                    Text("Cancel")
                }
            }) {
            DatePicker(state = datePickerState)
        }
    }
}