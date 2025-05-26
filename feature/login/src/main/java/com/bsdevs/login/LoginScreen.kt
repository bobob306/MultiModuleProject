package com.bsdevs.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.bsdevs.common.result.Result
import com.bsdevs.uicomponents.ErrorScreen
import com.bsdevs.uicomponents.LoadingScreen

@Composable
fun LoginScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToCoffeeHome: (navOptions: NavOptions?) -> Unit,
    viewModel: LoginScreenViewModel = hiltViewModel()
) {
    val viewData = viewModel.viewData.collectAsStateWithLifecycle()
    when (viewData.value) {
        Result.Loading -> LoadingScreen()
        is Result.Error -> ErrorScreen()
        is Result.Success -> LoginScreenContent(
            viewData = (viewData.value as Result.Success<LoginViewData>).data,
            onIntent = viewModel::processIntent
        )
    }
}

@Composable
fun LoginScreenContent(
    viewData: LoginViewData,
    onIntent: (LoginScreenIntent) -> Unit
) {
    val focusManager = LocalFocusManager.current // To handle keyboard actions
    viewData.run {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Add some padding around the content
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Login Screen",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            OutlinedTextField(
                value = email,
                onValueChange = {
                    onIntent(LoginScreenIntent.UpdateEmail(it))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email Address") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Icon"
                    )
                },
                trailingIcon = {
                    if (viewData.email.isNotEmpty()) {
                        IconButton(onClick = { onIntent(LoginScreenIntent.UpdateEmail("")) }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Email"
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next // Or ImeAction.Done if it's the last field
                ),
                singleLine = true,
                // You can also add supportingText = { Text("Error message") } when isError is true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = viewData.password,
                onValueChange = { newPassword ->
                    onIntent(LoginScreenIntent.UpdatePassword(newPassword))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password Icon"
                    )
                },
                trailingIcon = {
                    val image = if (viewData.isPasswordVisible)
                        Icons.Default.Lock
                    else Icons.Outlined.Lock

                    val description =
                        if (viewData.isPasswordVisible) "Hide password" else "Show password"

                    IconButton(onClick = { onIntent(LoginScreenIntent.UpdatePasswordVisibility) }) {
                        Icon(imageVector = image, description)
                    }
                },
                visualTransformation = if (viewData.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done // Set to Done as it's likely the last field before login
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus() // Clear focus when "Done" is pressed
                        onIntent(LoginScreenIntent.Login) // Optionally trigger login on Done
                    }
                ),
                singleLine = true,
                isError = false // TODO: Add password validation
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onIntent(LoginScreenIntent.Login) },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewData.email.isNotEmpty() && viewData.password.isNotEmpty() // Disable button while loading
            ) {
                if (viewData.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp), // Adjust size as needed
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }
            Button(
                onClick = { onIntent(LoginScreenIntent.Register) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewData.isLoading // Disable button while loading
            ) {
                Text("Register")
            }
        }
    }
}