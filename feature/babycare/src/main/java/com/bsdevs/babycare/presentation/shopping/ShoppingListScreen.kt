package com.bsdevs.babycare.presentation.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.uicomponents.DeleteConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.ShoppingListItems(
    viewModel: ShoppingListViewModel
) {
    item(key = "shopping_header") {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        
        Column {
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                Tab(
                    selected = uiState.selectedTab == ShoppingListTab.SHOPPING,
                    onClick = { viewModel.selectTab(ShoppingListTab.SHOPPING) },
                    text = { Text("Shopping") }
                )
                Tab(
                    selected = uiState.selectedTab == ShoppingListTab.TASKS,
                    onClick = { viewModel.selectTab(ShoppingListTab.TASKS) },
                    text = { Text("Tasks") }
                )
            }
            
            ShoppingListHeader(
                name = uiState.newItemName,
                label = if (uiState.selectedTab == ShoppingListTab.SHOPPING) "New Shopping Item" else "New Task",
                onNameChange = viewModel::onNewItemNameChange,
                onSave = viewModel::addItem
            )
        }
    }

    item(key = "shopping_content") {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (uiState.selectedTab == ShoppingListTab.SHOPPING) {
                uiState.items.forEach { item ->
                    ShoppingListItem(
                        name = item.name ?: "",
                        onEdit = { viewModel.setEditingItem(item.id) },
                        onDelete = { item.id?.let { viewModel.setDeletingItem(it) } }
                    )
                }
            } else {
                uiState.taskItems.forEach { task ->
                    ShoppingListItem(
                        name = task.name ?: "",
                        onEdit = { viewModel.setEditingItem(task.id) },
                        onDelete = { task.id?.let { viewModel.setDeletingItem(it) } }
                    )
                }
            }
        }

        if (uiState.deletingItemId != null) {
            val deletingName = if (uiState.selectedTab == ShoppingListTab.SHOPPING) {
                uiState.itemToDelete?.name
            } else {
                uiState.taskToEdit?.name
            }
            DeleteConfirmationDialog(
                title = if (uiState.selectedTab == ShoppingListTab.SHOPPING) "Delete Item" else "Delete Task",
                text = "Are you sure you want to delete '$deletingName'? This cannot be undone.",
                onConfirm = viewModel::confirmDelete,
                onDismiss = { viewModel.setDeletingItem(null) }
            )
        }

        if (uiState.editingItemId != null) {
            EditItemDialog(
                name = uiState.editingName,
                label = if (uiState.selectedTab == ShoppingListTab.SHOPPING) "Item Name" else "Task Name",
                onNameChange = viewModel::onEditingNameChange,
                onDismiss = { viewModel.setEditingItem(null) },
                onConfirm = viewModel::saveEdit
            )
        }
    }
}

@Composable
fun ShoppingListHeader(
    name: String,
    label: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListItem(
    name: String,
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
            else -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Edit
                else -> null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 16.dp),
                contentAlignment = alignment
            ) {
                icon?.let { Icon(it, contentDescription = null) }
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = name,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun EditItemDialog(
    name: String,
    label: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(label) }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
