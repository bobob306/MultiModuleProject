package com.bsdevs.coffeescreen.screens.detailscreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bsdevs.coffeescreen.network.CoffeeDto

@Composable
internal fun CoffeeDetailsFirstHalf(coffeeDto: CoffeeDto, isLandscape: Boolean) {
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .wrapContentHeight() // Ensure the height wraps content
        // Apply verticalScroll only to the Column, not the Card directly
        // to ensure the Card itself doesn't try to scroll if its content is fixed height.
        // Allow column to take available height within the Card
    ) {
        Text(
            text = coffeeDto.label ?: "Unnamed Coffee",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        CoffeeDetailGrid(coffeeDto)
    }
}

@Composable
internal fun CoffeeDetailGrid(coffeeDto: CoffeeDto) {
    LazyVerticalGrid(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth(),
        columns = GridCells.Fixed(2),
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

@Composable
internal fun CoffeeDetailItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .padding(start = 8.dp, bottom = 4.dp)
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