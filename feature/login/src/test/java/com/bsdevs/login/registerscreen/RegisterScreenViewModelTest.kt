package com.bsdevs.login.registerscreen

import app.cash.turbine.test
import com.bsdevs.common.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var accountService: FakeAccountService
    private lateinit var viewModel: RegisterScreenViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountService = FakeAccountService()
        viewModel = RegisterScreenViewModel(accountService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun ensureReady() {
        viewModel.viewData.filterIsInstance<Result.Success<RegisterScreenViewData>>().first()
    }

    @Test
    fun `initial state is Success with empty fields`() = runTest {
        ensureReady()
        val result = viewModel.viewData.value
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("", data.email)
        assertEquals("", data.password)
        assertEquals("", data.passwordConfirmation)
    }

    @Test
    fun `updateEmail updates viewData correctly`() = runTest {
        ensureReady()
        viewModel.processIntent(RegisterScreenIntent.UpdateEmail("new@example.com"))

        val result = viewModel.viewData.value as Result.Success
        assertEquals("new@example.com", result.data.email)
    }

    @Test
    fun `updatePassword updates viewData correctly`() = runTest {
        ensureReady()
        viewModel.processIntent(RegisterScreenIntent.UpdatePassword("pass123"))

        val result = viewModel.viewData.value as Result.Success
        assertEquals("pass123", result.data.password)
    }

    @Test
    fun `updatePasswordConfirmation updates viewData correctly`() = runTest {
        ensureReady()
        viewModel.processIntent(RegisterScreenIntent.UpdatePasswordConfirmation("pass123"))

        val result = viewModel.viewData.value as Result.Success
        assertEquals("pass123", result.data.passwordConfirmation)
    }

    @Test
    fun `togglePasswordVisibility works correctly`() = runTest {
        ensureReady()
        viewModel.processIntent(RegisterScreenIntent.UpdatePasswordVisibility)
        assertTrue((viewModel.viewData.value as Result.Success).data.isPasswordVisible)

        viewModel.processIntent(RegisterScreenIntent.UpdatePasswordVisibility)
        assertFalse((viewModel.viewData.value as Result.Success).data.isPasswordVisible)
    }

    @Test
    fun `togglePasswordConfirmationVisibility works correctly`() = runTest {
        ensureReady()
        viewModel.processIntent(RegisterScreenIntent.UpdatePasswordConfirmationVisibility)
        assertTrue((viewModel.viewData.value as Result.Success).data.isPasswordConfirmationVisible)

        viewModel.processIntent(RegisterScreenIntent.UpdatePasswordConfirmationVisibility)
        assertFalse((viewModel.viewData.value as Result.Success).data.isPasswordConfirmationVisible)
    }

    @Test
    fun `register success navigates to Login`() = runTest {
        ensureReady()
        viewModel.processIntent(RegisterScreenIntent.UpdateEmail("test@example.com"))
        viewModel.processIntent(RegisterScreenIntent.UpdatePassword("password"))

        viewModel.navigationEvent.test {
            viewModel.processIntent(RegisterScreenIntent.Register)
            assertEquals(RegisterNavigationEvent.NavigateToLogin, awaitItem())
        }
        advanceUntilIdle()
        assertEquals("test@example.com", accountService.lastSignedUpEmail)
    }

    @Test
    fun `register failure updates error messages and stops loading`() = runTest {
        ensureReady()
        accountService.shouldSucceed = false
        
        viewModel.processIntent(RegisterScreenIntent.Register)
        advanceUntilIdle()

        val result = viewModel.viewData.value as Result.Success
        assertFalse(result.data.isLoading)
        assertNotNull(result.data.emailError)
        assertNotNull(result.data.passwordError)
    }

    @Test
    fun `isLoading is true while registration is in progress`() = runTest {
        ensureReady()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.navigationEvent.collect {}
        }

        viewModel.viewData.test {
            skipItems(1) // Initial state

            viewModel.processIntent(RegisterScreenIntent.Register)

            val loadingItem = awaitItem() as Result.Success
            assertTrue(loadingItem.data.isLoading)

            val finalItem = awaitItem() as Result.Success
            assertFalse(finalItem.data.isLoading)
        }
    }
}
