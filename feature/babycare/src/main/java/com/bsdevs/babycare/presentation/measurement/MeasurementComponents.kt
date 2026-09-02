package com.bsdevs.babycare.presentation.measurement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bsdevs.babycare.network.MeasurementDto
import com.bsdevs.uicomponents.DeleteConfirmationDialog

@Composable
fun GrowthChartComponent(
    title: String,
    dataType: String,
    measurements: List<MeasurementDto>
) {
    val valueSelector: (MeasurementDto) -> Double? = when (dataType) {
        "WEIGHT" -> { { it.weight } }
        "HEIGHT" -> { { it.height } }
        "HEAD" -> { { it.headCircumference } }
        else -> { { null } }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        GrowthChartSection(
            title = title,
            data = measurements,
            valueSelector = valueSelector,
            dotColorMedical = MaterialTheme.colorScheme.primary,
            dotColorSelf = MaterialTheme.colorScheme.secondary,
            isWeight = dataType == "WEIGHT"
        )
    }
}

@Composable
fun MeasurementHistoryComponent(
    measurements: List<MeasurementDto>,
    onEdit: (String) -> Unit,
    onDelete: (String, String) -> Unit
) {
    var itemToDelete by remember { mutableStateOf<MeasurementDto?>(null) }

    if (itemToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                itemToDelete?.let { item ->
                    val id = item.id
                    val date = item.date
                    if (id != null && date != null) {
                        onDelete(id, date)
                    }
                }
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(text = "Measurement History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        
        if (measurements.isEmpty()) {
            Text("No measurements found.")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                measurements.forEach { item ->
                    MeasurementHistoryItem(
                        measurement = item, 
                        onEdit = { item.id?.let { onEdit(it) } },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }
    }
}
