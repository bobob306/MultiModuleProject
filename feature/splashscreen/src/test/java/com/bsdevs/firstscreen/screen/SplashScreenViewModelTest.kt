package com.bsdevs.firstscreen.screen

import app.cash.turbine.test
import com.bsdevs.authentication.AccountService
import com.bsdevs.network.dto.UserDto
import com.bsdevs.network.repository.UserRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var accountService: AccountService
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: SplashScreenViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountService = mockk()
        userRepository = mockk()
        viewModel = SplashScreenViewModel(accountService, userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when user is not authenticated, navigate to sign in screen`() = runTest {
        every { accountService.hasUser() } returns false

        viewModel.navigationEvent.test {
            viewModel.onAppStart()
            assertEquals(SplashScreenNavigationEvents.NavigateToSignInScreen, awaitItem())
        }
    }

    @Test
    fun `when user is authenticated and profile exists, navigate to home screen`() = runTest {
        val userId = "test_uid"
        every { accountService.hasUser() } returns true
        every { accountService.currentUserId } returns userId
        coEvery { userRepository.getUser(userId) } returns UserDto(id = userId)

        viewModel.navigationEvent.test {
            viewModel.onAppStart()
            assertEquals(SplashScreenNavigationEvents.NavigateToHomeScreen, awaitItem())
        }
    }

    @Test
    fun `when user is authenticated but profile missing, navigate to home screen`() = runTest {
        val userId = "test_uid"
        every { accountService.hasUser() } returns true
        every { accountService.currentUserId } returns userId
        coEvery { userRepository.getUser(userId) } returns null

        viewModel.navigationEvent.test {
            viewModel.onAppStart()
            assertEquals(SplashScreenNavigationEvents.NavigateToHomeScreen, awaitItem())
        }
    }
}
