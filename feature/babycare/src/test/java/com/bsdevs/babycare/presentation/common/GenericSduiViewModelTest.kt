package com.bsdevs.babycare.presentation.common

import app.cash.turbine.test
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.core.domain.BabyCareRepository
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.ScreenDataMapper
import com.bsdevs.data.repository.ScreenRepository
import com.bsdevs.network.dto.ScreenDto
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var mapper: ScreenDataMapper
    private lateinit var babyRepository: BabyCareRepository
    private lateinit var accountService: AccountService
    private lateinit var dispatchers: DispatcherProvider
    private lateinit var viewModel: GenericSduiViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        screenRepository = mockk()
        mapper = mockk()
        babyRepository = mockk(relaxed = true)
        accountService = mockk(relaxed = true)
        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }
        viewModel = GenericSduiViewModel(
            screenRepository, mapper, dispatchers, babyRepository, accountService,
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
        every { mapper.mapToData(dtos) } returns mappedData

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
