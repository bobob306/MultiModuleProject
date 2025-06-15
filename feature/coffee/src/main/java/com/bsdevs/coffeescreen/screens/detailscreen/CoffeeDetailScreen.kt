package com.bsdevs.coffeescreen.screens.detailscreen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.inputscreen.ErrorScreen
import com.bsdevs.coffeescreen.screens.inputscreen.LoadingScreen
import com.bsdevs.common.result.Result

@Composable
fun CoffeeDetailScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    navigateToCoffeeHome: (navOptions: NavOptions?) -> Unit,
    viewModel: CoffeeDetailsViewModel = hiltViewModel()
) {
    val viewData = viewModel.viewData.collectAsStateWithLifecycle()
    Surface(
        modifier = Modifier
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

            is Result.Success<CoffeeDto> -> {
                CoffeeDetailContent(coffeeDto = (viewData.value as Result.Success<CoffeeDto>).data)
            }
        }
    }
}

@Composable
fun CoffeeDetailContent(coffeeDto: CoffeeDto) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val contentModifier = if (isLandscape) {
        Modifier
            .padding(16.dp) // Add padding around the content area in landscape
            .fillMaxHeight() // Card column will fill height
            .fillMaxWidth(0.5f) // Card column takes left half
    } else {
        Modifier
            .padding(16.dp) // Add padding around the content area in portrait
            .fillMaxWidth() // Card column takes full width
            .wrapContentHeight() // Height wraps content
    }

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left half: Coffee Details Card
            Card(
                modifier = contentModifier, // This now applies .fillMaxWidth(0.5f) from above
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                CoffeeDetailsScrollableColumn(coffeeDto)
            }
            // Right half: Empty or for other content (e.g., an image, related items)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth() // Fills the remaining half
                    .padding(16.dp), // Optional padding for the right side
                contentAlignment = Alignment.Center
            ) {
                // You could put something else here for landscape mode
                // For example: an image of the coffee, or related info
                Text("Landscape Right Panel (Optional Content)")
            }
        }
    } else {
        // Portrait mode: Card takes full width
        Box(
            modifier = Modifier.fillMaxSize(), // Box to center the card if it's not filling size
            contentAlignment = Alignment.TopCenter // Align card to the top
        ) {
            Card(
                modifier = contentModifier, // This applies .fillMaxWidth() from above
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                CoffeeDetailsScrollableColumn(coffeeDto)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoffeeDetailsScrollableColumn(coffeeDto: CoffeeDto) {
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .padding(16.dp)
            .wrapContentHeight()
        // Apply verticalScroll only to the Column, not the Card directly
        // to ensure the Card itself doesn't try to scroll if its content is fixed height.
        // Allow column to take available height within the Card
    ) {
        Text(
            text = coffeeDto.label ?: "Unnamed Coffee",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        FlowColumn(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            coffeeDto.roastDate?.takeIf { it.isNotBlank() }?.let {
                CoffeeDetailItem(label = "Roast Date", value = it)
            }
            coffeeDto.roaster?.takeIf { it.isNotBlank() }?.let {
                CoffeeDetailItem(label = "Roaster", value = it)
            }
            coffeeDto.beanTypes?.takeIf { it.isNotEmpty() }?.let {
                CoffeeDetailItem(label = "Bean Types", value = it.joinToString(", "))
            }
            coffeeDto.originCountries?.takeIf { it.isNotEmpty() }?.let {
                CoffeeDetailItem(label = "Origin Countries", value = it.joinToString(", "))
            }
            coffeeDto.tastingNotes?.takeIf { it.isNotEmpty() }?.let {
                CoffeeDetailItem(label = "Tasting Notes", value = it.joinToString(", "))
            }
            coffeeDto.beanPreparationMethod?.takeIf { it.isNotEmpty() }?.let {
                CoffeeDetailItem(label = "Preparation Method", value = it.joinToString(", "))
            }
            coffeeDto.isDecaf?.let {
                CoffeeDetailItem(label = "Decaf", value = if (it) "Yes" else "No")
            }
        }
    }
}

@Composable
fun CoffeeDetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
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