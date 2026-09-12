package com.bsdevs.babycare.presentation.common

import app.cash.turbine.test
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.core.domain.BabyCareRepository
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.ScreenDataMapper
import com.bsdevs.data.repository.ScreenRepository
import com.bsdevs.data.repository.UserRepository
import com.bsdevs.network.dto.ScreenDto
import com.bsdevs.network.dto.UserDto
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GenericSduiViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var screenRepository: ScreenRepository
    private lateinit var userRepository: UserRepository
    private lateinit var mapper: ScreenDataMapper
    private lateinit var babyRepository: BabyCareRepository
    private lateinit var accountService: AccountService
    private lateinit var dispatchers: DispatcherProvider
    private lateinit var viewModel: GenericSduiViewModel
    private val userProfileFlow = MutableStateFlow<UserDto?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        screenRepository = mockk()
        userRepository = mockk()
        mapper = mockk()
        babyRepository = mockk(relaxed = true)
        accountService = mockk(relaxed = true)
        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

        every { userRepository.userProfile } returns userProfileFlow

        viewModel = GenericSduiViewModel(
            screenRepository, userRepository, mapper, dispatchers, babyRepository, accountService,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `getUiState returns loading then success`() = runTest {
        val screenId = "test_screen"
        val dtos = listOf(ScreenDto.TitleDto(0, "Title"))
        val mappedData = listOf(mockk<NetworkScreenData>())

        coEvery { screenRepository.getScreenFlow(screenId) } returns flowOf(Result.Success(dtos))
        every { mapper.mapToData(any()) } returns mappedData

        viewModel.getUiState(screenId).test {
            val first = awaitItem()
            if (first is Result.Loading) {
                val second = awaitItem() as Result.Success
                assertEquals(mappedData, second.data)
            } else {
                val success = first as Result.Success
                assertEquals(mappedData, success.data)
            }
        }
    }

    @Test
    fun `getUiState filters by role`() = runTest {
        val screenId = "test_screen"
        val dtos = listOf(
            ScreenDto.TitleDto(0, "Admin Title", requiredRoles = listOf("admin")),
            ScreenDto.TitleDto(1, "User Title", requiredRoles = listOf("user"))
        )
        val mappedData = listOf(mockk<NetworkScreenData>())

        userProfileFlow.value = UserDto(roles = listOf("user"))
        coEvery { screenRepository.getScreenFlow(screenId) } returns flowOf(Result.Success(dtos))
        
        val captor = slot<List<ScreenDto>>()
        every { mapper.mapToData(capture(captor)) } returns mappedData

        viewModel.getUiState(screenId).test {
            awaitItem() // Skip Loading or initial Result.Success
            
            assertEquals(1, captor.captured.size)
            assertEquals("User Title", (captor.captured[0] as ScreenDto.TitleDto).content)
        }
    }


    @Test
    fun `refresh triggers repository updates`() = runTest {
        val screenId = "test_screen"
        val userId = "user123"
        every { accountService.currentUserId } returns userId
        coEvery { screenRepository.getScreenFlow(screenId, forceRefresh = true) } returns flowOf(Result.Loading)

        viewModel.refresh(screenId)

        coVerify { screenRepository.getScreenFlow(screenId, forceRefresh = true) }
        coVerify { babyRepository.refreshData(userId, 20) }
    }
}
