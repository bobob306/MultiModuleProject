package com.bsdevs.renderer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bsdevs.data.RadioButtonLabelData
import com.bsdevs.data.NetworkScreenData.RadioButtonDataNetwork

@Composable
fun RadioButtonListComponent(
    radioButtons: RadioButtonDataNetwork,
) {
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioButtons.labels[0]) }
    Column(Modifier.selectableGroup()) {
        radioButtons.labels.forEach { label ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .selectable(
                        selected = (label == selectedOption),
                        onClick = { onOptionSelected(label) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label.label ?: "")
                Spacer(Modifier.weight(1f))
                RadioButton(
                    selected = (label == selectedOption),
                    onClick = { onOptionSelected(label) }
                )
            }
            if (label != radioButtons.labels.last())
                HorizontalDivider()
        }

    }
}

@Preview
@Composable
fun RadioButtonListComponentPreview() {
    RadioButtonListComponent(
        radioButtons = RadioButtonDataNetwork(
            index = 0,
            labels = listOf(
                RadioButtonLabelData("something", null, 0),
                RadioButtonLabelData("something 1", null, 1),
                RadioButtonLabelData("something 2", null, 2),
                RadioButtonLabelData("something 3", null, 3),
                RadioButtonLabelData("something 4", null, 4),
            )
        )
    )
}

