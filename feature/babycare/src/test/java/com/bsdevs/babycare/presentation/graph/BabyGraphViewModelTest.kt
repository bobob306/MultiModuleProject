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
        Dispatchers.setMain(testDispatcher)
        
        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

        fakeService = FakeBabyCareFirestoreService()
        val userRepository = mockk<UserRepository>(relaxed = true)
        every { userRepository.userProfile } returns MutableStateFlow(UserDto(id = userId))
        
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
        assertNull(state.analysisResult)
        assertTrue(state.dailyAverageGaps.isEmpty())
    }

    @Test
    fun `uiState updates when repository emits data`() = runTest {
        // Given
        val date = "2026-08-26"
        val event1 = BabyEvent.Feeding(
            id = "e1",
            time = "10:00",
            dateTimeString = "$date 10:00",
            totalDuration = 600L // 10 mins
        )
        val event2 = BabyEvent.Feeding(
            id = "e2",
            time = "14:00",
            dateTimeString = "$date 14:00",
            totalDuration = 600L // 10 mins
        )
        
        // When
        repository.saveActivityEvent(userId, date, event1)
        repository.saveActivityEvent(userId, date, event2)

        // Then
        viewModel.uiState.test {
            // Wait for the state with 2 events
            val state = expectMostRecentItem()
            assertEquals(2, state.totalFeedsInCache)
            assertEquals(1, state.hourlyCounts.first { it.hour == 10 }.count)
            assertEquals(1, state.hourlyCounts.first { it.hour == 14 }.count)
            
            assertNotNull(state.analysisResult)
            assertEquals(1, state.analysisResult?.bucketGaps?.first { it.rangeLabel == "0-10 min" }?.totalCount)
            assertEquals(240, state.analysisResult?.bucketGaps?.first { it.rangeLabel == "0-10 min" }?.averageGapMinutes)
        }
    }

    @Test
    fun `calculateDailyAverageGaps computes rolling averages correctly`() = runTest {
        // Given feedings over multiple days to trigger rolling average (needs >= 3 days for current logic)
        val days = listOf("2026-08-01", "2026-08-02", "2026-08-03")
        days.forEachIndexed { index, date ->
            val e1 = BabyEvent.Feeding(id = "a$index", time = "08:00", dateTimeString = "$date 08:00")
            val e2 = BabyEvent.Feeding(id = "b$index", time = "12:00", dateTimeString = "$date 12:00")
            repository.saveActivityEvent(userId, date, e1)
            repository.saveActivityEvent(userId, date, e2)
        }

        // Then
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            
            assertEquals(3, state.dailyAverageGaps.size)
            // Day 1 and 2 should have null rolling average
            assertNull(state.dailyAverageGaps[0].rolling14DayAverageMinutes)
            assertNull(state.dailyAverageGaps[1].rolling14DayAverageMinutes)
            // Day 3 should have a value
            assertNotNull(state.dailyAverageGaps[2].rolling14DayAverageMinutes)
            assertEquals(240, state.dailyAverageGaps[2].averageGapMinutes)
            assertEquals(240, state.dailyAverageGaps[2].rolling14DayAverageMinutes)
        }
    }

    @Test
    fun `ignores gaps outside 15-720 minutes range`() = runTest {
        val date = "2026-08-26"
        
        // Gap of 10 minutes (too short)
        repository.saveActivityEvent(userId, date, BabyEvent.Feeding(id = "1", time = "10:00", dateTimeString = "$date 10:00"))
        repository.saveActivityEvent(userId, date, BabyEvent.Feeding(id = "2", time = "10:10", dateTimeString = "$date 10:10"))
        
        // Gap of 13 hours (780 mins, too long)
        repository.saveActivityEvent(userId, date, BabyEvent.Feeding(id = "3", time = "23:10", dateTimeString = "$date 23:10"))

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            // No valid gaps because 10 < 15 and 780 > 720
            assertNull(state.analysisResult)
        }
    }

    @Test
    fun `handles malformed time strings gracefully`() = runTest {
        val date = "2026-08-26"
        
        repository.saveActivityEvent(userId, date, BabyEvent.Feeding(id = "e1", time = "invalid", dateTimeString = "$date invalid"))
        
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(1, state.totalFeedsInCache)
            assertEquals(1, state.hourlyCounts.first { it.hour == 0 }.count) // Defaults to 0 on error
        }
    }
}
