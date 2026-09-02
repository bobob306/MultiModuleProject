package com.bsdevs.forms.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bsdevs.data.FormFieldData
import com.bsdevs.uicomponents.MMPClickableTextField
import com.bsdevs.uicomponents.MMPDatePickerDialog
import com.bsdevs.uicomponents.MMPTimePickerDialog
import com.bsdevs.uicomponents.WheelInput
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role

@Composable
fun FormFieldItem(
    field: FormFieldData,
    value: Any?,
    fieldValues: Map<String, Any>,
    onFieldChanged: (String, Any) -> Unit,
) {
    val isVisible = field.showWhen?.let { condition ->
        fieldValues[condition.fieldKey] == condition.equals
    } ?: true
    if (!isVisible) return

    when (field) {
        is FormFieldData.TextInputData -> TextInputField(field, value as? String ?: "", onFieldChanged)
        is FormFieldData.NumberInputData -> NumberInputField(field, value as? String ?: "", onFieldChanged)
        is FormFieldData.SwitchFieldData -> SwitchField(field, value as? Boolean ?: field.default, onFieldChanged)
        is FormFieldData.RadioFieldData -> RadioField(field, value as? String, onFieldChanged)
        is FormFieldData.CheckboxListFieldData -> CheckboxListField(field, (value as? List<*>)?.filterIsInstance<String>() ?: emptyList(), onFieldChanged)
        is FormFieldData.DropdownFieldData -> DropdownField(field, value, onFieldChanged)
        is FormFieldData.DateInputData -> DateInputField(field, value as? String ?: "", onFieldChanged)
        is FormFieldData.TimeInputData -> TimeInputField(field, value as? String ?: "", onFieldChanged)
        is FormFieldData.WheelInputData -> WheelInputField(field, (value as? Number)?.toInt() ?: field.defaultValue, onFieldChanged)
        is FormFieldData.Unknown -> {}
    }
}

@Composable
private fun WheelInputField(
    field: FormFieldData.WheelInputData,
    value: Int,
    onFieldChanged: (String, Any) -> Unit,
) {
    WheelInput(
        decimalPlaces = field.decimalPlaces,
        startNumber = field.startNumber,
        endNumber = field.endNumber,
        initialSelectedItem = value,
        onItemSelected = { onFieldChanged(field.fieldKey, it) },
        label = field.label,
    )
}

@Composable
private fun TextInputField(
    field: FormFieldData.TextInputData,
    value: String,
    onFieldChanged: (String, Any) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onFieldChanged(field.fieldKey, it) },
        label = { Text(field.label) },
        placeholder = field.placeholder?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun NumberInputField(
    field: FormFieldData.NumberInputData,
    value: String,
    onFieldChanged: (String, Any) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onFieldChanged(field.fieldKey, it) },
        label = { Text(field.label) },
        placeholder = field.placeholder?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun SwitchField(
    field: FormFieldData.SwitchFieldData,
    checked: Boolean,
    onFieldChanged: (String, Any) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(field.label, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = { onFieldChanged(field.fieldKey, it) },
        )
    }
}

@Composable
private fun RadioField(
    field: FormFieldData.RadioFieldData,
    selectedOption: String?,
    onFieldChanged: (String, Any) -> Unit,
) {
    Column(Modifier.selectableGroup()) {
        Text(field.label, modifier = Modifier.padding(bottom = 4.dp))
        field.options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .selectable(
                        selected = option == selectedOption,
                        onClick = { onFieldChanged(field.fieldKey, option) },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = option == selectedOption,
                    onClick = { onFieldChanged(field.fieldKey, option) },
                )
                Text(option, modifier = Modifier.padding(start = 8.dp))
            }
            if (option != field.options.last()) HorizontalDivider()
        }
    }
}

@Composable
private fun CheckboxListField(
    field: FormFieldData.CheckboxListFieldData,
    selectedOptions: List<String>,
    onFieldChanged: (String, Any) -> Unit,
) {
    Column {
        Text(field.label, modifier = Modifier.padding(bottom = 4.dp))
        field.options.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = option in selectedOptions,
                    onCheckedChange = { checked ->
                        val updated = if (checked) selectedOptions + option else selectedOptions - option
                        onFieldChanged(field.fieldKey, updated)
                    },
                )
                Text(option, modifier = Modifier.padding(start = 8.dp))
            }
            if (option != field.options.last()) HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    field: FormFieldData.DropdownFieldData,
    value: Any?,
    onFieldChanged: (String, Any) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    val selectedSingle = value as? String ?: ""
    @Suppress("UNCHECKED_CAST")
    val selectedMulti = (value as? List<String>) ?: emptyList()

    val displayValue = when {
        expanded -> searchText
        field.multiSelect -> selectedMulti.joinToString()
        else -> selectedSingle
    }

    val filteredOptions = if (searchText.isBlank()) field.options
        else field.options.filter { it.contains(searchText, ignoreCase = true) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = if (field.editable && expanded) searchText else displayValue,
                onValueChange = { 
                    if (field.editable) {
                        searchText = it
                        onFieldChanged(field.fieldKey, it)
                    } else {
                        searchText = it
                    }
                    expanded = true 
                },
                label = { Text(field.label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                suffix = if (expanded && field.multiSelect && selectedMulti.isNotEmpty()) {
                    { Text(selectedMulti.joinToString(), modifier = Modifier.fillMaxWidth(0.5f)) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            )
            if (filteredOptions.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false; searchText = "" },
                ) {
                    filteredOptions.forEach { option ->
                        if (field.multiSelect) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = option in selectedMulti, onCheckedChange = null)
                                        Text(option, modifier = Modifier.padding(start = 8.dp))
                                    }
                                },
                                onClick = {
                                    val updated = if (option in selectedMulti) selectedMulti - option else selectedMulti + option
                                    onFieldChanged(field.fieldKey, updated)
                                    searchText = ""
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onFieldChanged(field.fieldKey, option)
                                    searchText = ""
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
}

@Composable
private fun DateInputField(
    field: FormFieldData.DateInputData,
    value: String,
    onFieldChanged: (String, Any) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    MMPClickableTextField(
        value = value,
        label = field.label,
        onClick = { showPicker = true },
        trailingIcon = Icons.Default.CalendarToday,
        contentDescription = "Select date",
        modifier = Modifier.fillMaxWidth(),
    )

    if (showPicker) {
        MMPDatePickerDialog(
            onDismissRequest = { showPicker = false },
            onDateSelected = { date ->
                onFieldChanged(field.fieldKey, date.toString())
                showPicker = false
            },
        )
    }
}

@Composable
private fun TimeInputField(
    field: FormFieldData.TimeInputData,
    value: String,
    onFieldChanged: (String, Any) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    MMPClickableTextField(
        value = value,
        label = field.label,
        onClick = { showPicker = true },
        trailingIcon = Icons.Default.Schedule,
        contentDescription = "Select time",
        modifier = Modifier.fillMaxWidth(),
    )

    if (showPicker) {
        MMPTimePickerDialog(
            onDismissRequest = { showPicker = false },
            onTimeSelected = { hour, minute ->
                onFieldChanged(field.fieldKey, "%02d:%02d".format(hour, minute))
                showPicker = false
            },
        )
    }
}
