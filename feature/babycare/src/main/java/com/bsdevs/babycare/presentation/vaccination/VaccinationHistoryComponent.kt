package com.bsdevs.babycare.presentation.vaccination

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.VaccinationDto
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.uicomponents.DeleteConfirmationDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class VaccinationGroup(
    val seriesId: String?,
    val vaccinations: List<VaccinationDto>,
)

@HiltViewModel
class VaccinationDataViewModel @Inject constructor(
    private val accountService: AccountService,
    private val repository: BabyCareRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    init {
        viewModelScope.launch(dispatchers.io) {
            if (repository.vaccinations.value.isEmpty()) {
                repository.loadInitialData(accountService.currentUserId, 20)
            }
        }
    }

    val groupedVaccinations: StateFlow<List<VaccinationGroup>> = repository.vaccinations
        .map { allVaccinations ->
            allVaccinations.map { event ->
                VaccinationDto(
                    id = event.id,
                    date = event.dateTimeString.split(" ").first(),
                    time = event.time,
                    dateTime = event.dateTimeString,
                    vaccinationNames = event.vaccinationNames ?: emptyList(),
                    location = event.location,
                    seriesId = event.seriesId,
                    comment = event.comment
                )
            }.groupBy { it.seriesId }
                .map { (seriesId, vaccines) ->
                    VaccinationGroup(seriesId, vaccines.sortedBy { it.dateTime })
                }.sortedByDescending { it.vaccinations.lastOrNull()?.dateTime ?: "" }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteVaccination(id: String, date: String) {
        viewModelScope.launch {
            repository.deleteActivityEvent(accountService.currentUserId, date, id)
        }
    }
}

@Composable
fun VaccinationHistoryComponent(
    groupedVaccinations: List<VaccinationGroup>,
    onEdit: (String) -> Unit,
    onDelete: (String, String) -> Unit
) {
    var itemToDelete by remember { mutableStateOf<VaccinationDto?>(null) }

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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedVaccinations.forEach { group ->
            VaccinationGroupItem(
                group = group,
                onEdit = onEdit,
                onDelete = { vaccination -> itemToDelete = vaccination }
            )
        }
    }
}

@Composable
internal fun VaccinationGroupItem(
    group: VaccinationGroup,
    onEdit: (String) -> Unit,
    onDelete: (VaccinationDto) -> Unit
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
                    onDelete = { onDelete(vaccination) }
                )
                if (index < (group.vaccinations.size - 1)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
internal fun VaccinationRow(
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
