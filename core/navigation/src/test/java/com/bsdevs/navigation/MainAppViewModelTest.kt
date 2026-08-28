package com.bsdevs.navigation

import app.cash.turbine.test
import com.bsdevs.network.dto.UserDto
import com.bsdevs.network.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainAppViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: MainAppViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `userRoles reflects roles from userRepository profile`() = runTest {
        val userProfile = MutableStateFlow<UserDto?>(null)
        every { userRepository.userProfile } returns userProfile

        viewModel = MainAppViewModel(userRepository)

        viewModel.userRoles.test {
            assertEquals(emptyList<String>(), awaitItem())

            userProfile.value = UserDto(roles = listOf("admin", "parent"))
            assertEquals(listOf("admin", "parent"), awaitItem())

            userProfile.value = null
            assertEquals(emptyList<String>(), awaitItem())
        }
    }
}
