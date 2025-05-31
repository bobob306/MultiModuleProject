package com.bsdevs.coffeescreen.screens.detailscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()) // Make content scrollable if it overflows
        ) {
            Text(
                text = coffeeDto.label ?: "Unnamed Coffee",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Display each field if it has a value
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
            coffeeDto.isDecaf?.let { // Boolean, so no need for takeIf { it.isNotBlank() }
                CoffeeDetailItem(label = "Decaf", value = if (it) "Yes" else "No")
            }

            // Note: 'id' and 'userId' are intentionally excluded as per the request.
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