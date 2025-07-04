package com.bsdevs.coffeescreen.screens.detailscreen

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.detailscreen.components.CoffeeDetailsFirstHalf
import com.bsdevs.coffeescreen.screens.detailscreen.components.EspressoShotInputSheetContent
import com.bsdevs.coffeescreen.screens.detailscreen.components.SecondHalfContent
import com.bsdevs.coffeescreen.screens.inputscreen.ErrorScreen
import com.bsdevs.coffeescreen.screens.inputscreen.LoadingScreen
import com.bsdevs.common.result.Result
import java.time.LocalDate
import java.util.UUID

@Composable
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

    if (coffeeDetailsViewData.showSheet) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(CoffeeDetailsIntent.HideSheet) },
            sheetState = sheetState,
            // You can customize windowInsets, dragHandle, etc.
        ) {
            EspressoShotInputSheetContent(
                onSave = { details ->
                    onIntent(CoffeeDetailsIntent.SubmitShot(details))
                    onIntent(CoffeeDetailsIntent.HideSheet)
                },
                onDismiss = {
                    onIntent(CoffeeDetailsIntent.HideSheet)
                }
            )
        }
    }
    if (isLandscape) {
        // Landscape mode:
        CoffeeDetailLandscapeMode(coffeeDetailsViewData, onAddShotClicked = { onIntent(CoffeeDetailsIntent.ShowSheet) })
    } else {
        // Portrait mode:
        CoffeeDetailPortraitMode(coffeeDetailsViewData, onAddShotClicked = { onIntent(CoffeeDetailsIntent.ShowSheet) })
    }
}

@Composable
private fun CoffeeDetailLandscapeMode(
    viewData: CoffeeDetailsViewData,
    onAddShotClicked: () -> Unit = {},
) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = spacedBy(16.dp)) {
        // Left half: Coffee Details Card
        Card(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .clip(shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 16.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) { // Pass showSheet state and its updater
            CoffeeDetailsFirstHalf(viewData.coffeeDto, true)
        }
        // Right half: Empty or for other content
        Card(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 16.dp))
                .fillMaxWidth()
                .fillMaxHeight(), // This applies .fillMaxWidth() from above
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            SecondHalfContent(
                onAddShotClicked = onAddShotClicked,
                shotList = viewData.shotList
            )
        }
    }
}

@Composable
private fun CoffeeDetailPortraitMode(
    viewData: CoffeeDetailsViewData,
    onAddShotClicked: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(), // This applies .fillMaxWidth() from above
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            CoffeeDetailsFirstHalf(viewData.coffeeDto, false)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(), // This applies .fillMaxWidth() from above
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            SecondHalfContent(
                onAddShotClicked = onAddShotClicked,
                shotList = viewData.shotList,
            )
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

@Preview(showBackground = true)
@Preview(showBackground = true, fontScale = 2f)
@Preview(showBackground = true, device = "spec:parent=resizable,orientation=landscape")
@Composable
private fun CoffeeDetailContentPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
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
                        isDecaf = false,
                        rating = 3,
                    ),
                    null
                ),
                onIntent = {}
            )
        }
    }
}

data class EspressoShotDetails(
    val id: String = UUID.randomUUID().toString(),
    val timeInSeconds: Int = 27,
    val weightInGrams: Int = 175,
    val weightOutGrams: Int = 360,
    val rating: Int = 0,
    @SuppressLint("NewApi") val date: LocalDate = LocalDate.now()
)