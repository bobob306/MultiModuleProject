package com.bsdevs.login.registerscreen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.bsdevs.common.result.Result
import com.bsdevs.uicomponents.ErrorScreen
import com.bsdevs.uicomponents.LoadingScreen

@Composable
fun RegisterScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToLogin: (navOptions: NavOptions?) -> Unit,
    viewModel: RegisterScreenViewModel = hiltViewModel()
) {
    val viewData = viewModel.viewData.collectAsStateWithLifecycle()
    when (viewData.value) {
        Result.Loading -> LoadingScreen()
        is Result.Error -> ErrorScreen()
        is Result.Success -> RegisterScreenContent(
            viewData = (viewData.value as Result.Success<RegisterScreenViewData>).data,
            onIntent = viewModel::processIntent
        )
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { navigationEvent ->
            when (navigationEvent) {
                is RegisterNavigationEvent.SuccessfulAccountCreation -> {
                    onShowSnackBar("Account Created", null)
                    onNavigateToLogin.invoke(null)
                }

                RegisterNavigationEvent.NavigateToLogin -> onNavigateToLogin.invoke(null)
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:parent=pixel_5,orientation=landscape")
@Composable
fun RegisterScreenContentPreview() {
    RegisterScreenContent(
        viewData = RegisterScreenViewData(
            email = "test@example.com",
            password = "password",
            passwordConfirmation = "password",
            isPasswordVisible = false,
            isPasswordConfirmationVisible = false,
            isLoading = false
        ),
        onIntent = {}
    )
}


@Composable
fun RegisterScreenContent(
    viewData: RegisterScreenViewData,
    onIntent: (RegisterScreenIntent) -> Unit
) {
    val focusManager = LocalFocusManager.current // To handle keyboard actions
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()

    val scrollProgress by remember(scrollState.value, scrollState.maxValue) {
        derivedStateOf {
            if (scrollState.maxValue > 0) {
                scrollState.value.toFloat() / scrollState.maxValue.toFloat()
            } else {
                0f
            }
        }
    }

    val isScrollable by remember(scrollState.maxValue) {
        derivedStateOf {
            scrollState.maxValue > 0
        }
    }
    val scrollProgressFromTop by remember(scrollState.value, scrollState.maxValue) {
        derivedStateOf {
            if (scrollState.maxValue > 0) {
                scrollState.value.toFloat() / scrollState.maxValue.toFloat()
            } else {
                0f
            }
        }
    }
    val density = LocalDensity.current
    val visiblePortionFraction by remember(scrollState.maxValue, scrollState.viewportSize) {
        derivedStateOf {
            if (scrollState.maxValue > 0 && scrollState.viewportSize > 0) {
                val viewportSize = scrollState.viewportSize.toFloat()
                val totalContentHeight = viewportSize + scrollState.maxValue.toFloat()
                (viewportSize / totalContentHeight).coerceIn(
                    0.05f,
                    1f
                ) // Ensure min 5% height for thumb
            } else {
                1f // If not scrollable or viewport not determined, thumb is full height (won't be shown anyway)
            }
        }
    }
    Surface(
        Modifier
            .fillMaxSize()
    ) {
        viewData.run {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .padding(end = if (isScrollable && isLandscape) 16.dp else 0.dp), // Add some padding around the content
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Register Screen",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = if (isLandscape) 8.dp else 24.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email ->
                        onIntent(RegisterScreenIntent.UpdateEmail(email))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email Address") },
                    supportingText = { emailError?.let { Text(it) } },
                    singleLine = true,
                    // You can also add supportingText = { Text("Error message") } when isError is true
                    trailingIcon = {
                        if (viewData.email.isNotEmpty()) {
                            IconButton(onClick = { onIntent(RegisterScreenIntent.UpdateEmail("")) }) {
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
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Icon"
                        )
                    },
                )
                Spacer(modifier = Modifier.height(if (isLandscape) 0.dp else 16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { newPassword: String ->
                        onIntent(RegisterScreenIntent.UpdatePassword(newPassword))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Password") },
                    placeholder = { Text("Enter your password") },
                    supportingText = { passwordError?.let { Text(it) } },
                    singleLine = true,
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

                        IconButton(onClick = { onIntent(RegisterScreenIntent.UpdatePasswordVisibility) }) {
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
                            focusManager.clearFocus()
                            ImeAction.Next // Clear focus when "Done" is pressed
                        }
                    ),
                )
                Spacer(modifier = Modifier.height(if (isLandscape) 0.dp else 16.dp))
                OutlinedTextField(
                    value = passwordConfirmation,
                    visualTransformation = if (viewData.isPasswordConfirmationVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    onValueChange = { newPasswordConfirmation: String ->
                        onIntent(
                            RegisterScreenIntent.UpdatePasswordConfirmation(
                                newPasswordConfirmation
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Confirm Password") },
                    placeholder = { Text("Confirm your password") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password Icon"
                        )
                    },
                    trailingIcon = {
                        val image = if (viewData.isPasswordConfirmationVisible)
                            Icons.Default.Lock
                        else Icons.Outlined.Lock
                        val description =
                            if (viewData.isPasswordConfirmationVisible) "Hide password" else "Show password"
                        IconButton(onClick = { onIntent(RegisterScreenIntent.UpdatePasswordConfirmationVisibility) }) {
                            Icon(imageVector = image, description)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done // Set to Done as it's likely the last field before login
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (
                                viewData.password == viewData.passwordConfirmation && viewData.password.isNotEmpty() && viewData.email.isNotEmpty() && !viewData.isLoading
                            ) {
                                onIntent(RegisterScreenIntent.Register) // Clear focus when "Done" is pressed
                            }
                        }
                    ),
                )
                Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 16.dp))
                Button(
                    onClick = { onIntent(RegisterScreenIntent.Register) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewData.password == viewData.passwordConfirmation && viewData.password.isNotEmpty() && viewData.email.isNotEmpty() && !viewData.isLoading
                ) {
                    Text("Register")
                }
                Button(
                    onClick = { onIntent(RegisterScreenIntent.NavigateToLogin) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Go To Login")
                }
            }
        }
        if (isScrollable && isLandscape) {
            val scrollbarWidth = 8.dp
            val minThumbVisualHeightDp = 20.dp // Minimum visible height for the thumb in Dp

            Box( // Container for the scrollbar
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 12.dp) // Align with content padding
            ) {
                BoxWithConstraints(contentAlignment = Alignment.CenterEnd) { // Use BoxWithConstraints to get the actual track height
                    val trackActualHeightDp = this.maxHeight
                    val trackActualHeightPx = with(density) { trackActualHeightDp.toPx() }

                    // Calculate thumb height in Dp, ensuring it's not smaller than minThumbVisualHeightDp
                    val thumbHeightDp = (trackActualHeightDp * visiblePortionFraction)
                        .coerceAtLeast(minThumbVisualHeightDp)
                    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }


                    // Calculate the total movable range for the top of the thumb
                    val movableRangePx = trackActualHeightPx - thumbHeightPx

                    // Calculate the thumb's Y offset based on how much is scrolled from the top
                    val thumbOffsetYPx = (movableRangePx * scrollProgressFromTop).coerceAtLeast(0f)
                    val thumbOffsetYDp = with(density) { thumbOffsetYPx.toDp() }


                    // Track
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(scrollbarWidth)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clip(RoundedCornerShape(4.dp)) // Clip background
                    ) {
                        // Thumb
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart) // Thumb starts at top of its calculated offset
                                .width(scrollbarWidth)
                                .height(thumbHeightDp) // Use calculated thumb height
                                .offset(y = thumbOffsetYDp) // Apply calculated offset
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clip(RoundedCornerShape(4.dp)) // Clip thumb itself
                        )
                    }
                }
            }
        }
    }
}