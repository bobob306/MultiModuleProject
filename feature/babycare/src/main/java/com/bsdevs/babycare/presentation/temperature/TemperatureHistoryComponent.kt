package com.bsdevs.babycare.presentation.temperature

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.DailyLogDto
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.babycare.presentation.temperature.TemperatureHistoryUiData
import com.bsdevs.babycare.presentation.temperature.TemperatureItem
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.uicomponents.DeleteConfirmationDialog
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class TemperatureHistoryUiData(
    val dates: List<String> = emptyList(),
    val dailyReadings: Map<String, List<TemperatureItem>> = emptyMap()
)

@HiltViewModel
class TemperatureDataViewModel @Inject constructor(
    private val accountService: AccountService,
    private val repository: BabyCareRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemperatureHistoryUiData())
    val uiState: StateFlow<TemperatureHistoryUiData> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.cachedDays.collect { dailyLogs ->
                updateHistory(dailyLogs)
            }
        }
    }

    private suspend fun updateHistory(dailyLogs: List<DailyLogDto>) = withContext(dispatchers.default) {
        val allReadings = dailyLogs.flatMap { day ->
            day.events
                .filter { it.type == "TEMPERATURE" }
                .map { event ->
                    TemperatureItem(
                        id = event.id,
                        date = day.date,
                        time = event.time,
                        temperature = event.temperature ?: 37.0,
                        comment = event.comment
                    )
                }
        }

        val grouped = allReadings.groupBy { it.date }
        val sortedDates = grouped.keys.sortedDescending()

        _uiState.update { it.copy(
            dates = sortedDates,
            dailyReadings = grouped
        ) }
    }

    fun deleteTemperature(id: String, date: String) {
        val userId = accountService.currentUserId
        viewModelScope.launch {
            try {
                repository.deleteActivityEvent(userId, date, id)
            } catch (e: Exception) {
                Log.e("TEMP_DELETE", "Error deleting temperature", e)
            }
        }
    }
}

@Composable
fun TemperatureHistoryComponent(
    uiData: TemperatureHistoryUiData,
    onEdit: (String) -> Unit,
    onDelete: (String, String) -> Unit
) {
    var itemToDelete by remember { mutableStateOf<TemperatureItem?>(null) }

    if (itemToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                itemToDelete?.let { item ->
                    onDelete(item.id, item.date)
                }
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    val pagerState = rememberPagerState { uiData.dates.size }
    val scope = rememberCoroutineScope()

    if (uiData.dates.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("No temperature records found.")
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(400.dp),
            verticalAlignment = Alignment.Top
        ) { pageIndex ->
            val date = uiData.dates[pageIndex]
            val readings = uiData.dailyReadings[date] ?: emptyList()

            Column(modifier = Modifier.fillMaxSize()) {
                DayNavigationHeader(
                    date = date,
                    pagerState = pagerState,
                    datesCount = uiData.dates.size,
                    onNavigate = { page: Int ->
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
                    items(readings, key = { it.id }) { item ->
                        TemperatureHistoryItem(
                            item = item,
                            onEdit = { onEdit(item.id) },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TemperatureChartComponent(
    uiData: TemperatureHistoryUiData
) {
    val latestDate = uiData.dates.firstOrNull() ?: return
    val readings = uiData.dailyReadings[latestDate] ?: emptyList()

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp)) {
        Text(
            text = "Daily Trend ($latestDate)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp).padding(horizontal = 16.dp)
        )
        TemperatureChart(
            readings = readings,
            modifier = Modifier.fillMaxWidth().height(260.dp)
        )
    }
}

@Composable
internal fun DayNavigationHeader(
    date: String,
    pagerState: PagerState,
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

        val displayDate = try {
            val parts = date.split("-")
            if (parts.size >= 3) {
                "${parts[2]} ${parts[1]} ${parts[0].substring(2)}"
            } else date
        } catch (e: Exception) { date }

        Text(
            text = displayDate,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemperatureHistoryItem(
    item: TemperatureItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onDelete()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> {
                onEdit()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.Settled -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            val bgColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
            }
            val swipeIcon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Edit
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CardDefaults.shape)
                    .background(bgColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = swipeIcon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (direction == SwipeToDismissBoxValue.StartToEnd)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
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
}
