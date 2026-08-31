package com.bsdevs.forms.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bsdevs.common.result.Result

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: FormViewModel = hiltViewModel(),
) {
    val formSchema by viewModel.formSchema.collectAsState()
    val fieldValues by viewModel.fieldValues.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditMode = viewModel.entityId != null

    LaunchedEffect(submitState) {
        when (val state = submitState) {
            is FormSubmitState.Success -> {
                viewModel.clearSubmitState()
                onNavigate(state.destination)
            }
            is FormSubmitState.Deleted -> {
                viewModel.clearSubmitState()
                onNavigateBack()
            }
            is FormSubmitState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearSubmitState()
            }
            else -> Unit
        }
    }

    val title = (formSchema as? Result.Success)?.data?.title ?: ""

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val schema = formSchema) {
            Result.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is Result.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Failed to load form: ${schema.exception.message}") }

            is Result.Success -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                ) {
                    items(schema.data.fields, key = { it.fieldKey }) { field ->
                        FormFieldItem(
                            field = field,
                            value = fieldValues[field.fieldKey],
                            fieldValues = fieldValues,
                            onFieldChanged = viewModel::onFieldChanged,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                val isLoading = submitState is FormSubmitState.Loading

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Button(
                        onClick = viewModel::onSubmit,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        } else {
                            Text(if (isEditMode) "Save Changes" else "Submit")
                        }
                    }

                    if (isEditMode && schema.data.deletable) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = viewModel::onDelete,
                            enabled = !isLoading,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Delete")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
