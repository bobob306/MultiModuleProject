package com.bsdevs.babycare.presentation.feeding

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bsdevs.babycare.data.repository.BabyCareRepositoryImpl
import com.bsdevs.babycare.data.repository.FakeBabyCareFirestoreService
import com.bsdevs.babycare.presentation.home.FakeAccountService
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.repository.UserRepository
import com.bsdevs.babycare.presentation.common.TimeProvider
import java.time.LocalDate
import io.mockk.*
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class FeedingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var fakeService: FakeBabyCareFirestoreService
    private lateinit var repository: BabyCareRepositoryImpl
    private lateinit var accountService: FakeAccountService
    private lateinit var timerManager: FeedingTimerManager
    private lateinit var viewModel: FeedingViewModel
    private lateinit var dispatchers: DispatcherProvider
    private val context = mockk<android.content.Context>(relaxed = true)

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
        val timeProvider = mockk<TimeProvider>(relaxed = true)
        every { timeProvider.currentLocalDate() } returns LocalDate.of(2026, 9, 1)
        repository = BabyCareRepositoryImpl(fakeService, userRepository, dispatchers, timeProvider)
        accountService = FakeAccountService(userId)
        
        // 🚀 INSTANT TESTS: Mock the manager so we don't run real timer loops in VM tests
        timerManager = mockk(relaxed = true)
        every { timerManager.timerState } returns MutableStateFlow(FeedingTimerState())
    }

    @After
    fun tearDown() {
        timerManager.reset() // Kill any active jobs
        Dispatchers.resetMain()
        unmockkAll()
    }

    private suspend fun createViewModel(
        activityId: String? = null,
        startSide: String? = null
    ) {
        val savedStateHandle = SavedStateHandle()
        activityId?.let { savedStateHandle["activityId"] = it }
        startSide?.let { savedStateHandle["startSide"] = it }
        
        // Populate cache to ensure repository updates work
        repository.loadInitialData(userId, 1)
        
        viewModel = FeedingViewModel(accountService, repository, timerManager, context, savedStateHandle)
    }

    @Test
    fun `init with activityId loads feeding data`() = runTest {
        // Given
        val eventId = UUID.randomUUID().toString()
        val date = "2026-08-26"
        val rawData = mapOf("days" to mapOf(date to listOf(
            mapOf(
                "id" to eventId, 
                "type" to "FEEDING", 
                "time" to "10:00", 
                "dateTimeString" to "$date 10:00",
                "leftDuration" to 300L,
                "rightDuration" to 200L,
                "bottleAmountMl" to 120L,
                "comment" to "Good feed"
            )
        )))
        fakeService.injectMonth(userId, "2026-08", rawData)

        // When
        createViewModel(activityId = eventId)

        // Then
        viewModel.uiState.filter { it.id == eventId && !it.isLoading }.test {
            val finalState = awaitItem()
            assertEquals(eventId, finalState.id)
            assertEquals(120, finalState.bottleAmountMl)
            assertEquals("Good feed", finalState.comment)
        }
    }

    @Test
    fun `toggleTimer calls timerManager correctly`() = runTest {
        createViewModel()
        
        viewModel.toggleTimer(FeedingSide.LEFT)
        
        verify { timerManager.toggleTimer(FeedingSide.LEFT, any()) }
    }

    @Test
    fun `submitFeeding saves new feeding successfully`() = runTest {
        createViewModel()
        viewModel.onStartTimeSelected(14, 30)
        viewModel.updateBottleAmount(150)
        viewModel.onCommentChanged("New bottle feed")
        
        // ⏳ WAIT for state to reflect ALL inputs to avoid race conditions
        viewModel.uiState.filter { it.bottleAmountMl == 150 && it.startTime == "14:30" && it.comment == "New bottle feed" }.test {
            awaitItem()
        }

        viewModel.events.test {
            viewModel.submitFeeding()
            assertEquals(FeedingEvent.SaveSuccess, awaitItem())
        }

        val today = viewModel.uiState.value.date
        val monthId = today.substring(0, 7)
        val savedMonth = fakeService.fetchMonthDocument(userId, monthId)
        assertNotNull("Month document not found for $monthId", savedMonth)
        val days = savedMonth!!["days"] as Map<*, *>
        val dayEvents = days[today] as? List<*> ?: emptyList<Any>()
        assertTrue("Event with bottle amount 150 not found in $dayEvents for date $today", 
            dayEvents.any { (it as Map<*, *>)["bottleAmountMl"].toString() == "150" })
    }

    @Test
    fun `submitFeeding updates existing feeding successfully`() = runTest {
        // Given
        val eventId = UUID.randomUUID().toString()
        val date = "2026-08-26"
        val rawData = mapOf("days" to mapOf(date to listOf(
            mapOf("id" to eventId, "type" to "FEEDING", "time" to "10:00", "dateTimeString" to "$date 10:00")
        )))
        fakeService.injectMonth(userId, "2026-08", rawData)
        
        createViewModel(activityId = eventId)
        
        // Wait for data to load
        viewModel.uiState.filter { it.id == eventId }.first()

        // When
        viewModel.onCommentChanged("Updated comment")
        
        viewModel.events.test {
            viewModel.submitFeeding()
            assertEquals(FeedingEvent.SaveSuccess, awaitItem())
        }

        // Then
        val feeding = repository.getFeedingEventById(userId, eventId)
        assertEquals("Updated comment", feeding?.comment)
    }

    @Test
    fun `deleteFeeding removes event and triggers success`() = runTest {
        // Given
        val eventId = UUID.randomUUID().toString()
        val date = "2026-08-26"
        fakeService.injectMonth(userId, "2026-08", mapOf("days" to mapOf(date to listOf(
            mapOf("id" to eventId, "type" to "FEEDING", "time" to "10:00", "dateTimeString" to "$date 10:00")
        ))))
        
        createViewModel(activityId = eventId)
        
        // Wait for data to load
        viewModel.uiState.filter { it.id == eventId }.first()

        // When
        viewModel.events.test {
            viewModel.deleteFeeding()
            assertEquals(FeedingEvent.DeleteSuccess, awaitItem())
        }

        // Then
        assertNull(repository.getFeedingEventById(userId, eventId))
    }

    @Test
    fun `cancelFeeding resets timer and triggers success`() = runTest {
        createViewModel()
        
        // Start a timer (mocked)
        every { timerManager.isAnyTimerRunning() } returns true
        
        viewModel.events.test {
            viewModel.cancelFeeding()
            assertEquals(FeedingEvent.CancelSuccess, awaitItem())
        }

        verify { timerManager.reset() }
    }

    @Test
    fun `setShowBottleDialog updates uiState`() = runTest {
        createViewModel()
        viewModel.setShowBottleDialog(true)
        viewModel.uiState.filter { it.showBottleDialog }.test {
            assertTrue(awaitItem().showBottleDialog)
        }
        
        viewModel.setShowBottleDialog(false)
        viewModel.uiState.filter { !it.showBottleDialog }.test {
            assertFalse(awaitItem().showBottleDialog)
        }
    }

    @Test
    fun `setShowTimePicker updates uiState`() = runTest {
        createViewModel()
        viewModel.setShowTimePicker(true)
        viewModel.uiState.filter { it.showTimePicker }.test {
            assertTrue(awaitItem().showTimePicker)
        }
    }

    @Test
    fun `re-entering screen with running timer restores startTime and date`() = runTest {
        // Given: A timer is already running with a specific locked-in start time
        val lockedStartTime = "09:45"
        val lockedDate = "2026-08-31"
        
        val runningState = FeedingTimerState(
            startTime = lockedStartTime,
            date = lockedDate,
            isLeftRunning = true
        )
        every { timerManager.timerState } returns MutableStateFlow(runningState)

        // When: A new ViewModel is created (simulating re-entry from notification)
        createViewModel()

        // Then: The UI state should reflect the timer manager's locked-in metadata
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(lockedStartTime, state.startTime)
            assertEquals(lockedDate, state.date)
        }
    }

    @Test
    fun `manual start time change while timer running updates manager`() = runTest {
        createViewModel()
        
        // Start timer
        every { timerManager.isAnyTimerRunning() } returns true
        viewModel.toggleTimer(FeedingSide.LEFT)
        
        // Manually change start time
        viewModel.onStartTimeSelected(11, 45)
        
        // Manager should now reflect the manual override
        verify { timerManager.setSessionMetadata("11:45", any()) }
    }

    @Test
    fun `loading existing feed updates manager metadata`() = runTest {
        // Given: An existing feed with a specific start time
        val eventId = "existing-id"
        val historicalTime = "07:15"
        val historicalDate = "2026-08-01"
        fakeService.injectMonth(userId, "2026-08", mapOf("days" to mapOf(historicalDate to listOf(
            mapOf("id" to eventId, "type" to "FEEDING", "time" to historicalTime, "dateTimeString" to "$historicalDate $historicalTime")
        ))))

        // When: Loading that feed
        createViewModel(activityId = eventId)
        
        // Then: Manager should be synced with historical metadata
        // We use a reactive wait and then verify the interaction
        viewModel.uiState.filter { it.id == eventId && !it.isLoading }.test {
            awaitItem()
            verify(timeout = 2000) { timerManager.setSessionMetadata(historicalTime, historicalDate) }
        }
    }

    @Test
    fun `reset clears session metadata`() = runTest {
        createViewModel()
        
        // When: Cancelling (which calls reset)
        viewModel.cancelFeeding()
        
        // Then: Manager reset should be called
        verify { timerManager.reset() }
    }



    @Test
    fun `setIsPlayingSplodge updates uiState`() = runTest {
        createViewModel()
        viewModel.setIsPlayingSplodge(true)
        viewModel.uiState.filter { it.isPlayingSplodge }.test {
            assertTrue(awaitItem().isPlayingSplodge)
        }
    }
}
