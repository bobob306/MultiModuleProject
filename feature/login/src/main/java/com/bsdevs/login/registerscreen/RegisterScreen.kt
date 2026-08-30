package com.bsdevs.login.registerscreen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.bsdevs.common.result.Result
import com.bsdevs.uicomponents.ErrorScreen
import com.bsdevs.uicomponents.LoadingScreen
import com.bsdevs.uicomponents.MMPDatePickerDialog
import com.bsdevs.uicomponents.MMPScaffold
import java.time.Instant
import java.time.ZoneId

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
                    onShowSnackBar("Account Created Successfully", null)
                    onNavigateToLogin.invoke(
                        NavOptions.Builder()
                            .setPopUpTo(route = com.bsdevs.login.RegisterScreenRoute, inclusive = true)
                            .build()
                    )
                }

                is RegisterNavigationEvent.Failure -> {
                    onShowSnackBar("Registration Failed: ${navigationEvent.message}", null)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterScreenContent(
    viewData: RegisterScreenViewData,
    onIntent: (RegisterScreenIntent) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()

    val isScrollable by remember(scrollState.maxValue) {
        derivedStateOf { scrollState.maxValue > 0 }
    }
    val scrollProgressFromTop by remember(scrollState.maxValue) {
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
                (viewportSize / totalContentHeight).coerceIn(0.05f, 1f)
            } else {
                1f
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val horizontalPadding = if (isLandscape) 8.dp else 16.dp

    if (viewData.isDatePickerVisible) {
        val initialDate = try {
            java.time.LocalDate.parse(viewData.babyBirthDate)
        } catch (_: Exception) {
            java.time.LocalDate.now()
        }

        MMPDatePickerDialog(
            onDismissRequest = { onIntent(RegisterScreenIntent.SetDatePickerVisibility(false)) },
            initialDate = initialDate,
            onDateSelected = { selectedDate ->
                onIntent(RegisterScreenIntent.UpdateBabyBirthDate(selectedDate.toString()))
                onIntent(RegisterScreenIntent.SetDatePickerVisibility(false))
            }
        )
    }

    MMPScaffold(
        title = "Register Screen",
        scrollBehavior = scrollBehavior
    ) { innerPadding ->
        Surface(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            viewData.run {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(start = horizontalPadding, end = horizontalPadding, bottom = 16.dp)
                        .padding(end = if (isScrollable && isLandscape) 16.dp else 0.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Register Account",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = if (isLandscape) 8.dp else 24.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { onIntent(RegisterScreenIntent.UpdateEmail(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email Address") },
                        supportingText = { emailError?.let { Text(it) } },
                        isError = emailError != null,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { onIntent(RegisterScreenIntent.UpdateFirstName(it)) },
                            modifier = Modifier.weight(1f),
                            label = { Text("First Name") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { onIntent(RegisterScreenIntent.UpdateLastName(it)) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Last Name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = middleName,
                        onValueChange = { onIntent(RegisterScreenIntent.UpdateMiddleName(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Middle Name (Optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Roles",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("parent", "coffee", "flashcards").forEach { role ->
                            FilterChip(
                                selected = roles.contains(role),
                                onClick = { onIntent(RegisterScreenIntent.ToggleRole(role)) },
                                label = { Text(role.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    if (roles.contains("parent")) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Baby Registration Method",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = babyEntryMethod == BabyEntryMethod.BY_ID,
                                onClick = { onIntent(RegisterScreenIntent.SetBabyEntryMethod(BabyEntryMethod.BY_ID)) },
                                label = { Text("Existing Baby ID") }
                            )
                            FilterChip(
                                selected = babyEntryMethod == BabyEntryMethod.BY_DETAILS,
                                onClick = { onIntent(RegisterScreenIntent.SetBabyEntryMethod(BabyEntryMethod.BY_DETAILS)) },
                                label = { Text("New Baby Profile") }
                            )
                        }

                        if (babyEntryMethod == BabyEntryMethod.BY_ID) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = babyId,
                                onValueChange = { onIntent(RegisterScreenIntent.UpdateBabyId(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Existing Baby ID *") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null
                                    )
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            )
                        }

                        if (babyEntryMethod == BabyEntryMethod.BY_DETAILS) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = babyFirstName,
                                    onValueChange = {
                                        onIntent(
                                            RegisterScreenIntent.UpdateBabyFirstName(
                                                it
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Baby First Name *") },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = babyLastName,
                                    onValueChange = {
                                        onIntent(
                                            RegisterScreenIntent.UpdateBabyLastName(
                                                it
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Baby Last Name *") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = babyMiddleName,
                                onValueChange = {
                                    onIntent(
                                        RegisterScreenIntent.UpdateBabyMiddleName(
                                            it
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Baby Middle Name (Optional)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = babyBirthDate,
                                onValueChange = { },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Baby Birth Date *") },
                                readOnly = true,
                                singleLine = true,
                                leadingIcon = {
                                    IconButton(onClick = { onIntent(RegisterScreenIntent.SetDatePickerVisibility(true)) }) {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = "Select Date"
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                placeholder = { Text("YYYY-MM-DD") }
                            )
                        }

                        babyError?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { onIntent(RegisterScreenIntent.UpdatePassword(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        supportingText = { passwordError?.let { Text(it) } },
                        isError = passwordError != null,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { onIntent(RegisterScreenIntent.UpdatePasswordVisibility) }) {
                                Icon(
                                    if (isPasswordVisible) Icons.Default.Lock else Icons.Outlined.Lock,
                                    null
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = passwordConfirmation,
                        onValueChange = {
                            onIntent(
                                RegisterScreenIntent.UpdatePasswordConfirmation(
                                    it
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { onIntent(RegisterScreenIntent.UpdatePasswordConfirmationVisibility) }) {
                                Icon(
                                    if (isPasswordConfirmationVisible) Icons.Default.Lock else Icons.Outlined.Lock,
                                    null
                                )
                            }
                        },
                        visualTransformation = if (isPasswordConfirmationVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        supportingText = {
                            if (passwordConfirmation.isNotEmpty() && password != passwordConfirmation) {
                                Text("Passwords do not match")
                            }
                        },
                        isError = passwordConfirmation.isNotEmpty() && password != passwordConfirmation
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    generalError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = { onIntent(RegisterScreenIntent.Register) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = password == passwordConfirmation && password.isNotEmpty() && email.isNotEmpty() && firstName.isNotEmpty() && lastName.isNotEmpty() && roles.isNotEmpty() && !isLoading
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
                val minThumbVisualHeightDp = 20.dp

                Box(
                    contentAlignment = Alignment.CenterEnd,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp, vertical = 12.dp)
                ) {
                    BoxWithConstraints(contentAlignment = Alignment.CenterEnd) {
                        val trackActualHeightDp = this.maxHeight
                        val trackActualHeightPx = with(density) { trackActualHeightDp.toPx() }
                        val thumbHeightDp =
                            (trackActualHeightDp * visiblePortionFraction).coerceAtLeast(
                                minThumbVisualHeightDp
                            )
                        val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
                        val movableRangePx = trackActualHeightPx - thumbHeightPx
                        val thumbOffsetYPx =
                            (movableRangePx * scrollProgressFromTop).coerceAtLeast(0f)
                        val thumbOffsetYDp = with(density) { thumbOffsetYPx.toDp() }

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(scrollbarWidth)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .width(scrollbarWidth)
                                    .height(thumbHeightDp)
                                    .offset(y = thumbOffsetYDp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
