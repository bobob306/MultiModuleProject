package com.bsdevs.homescreen.presentation.settings

import app.cash.turbine.test
import com.bsdevs.authentication.AccountService
import com.bsdevs.network.dto.BabyDto
import com.bsdevs.network.dto.UserDto
import com.bsdevs.network.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var userRepository: UserRepository
    private lateinit var accountService: AccountService
    private lateinit var viewModel: SettingsViewModel

    private val userId = "test_uid"
    private val babyId = "baby_uid"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk(relaxed = true)
        accountService = mockk()

        every { accountService.currentUserId } returns userId
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadUserData fetches user and baby data successfully`() = runTest {
        val user = UserDto(id = userId, firstName = "John", lastName = "Doe", babyId = babyId)
        val baby = BabyDto(id = babyId, firstName = "Junior")

        coEvery { userRepository.getUser(userId) } returns user
        coEvery { userRepository.getBaby(babyId) } returns baby

        viewModel = SettingsViewModel(userRepository, accountService)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("John Doe", state.userName)
            assertEquals("Junior", state.babyName)
        }
    }

    @Test
    fun `deleteAccount calls userRepository and accountService then updates state`() = runTest {
        coEvery { userRepository.getUser(userId) } returns UserDto(id = userId)
        coEvery { userRepository.deleteUserData(any()) } returns Unit
        coEvery { accountService.deleteAccount() } returns Unit
        
        viewModel = SettingsViewModel(userRepository, accountService)

        viewModel.uiState.test {
            // Skip initial emissions from loadUserData
            var lastState = awaitItem()
            while (lastState.isLoading || lastState.userName.isEmpty()) {
                lastState = awaitItem()
            }

            viewModel.deleteAccount()

            // Capture all emissions until accountDeleted is true
            var foundDeleted = false
            
            val nextState = awaitItem()
            if (nextState.isLoading) {
                val finalState = awaitItem()
                if (finalState.accountDeleted) foundDeleted = true
            } else if (nextState.accountDeleted) {
                foundDeleted = true
            }
            
            assertTrue("Should have reached accountDeleted state", foundDeleted)
        }

        coVerify { userRepository.deleteUserData(userId) }
        coVerify { accountService.deleteAccount() }
    }
}
