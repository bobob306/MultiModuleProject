package com.bsdevs.homescreen.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.uicomponents.MMPScaffold

@Composable
fun SettingsRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.accountDeleted) {
        LaunchedEffect(Unit) {
            onShowSnackBar("Account deleted successfully", null)
        }
    }

    SettingsScreen(
        uiState = uiState,
        onDeleteAccount = viewModel::deleteAccount,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onDeleteAccount: () -> Unit
) {
    var showFirstConfirmation by remember { mutableStateOf(false) }
    var showSecondConfirmation by remember { mutableStateOf(false) }

    MMPScaffold(
        title = "Settings"
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Text(
                    text = "User: ${uiState.userName}",
                    style = MaterialTheme.typography.bodyLarge
                )

                uiState.babyName?.let {
                    Text(
                        text = "Baby: $it",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { showFirstConfirmation = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Account & Wipe Data")
                }

                uiState.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    if (showFirstConfirmation) {
        AlertDialog(
            onDismissRequest = { showFirstConfirmation = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete your account? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showFirstConfirmation = false
                    showSecondConfirmation = true
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFirstConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSecondConfirmation) {
        AlertDialog(
            onDismissRequest = { showSecondConfirmation = false },
            title = { Text("Final Confirmation") },
            text = { Text("ALL your data will be permanently wiped. Are you absolutely sure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSecondConfirmation = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("DELETE EVERYTHING")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecondConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
