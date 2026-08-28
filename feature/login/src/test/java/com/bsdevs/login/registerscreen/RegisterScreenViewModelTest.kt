package com.bsdevs.login.registerscreen

import app.cash.turbine.test
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.network.repository.UserRepository
import io.mockk.mockk
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
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: RegisterScreenViewModel
    private lateinit var dispatchers: DispatcherProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

        accountService = FakeAccountService()
        userRepository = mockk(relaxed = true)
        viewModel = RegisterScreenViewModel(accountService, userRepository, dispatchers)
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
    fun `register success navigates to SuccessfulAccountCreation`() = runTest {
        ensureReady()
        viewModel.processIntent(RegisterScreenIntent.UpdateEmail("test@example.com"))
        viewModel.processIntent(RegisterScreenIntent.UpdatePassword("password"))

        viewModel.navigationEvent.test {
            viewModel.processIntent(RegisterScreenIntent.Register)
            assertEquals(RegisterNavigationEvent.SuccessfulAccountCreation, awaitItem())
        }
        advanceUntilIdle()
        assertEquals("test@example.com", accountService.lastSignedUpEmail)
    }

    @Test
    fun `navigateToLogin intent emits NavigateToLogin event`() = runTest {
        ensureReady()
        viewModel.navigationEvent.test {
            viewModel.processIntent(RegisterScreenIntent.NavigateToLogin)
            assertEquals(RegisterNavigationEvent.NavigateToLogin, awaitItem())
        }
    }

    @Test
    fun `updateFirstName updates viewData correctly`() = runTest {
        ensureReady()
        viewModel.processIntent(RegisterScreenIntent.UpdateFirstName("John"))

        val result = viewModel.viewData.value as Result.Success
        assertEquals("John", result.data.firstName)
    }

    @Test
    fun `updateLastName updates viewData correctly`() = runTest {
        ensureReady()
        viewModel.processIntent(RegisterScreenIntent.UpdateLastName("Doe"))

        val result = viewModel.viewData.value as Result.Success
        assertEquals("Doe", result.data.lastName)
    }

    @Test
    fun `updateMiddleName updates viewData correctly`() = runTest {
        ensureReady()
        viewModel.processIntent(RegisterScreenIntent.UpdateMiddleName("Quincy"))

        val result = viewModel.viewData.value as Result.Success
        assertEquals("Quincy", result.data.middleName)
    }

    @Test
    fun `toggleRole adds and removes roles correctly`() = runTest {
        ensureReady()
        viewModel.processIntent(RegisterScreenIntent.ToggleRole("parent"))
        var result = viewModel.viewData.value as Result.Success
        assertTrue(result.data.roles.contains("parent"))

        viewModel.processIntent(RegisterScreenIntent.ToggleRole("caregiver"))
        result = viewModel.viewData.value as Result.Success
        assertTrue(result.data.roles.contains("parent"))
        assertTrue(result.data.roles.contains("caregiver"))

        viewModel.processIntent(RegisterScreenIntent.ToggleRole("parent"))
        result = viewModel.viewData.value as Result.Success
        assertFalse(result.data.roles.contains("parent"))
        assertTrue(result.data.roles.contains("caregiver"))
    }

    @Test
    fun `updateBabyFields updates viewData correctly`() = runTest {
        ensureReady()
        viewModel.processIntent(RegisterScreenIntent.SetBabyEntryMethod(BabyEntryMethod.BY_ID))
        viewModel.processIntent(RegisterScreenIntent.UpdateBabyId("baby123"))
        
        var result = viewModel.viewData.value as Result.Success
        assertEquals("baby123", result.data.babyId)

        viewModel.processIntent(RegisterScreenIntent.SetBabyEntryMethod(BabyEntryMethod.BY_DETAILS))
        viewModel.processIntent(RegisterScreenIntent.UpdateBabyFirstName("Baby"))
        viewModel.processIntent(RegisterScreenIntent.UpdateBabyLastName("Boy"))
        viewModel.processIntent(RegisterScreenIntent.UpdateBabyMiddleName("M"))
        viewModel.processIntent(RegisterScreenIntent.UpdateBabyBirthDate("2023-01-01"))

        result = viewModel.viewData.value as Result.Success
        assertEquals("Baby", result.data.babyFirstName)
        assertEquals("Boy", result.data.babyLastName)
        assertEquals("M", result.data.babyMiddleName)
        assertEquals("2023-01-01", result.data.babyBirthDate)
    }

    @Test
    fun `register with parent role and no method selected shows error`() = runTest {
        ensureReady()
        viewModel.processIntent(RegisterScreenIntent.ToggleRole("parent"))
        // No method selected (defaults to NONE)
        
        viewModel.processIntent(RegisterScreenIntent.Register)
        
        val result = viewModel.viewData.value as Result.Success
        assertEquals("Please choose how to add your baby.", result.data.babyError)
        assertEquals(0, accountService.signUpCallCount)
    }

    @Test
    fun `register success with parent role and new baby info saves baby and user`() = runTest {
        ensureReady()
        viewModel.processIntent(RegisterScreenIntent.UpdateEmail("test@example.com"))
        viewModel.processIntent(RegisterScreenIntent.UpdatePassword("password"))
        viewModel.processIntent(RegisterScreenIntent.ToggleRole("parent"))
        viewModel.processIntent(RegisterScreenIntent.SetBabyEntryMethod(BabyEntryMethod.BY_DETAILS))
        viewModel.processIntent(RegisterScreenIntent.UpdateBabyFirstName("Baby"))
        viewModel.processIntent(RegisterScreenIntent.UpdateBabyLastName("Boy"))
        viewModel.processIntent(RegisterScreenIntent.UpdateBabyBirthDate("2023-01-01"))

        viewModel.navigationEvent.test {
            viewModel.processIntent(RegisterScreenIntent.Register)
            assertEquals(RegisterNavigationEvent.SuccessfulAccountCreation, awaitItem())
        }
        
        io.mockk.coVerify { userRepository.saveBaby(any()) }
        io.mockk.coVerify { userRepository.saveUser(any()) }
    }

    @Test
    fun `register success with existing babyId verifies baby exists and saves user`() = runTest {
        ensureReady()
        io.mockk.coEvery { userRepository.babyExists("existing_baby") } returns true
        
        viewModel.processIntent(RegisterScreenIntent.UpdateEmail("test@example.com"))
        viewModel.processIntent(RegisterScreenIntent.UpdatePassword("password"))
        viewModel.processIntent(RegisterScreenIntent.ToggleRole("parent"))
        viewModel.processIntent(RegisterScreenIntent.SetBabyEntryMethod(BabyEntryMethod.BY_ID))
        viewModel.processIntent(RegisterScreenIntent.UpdateBabyId("existing_baby"))

        viewModel.navigationEvent.test {
            viewModel.processIntent(RegisterScreenIntent.Register)
            assertEquals(RegisterNavigationEvent.SuccessfulAccountCreation, awaitItem())
        }
        
        io.mockk.coVerify { userRepository.babyExists("existing_baby") }
        io.mockk.coVerify { userRepository.saveUser(match { it.babyId == "existing_baby" }) }
        io.mockk.coVerify(exactly = 0) { userRepository.saveBaby(any()) }
    }

    @Test
    fun `register failure updates error messages and sends failure event`() = runTest {
        ensureReady()
        accountService.shouldSucceed = false
        
        viewModel.navigationEvent.test {
            viewModel.processIntent(RegisterScreenIntent.Register)
            val event = awaitItem()
            assertTrue(event is RegisterNavigationEvent.Failure)
        }

        val result = viewModel.viewData.value as Result.Success
        assertFalse(result.data.isLoading)
        // General error is set if message doesn't contain "email" or "password"
        assertNotNull(result.data.generalError)
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
