package com.bsdevs.babycare.presentation.vaccination

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.babycare.network.VaccinationDto
import com.bsdevs.common.result.Result
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.renderer.RenderUI
import com.bsdevs.uicomponents.MMPScaffold

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VaccinationHistoryScreenRoute(
    onNavigateBack: () -> Unit,
    onAddNew: () -> Unit,
    onEditItem: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: VaccinationHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    VaccinationHistoryScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onNavigateBack = onNavigateBack,
        onAddNew = onAddNew,
        onEditItem = onEditItem,
        onDeleteItem = viewModel::deleteVaccination,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VaccinationHistoryScreen(
    uiState: VaccinationHistoryUiState,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit,
    onAddNew: () -> Unit,
    onEditItem: (String) -> Unit,
    onDeleteItem: (String, String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current

    MMPScaffold(
        title = "Vaccinations",
        onBackClick = onNavigateBack,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew) {
                Icon(Icons.Default.Add, contentDescription = "Add Vaccination")
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (val dynamicUi = uiState.dynamicUi) {
                    is Result.Success -> {
                        dynamicUi.data.forEach { component ->
                            when (component) {
                                is NetworkScreenData.VaccinationHistoryDataNetwork -> {
                                    items(uiState.groupedVaccinations) { group ->
                                        VaccinationGroupItem(
                                            group = group,
                                            onEdit = onEditItem,
                                            onDelete = onDeleteItem
                                        )
                                    }
                                }
                                else -> {
                                    item(key = "dynamic_${component.index}") {
                                        Column {
                                            RenderUI(
                                                item = component,
                                                context = context,
                                                onClick = { _, _ -> },
                                                onChipClick = {},
                                                onSwitchClick = {}
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is Result.Loading -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is Result.Error -> {
                        item {
                            Text("Error loading screen configuration", modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaccinationGroupItem(
    group: VaccinationGroup,
    onEdit: (String) -> Unit,
    onDelete: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!group.seriesId.isNullOrEmpty()) {
                Text(
                    text = "Series: ${group.seriesId}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            group.vaccinations.forEachIndexed { index, vaccination ->
                VaccinationRow(
                    vaccination = vaccination,
                    onEdit = { vaccination.id?.let { onEdit(it) } },
                    onDelete = { vaccination.date?.let { d -> vaccination.id?.let { id -> onDelete(d, id) } } }
                )
                if (index < group.vaccinations.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun VaccinationRow(
    vaccination: VaccinationDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color(0xFFFCE4EC), CircleShape).clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MedicalServices,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFFC2185B)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = vaccination.vaccinationNames.joinToString(", "),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${vaccination.date} at ${vaccination.time}",
                style = MaterialTheme.typography.bodySmall
            )
            if (!vaccination.location.isNullOrEmpty()) {
                Text(
                    text = "Location: ${vaccination.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            Icons.Default.Delete,
            contentDescription = "Delete",
            modifier = Modifier.clickable { onDelete() }.size(24.dp),
            tint = MaterialTheme.colorScheme.error
        )
    }
}
