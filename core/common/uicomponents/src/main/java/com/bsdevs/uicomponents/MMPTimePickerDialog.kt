package com.bsdevs.uicomponents

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MMPTimePickerDialog(
    onDismissRequest: () -> Unit,
    initialTime: LocalTime = LocalTime.now(),
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                    onDismissRequest()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLandscape) {
                    Text(
                        text = "Enter time",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    TimeInput(state = timePickerState)
                } else {
                    TimePicker(state = timePickerState)
                }
            }
        }
    )
}
