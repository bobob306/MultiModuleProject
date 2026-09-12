package com.bsdevs.babycare.presentation.graph

import app.cash.turbine.test
import com.bsdevs.babycare.core.data.BabyCareRepositoryImpl
import com.bsdevs.babycare.core.testing.FakeBabyCareFirestoreService
import com.bsdevs.common.TimeProvider
import com.bsdevs.data.SyncManager
import com.bsdevs.data.local.dao.BabyEventDao
import java.time.LocalDate
import androidx.lifecycle.SavedStateHandle
import com.bsdevs.network.dto.BabyEvent
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.repository.UserRepository
import com.bsdevs.network.dto.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class BabyGraphViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeService: FakeBabyCareFirestoreService
    private lateinit var repository: BabyCareRepositoryImpl
    private lateinit var viewModel: BabyGraphViewModel
    private lateinit var dispatchers: DispatcherProvider

    private val userId = "testUser"

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        Dispatchers.setMain(testDispatcher)
        
        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

        fakeService = FakeBabyCareFirestoreService()
        val userRepository = mockk<UserRepository>(relaxed = true)
        every { userRepository.userProfile } returns MutableStateFlow(UserDto(id = userId, babyId = "baby1"))
        
        val timeProvider = mockk<TimeProvider>(relaxed = true)
        every { timeProvider.currentLocalDate() } returns LocalDate.of(2026, 9, 1)
        
        val babyEventDao = mockk<BabyEventDao>(relaxed = true)
        every { babyEventDao.getEvents(any()) } returns flowOf(emptyList())
        val syncManager = mockk<SyncManager>(relaxed = true)
        
        repository = BabyCareRepositoryImpl(
            apiService = fakeService, 
            userRepository = userRepository, 
            dispatchers = dispatchers, 
            timeProvider = timeProvider,
            babyEventDao = babyEventDao,
            syncManager = syncManager
        )
        viewModel = BabyGraphViewModel(repository, dispatchers, SavedStateHandle())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState has default values`() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.hourlyCounts.all { it.count == 0 })
        assertEquals(0, state.totalFeedsInCache)
    }

    @Test
    fun `uiState reflects repository data`() = runTest {
        viewModel.uiState.test {
            // Wait for initial (isLoading = true) or default
            awaitItem()
            
            val date = "2026-08-26"
            repository.saveActivityEvent(userId, date, BabyEvent.Feeding(id = "e1", time = "10:00", dateTimeString = "$date 10:00", totalDuration = 600L))
            
            var state = awaitItem()
            while (state.totalFeedsInCache < 1) {
                state = awaitItem()
            }
            assertEquals(1, state.totalFeedsInCache)
        }
    }
}
