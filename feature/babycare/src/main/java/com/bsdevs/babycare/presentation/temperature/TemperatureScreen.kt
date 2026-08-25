package com.bsdevs.babycare.presentation.temperature

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.uicomponents.LogCommentInput
import com.bsdevs.uicomponents.MMPClickableTextField
import com.bsdevs.uicomponents.MMPScaffold
import com.bsdevs.uicomponents.MMPTimePickerDialog
import com.bsdevs.uicomponents.WheelInput
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun TemperatureScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: TemperatureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TemperatureUiEffect.SaveSuccess -> {
                    onShowSnackBar("Temperature saved", null)
                }
                is TemperatureUiEffect.DeleteSuccess -> {
                    onShowSnackBar("Temperature deleted", null)
                }
                is TemperatureUiEffect.SaveError -> {
                    onShowSnackBar("Error saving temperature: ${event.message}", null)
                }
            }
        }
    }

    TemperatureScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onTemperatureValueSelected = viewModel::onTemperatureValueSelected,
        onCommentChanged = viewModel::onCommentChanged,
        onDateSelected = viewModel::onDateSelected,
        onTimeSelected = viewModel::onTimeSelected,
        onSave = viewModel::submitTemperature,
        onDelete = viewModel::deleteTemperature,
        onResetForm = viewModel::resetForm,
        onEditItem = viewModel::onEditTemperature
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperatureScreen(
    uiState: TemperatureUiState,
    onNavigateBack: () -> Unit,
    onTemperatureValueSelected: (Int) -> Unit,
    onCommentChanged: (String) -> Unit,
    onDateSelected: (String) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onResetForm: () -> Unit,
    onEditItem: (String) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = rememberPagerState { uiState.dates.size }
    var showSheet by rememberSaveable { mutableStateOf(uiState.id != null) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Re-open sheet if id is set (e.g. from navigation)
    LaunchedEffect(uiState.id) {
        if (uiState.id != null) {
            showSheet = true
        }
    }

    MMPScaffold(
        title = "Temperature History",
        onBackClick = onNavigateBack,
        scrollBehavior = scrollBehavior,
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                onResetForm()
                showSheet = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Temperature")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.dates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No temperature records found.")
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top
                ) { pageIndex ->
                    val date = uiState.dates[pageIndex]
                    val readings = uiState.dailyReadings[date] ?: emptyList()

                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left Column: Navigation + List
                            Column(modifier = Modifier.weight(1f)) {
                                DayNavigationHeader(
                                    date = date,
                                    pagerState = pagerState,
                                    datesCount = uiState.dates.size,
                                    onNavigate = { page ->
                                        scope.launch { pagerState.animateScrollToPage(page) }
                                    }
                                )

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.surfaceContainer),
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(readings) { item ->
                                        TemperatureHistoryItem(item = item, onClick = {
                                            onEditItem(item.id)
                                            showSheet = true
                                        })
                                    }
                                }
                            }

                            // Right Column: Chart
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Daily Trend",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )

                                TemperatureChart(
                                    readings = readings,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            DayNavigationHeader(
                                date = date,
                                pagerState = pagerState,
                                datesCount = uiState.dates.size,
                                onNavigate = { page ->
                                    scope.launch { pagerState.animateScrollToPage(page) }
                                }
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(readings) { item ->
                                    TemperatureHistoryItem(item = item, onClick = {
                                        onEditItem(item.id)
                                        showSheet = true
                                    })
                                }
                            }

                            Text(
                                text = "Daily Trend",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            TemperatureChart(
                                readings = readings,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            if (uiState.isLoading && !showSheet) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showSheet = false 
                },
                sheetState = sheetState
            ) {
                TemperatureForm(
                    uiState = uiState,
                    onTemperatureValueSelected = onTemperatureValueSelected,
                    onCommentChanged = onCommentChanged,
                    onDateSelected = { showDatePicker = true },
                    onTimeSelected = { showTimePicker = true },
                    onSave = {
                        onSave()
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showSheet = false
                        }
                    },
                    onDelete = { showDeleteConfirmation = true }
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(uiState.date)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val selectedDate = Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(selectedDate.toString())
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initialTime = try {
            LocalTime.parse(uiState.time)
        } catch (e: Exception) {
            LocalTime.now()
        }

        MMPTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            initialTime = initialTime,
            onTimeSelected = { h, m ->
                onTimeSelected(h, m)
                showTimePicker = false
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Temperature Record") },
            text = { Text("Are you sure you want to delete this temperature reading?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showSheet = false
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DayNavigationHeader(
    date: String,
    pagerState: androidx.compose.foundation.pager.PagerState,
    datesCount: Int,
    onNavigate: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onNavigate(pagerState.currentPage - 1) },
            enabled = pagerState.currentPage > 0
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous Day",
                tint = if (pagerState.currentPage > 0)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline
            )
        }

        Text(
            text = date,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(
            onClick = { onNavigate(pagerState.currentPage + 1) },
            enabled = pagerState.currentPage < datesCount - 1
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next Day",
                tint = if (pagerState.currentPage < datesCount - 1)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun TemperatureHistoryItem(item: TemperatureItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Thermostat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.temperature}°C",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!item.comment.isNullOrBlank()) {
                    Text(
                        text = item.comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = item.time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TemperatureForm(
    uiState: TemperatureUiState,
    onTemperatureValueSelected: (Int) -> Unit,
    onCommentChanged: (String) -> Unit,
    onDateSelected: () -> Unit,
    onTimeSelected: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (uiState.id == null) "Add Temperature" else "Edit Temperature",
            style = MaterialTheme.typography.headlineSmall
        )
        
        HorizontalDivider()

        MMPClickableTextField(
            value = uiState.date,
            label = "Date",
            onClick = onDateSelected,
            enabled = !uiState.isLoading,
            trailingIcon = Icons.Default.DateRange,
            contentDescription = "Select Date"
        )

        MMPClickableTextField(
            value = uiState.time,
            label = "Time",
            onClick = onTimeSelected,
            enabled = !uiState.isLoading,
            trailingIcon = Icons.Default.DateRange,
            contentDescription = "Select Time"
        )

        WheelInput(
            isDecimal = true,
            startNumber = 350,
            endNumber = 420,
            initialSelectedItem = uiState.temperatureValue,
            onItemSelected = onTemperatureValueSelected,
            label = "Temperature (°C)"
        )

        LogCommentInput(
            comment = uiState.comment,
            onCommentChange = onCommentChanged,
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.error != null) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Save Temperature")
            }
        }

        if (uiState.id != null) {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                enabled = !uiState.isLoading
            ) {
                Text("Delete Record")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
