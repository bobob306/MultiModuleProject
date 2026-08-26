package com.bsdevs.login.loginscreen

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
class LoginScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var accountService: FakeAccountService
    private lateinit var viewModel: LoginScreenViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountService = FakeAccountService()
        viewModel = LoginScreenViewModel(accountService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun ensureReady() {
        // Collect once to trigger onStart and get to Success state
        viewModel.viewData.filterIsInstance<Result.Success<LoginViewData>>().first()
    }

    @Test
    fun `initial state is Success with empty fields`() = runTest {
        ensureReady()
        val result = viewModel.viewData.value
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("", data.email)
        assertEquals("", data.password)
    }

    @Test
    fun `updateEmail updates viewData correctly`() = runTest {
        ensureReady()
        viewModel.processIntent(LoginScreenIntent.UpdateEmail("test@example.com"))

        val result = viewModel.viewData.value as Result.Success
        assertEquals("test@example.com", result.data.email)
    }

    @Test
    fun `updatePassword updates viewData and hides visibility if empty`() = runTest {
        ensureReady()
        viewModel.processIntent(LoginScreenIntent.UpdatePassword("pass123"))
        var result = viewModel.viewData.value as Result.Success
        assertEquals("pass123", result.data.password)

        viewModel.processIntent(LoginScreenIntent.UpdatePassword(""))
        result = viewModel.viewData.value as Result.Success
        assertFalse(result.data.isPasswordVisible)
    }

    @Test
    fun `togglePasswordVisibility works correctly`() = runTest {
        ensureReady()
        viewModel.processIntent(LoginScreenIntent.UpdatePassword("secret"))
        
        viewModel.processIntent(LoginScreenIntent.UpdatePasswordVisibility)
        var result = viewModel.viewData.value as Result.Success
        assertTrue(result.data.isPasswordVisible)

        viewModel.processIntent(LoginScreenIntent.UpdatePasswordVisibility)
        result = viewModel.viewData.value as Result.Success
        assertFalse(result.data.isPasswordVisible)
    }

    @Test
    fun `login success navigates to CoffeeHome`() = runTest {
        ensureReady()
        viewModel.processIntent(LoginScreenIntent.UpdateEmail("test@example.com"))
        viewModel.processIntent(LoginScreenIntent.UpdatePassword("password"))

        viewModel.navigationEvent.test {
            viewModel.processIntent(LoginScreenIntent.Login)
            assertEquals(NavigationEvent.NavigateToCoffeeHome, awaitItem())
        }
        advanceUntilIdle()
        assertEquals("test@example.com", accountService.lastSignedInEmail)
    }

    @Test
    fun `login failure updates error message and stops loading`() = runTest {
        ensureReady()
        accountService.shouldSucceed = false
        viewModel.processIntent(LoginScreenIntent.UpdateEmail("wrong@example.com"))
        viewModel.processIntent(LoginScreenIntent.UpdatePassword("wrong"))

        viewModel.processIntent(LoginScreenIntent.Login)
        advanceUntilIdle()

        val result = viewModel.viewData.value as Result.Success
        assertFalse(result.data.isLoading)
        assertNotNull(result.data.emailError)
    }

    @Test
    fun `register click triggers navigation`() = runTest {
        viewModel.navigationEvent.test {
            viewModel.processIntent(LoginScreenIntent.Register)
            assertEquals(NavigationEvent.NavigateToRegister, awaitItem())
        }
    }

    @Test
    fun `typing email clears existing email error`() = runTest {
        ensureReady()
        accountService.shouldSucceed = false

        viewModel.viewData.test {
            skipItems(1) // current state

            viewModel.processIntent(LoginScreenIntent.Login)
            
            // Skip loading state
            skipItems(1)
            
            // await error state
            val errorState = awaitItem() as Result.Success
            assertNotNull(errorState.data.emailError)

            viewModel.processIntent(LoginScreenIntent.UpdateEmail("new@email.com"))

            val clearedState = awaitItem() as Result.Success
            assertNull(clearedState.data.emailError)
            assertEquals("new@email.com", clearedState.data.email)
        }
    }

    @Test
    fun `isLoading is true while login is in progress`() = runTest {
        ensureReady()
        // Start collecting navigation events to prevent blocking
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { 
            viewModel.navigationEvent.collect {} 
        }

        viewModel.viewData.test {
            // Initial success state
            skipItems(1)

            viewModel.processIntent(LoginScreenIntent.Login)

            val loadingItem = awaitItem() as Result.Success
            assertTrue(loadingItem.data.isLoading)

            val finalItem = awaitItem() as Result.Success
            assertFalse(finalItem.data.isLoading)
        }
    }

    @Test
    fun `clearing password resets visibility to false`() = runTest {
        ensureReady()
        viewModel.processIntent(LoginScreenIntent.UpdatePassword("password"))
        viewModel.processIntent(LoginScreenIntent.UpdatePasswordVisibility)
        assertTrue((viewModel.viewData.value as Result.Success).data.isPasswordVisible)

        viewModel.processIntent(LoginScreenIntent.UpdatePassword(""))

        assertFalse((viewModel.viewData.value as Result.Success).data.isPasswordVisible)
    }
}
