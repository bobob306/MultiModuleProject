package com.bsdevs.babycare.presentation.temperature

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.cash.turbine.test
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.DailyLogDto
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.babycare.presentation.home.FakeAccountService
import com.bsdevs.babycare.presentation.navigation.TemperatureRoute
import com.bsdevs.common.DispatcherProvider
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TemperatureViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var accountService: FakeAccountService
    private lateinit var repository: BabyCareRepository
    private lateinit var viewModel: TemperatureViewModel
    private lateinit var dispatchers: DispatcherProvider
    private lateinit var savedStateHandle: SavedStateHandle

    private val userId = "testUser"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("androidx.navigation.SavedStateHandleKt")

        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

        accountService = FakeAccountService(userId)
        repository = mockk(relaxed = true)
        every { repository.cachedDays } returns MutableStateFlow(emptyList())
        
        savedStateHandle = mockk(relaxed = true)
        every { savedStateHandle.toRoute<TemperatureRoute>() } returns TemperatureRoute(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial load observes repository cache`() = runTest {
        val dailyLogs = listOf(
            DailyLogDto("2026-08-27", userId, listOf(
                UnifiedEventDto(id = "t1", type = "TEMPERATURE", temperature = 37.5, time = "10:00")
            ))
        )
        val cache = MutableStateFlow(dailyLogs)
        every { repository.cachedDays } returns cache

        viewModel = TemperatureViewModel(accountService, repository, dispatchers, savedStateHandle)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.dates.size)
            assertEquals("2026-08-27", state.dates.first())
            assertEquals(1, state.dailyReadings["2026-08-27"]?.size)
        }
    }

    @Test
    fun `editing existing temperature loads data correctly`() = runTest {
        val eventId = "t123"
        val event = UnifiedEventDto(
            id = eventId,
            type = "TEMPERATURE",
            temperature = 38.2,
            time = "11:30",
            dateTimeString = "2026-08-27 11:30"
        )
        coEvery { repository.getFeedingEventById(userId, eventId) } returns event
        every { savedStateHandle.toRoute<TemperatureRoute>() } returns TemperatureRoute(eventId)

        viewModel = TemperatureViewModel(accountService, repository, dispatchers, savedStateHandle)

        viewModel.uiState.test {
            // Wait for data to load
            var state = awaitItem()
            while (state.isLoading) { state = awaitItem() }
            
            assertEquals("38.2", state.temperature)
            assertEquals(382, state.temperatureValue)
            assertEquals("11:30", state.time)
        }
    }

    @Test
    fun `submitTemperature saves new entry`() = runTest {
        viewModel = TemperatureViewModel(accountService, repository, dispatchers, savedStateHandle)
        
        viewModel.onTemperatureValueSelected(366) // 36.6
        viewModel.onDateSelected("2026-08-27")
        viewModel.onTimeSelected(12, 0)
        
        viewModel.events.test {
            viewModel.submitTemperature()
            
            coVerify { repository.saveActivityEvent(userId, "2026-08-27", match { 
                it.type == "TEMPERATURE" && it.temperature == 36.6 
            }) }
            assertEquals(TemperatureUiEffect.SaveSuccess, awaitItem())
        }
    }

    @Test
    fun `deleteTemperature calls repository and emits success`() = runTest {
        val eventId = "t1"
        val event = UnifiedEventDto(id = eventId, type = "TEMPERATURE", time = "10:00", dateTimeString = "2026-08-27 10:00")
        coEvery { repository.getFeedingEventById(userId, eventId) } returns event
        every { savedStateHandle.toRoute<TemperatureRoute>() } returns TemperatureRoute(eventId)
        
        viewModel = TemperatureViewModel(accountService, repository, dispatchers, savedStateHandle)
        
        viewModel.events.test {
            viewModel.deleteTemperature()
            
            coVerify { repository.deleteActivityEvent(userId, any(), eventId) }
            assertEquals(TemperatureUiEffect.DeleteSuccess, awaitItem())
        }
    }

    @Test
    fun `setShowSheet updates uiState`() = runTest {
        viewModel = TemperatureViewModel(accountService, repository, dispatchers, savedStateHandle)
        viewModel.setShowSheet(true)
        assertTrue(viewModel.uiState.value.showSheet)
    }

    @Test
    fun `setShowDatePicker updates uiState`() = runTest {
        viewModel = TemperatureViewModel(accountService, repository, dispatchers, savedStateHandle)
        viewModel.setShowDatePicker(true)
        assertTrue(viewModel.uiState.value.showDatePicker)
    }
}
