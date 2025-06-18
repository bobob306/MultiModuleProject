package com.bsdevs.login.loginscreen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.material3.CircularProgressIndicator
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
fun LoginScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToCoffeeHome: (navOptions: NavOptions?) -> Unit,
    onNavigateToRegisterScreen: (navOptions: NavOptions?) -> Unit,
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

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                NavigationEvent.NavigateToCoffeeHome -> onNavigateToCoffeeHome(null)
                NavigationEvent.NavigateToRegister -> onNavigateToRegisterScreen(null)
            }
        }
    }
}

@Composable
fun LoginScreenContent(
    viewData: LoginViewData,
    onIntent: (LoginScreenIntent) -> Unit
) {
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
        val focusManager = LocalFocusManager.current // To handle keyboard actions
        viewData.run {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(end = if (isScrollable) 16.dp else 0.dp)
                    .verticalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
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
                        supportingText = { emailError?.let { Text(it) } },
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
                                if (viewData.email.isNotEmpty() && viewData.password.isNotEmpty() && !isLoading) {
                                    onIntent(LoginScreenIntent.Login) // Optionally trigger login on Done
                                }
                            }
                        ),
                        singleLine = true,
                        isError = false // TODO: Add password validation
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onIntent(LoginScreenIntent.Login) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewData.email.isNotEmpty() && viewData.password.isNotEmpty() && !isLoading // Disable button while loading
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

@Preview(showBackground = true)
@Preview(showBackground = true, device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun LoginScreenContentPreview() {
    LoginScreenContent(
        viewData = LoginViewData(),
        onIntent = {}
    )
}