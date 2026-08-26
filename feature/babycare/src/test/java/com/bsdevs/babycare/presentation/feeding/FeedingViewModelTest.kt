package com.bsdevs.babycare.presentation.feeding

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.cash.turbine.test
import com.bsdevs.babycare.data.repository.BabyCareRepositoryImpl
import com.bsdevs.babycare.data.repository.FakeBabyCareFirestoreService
import com.bsdevs.babycare.presentation.home.FakeAccountService
import com.bsdevs.babycare.presentation.navigation.FeedingRoute
import com.bsdevs.common.DispatcherProvider
import io.mockk.*
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var fakeService: FakeBabyCareFirestoreService
    private lateinit var repository: BabyCareRepositoryImpl
    private lateinit var accountService: FakeAccountService
    private lateinit var timeProvider: com.bsdevs.babycare.presentation.common.TimeProvider
    private lateinit var viewModel: FeedingViewModel
    private lateinit var dispatchers: DispatcherProvider

    private val userId = "testUser"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        mockkStatic(SystemClock::class)
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        
        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

        fakeService = FakeBabyCareFirestoreService()
        repository = BabyCareRepositoryImpl(fakeService, dispatchers)
        accountService = FakeAccountService(userId)
        timeProvider = mockk()
        every { timeProvider.elapsedRealtime() } returns 0L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private suspend fun createViewModel(
        activityId: String? = null,
        startSide: String? = null
    ) {
        val savedStateHandle = SavedStateHandle()
        every { savedStateHandle.toRoute<FeedingRoute>() } returns FeedingRoute(activityId, startSide)
        
        // Populate cache to ensure repository updates work
        repository.loadInitialData(userId, 20)
        
        viewModel = FeedingViewModel(accountService, repository, timeProvider, dispatchers, savedStateHandle)
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
        viewModel.uiState.test {
            // Unconfined handles the emission immediately
            val finalState = awaitItem()
            assertEquals(eventId, finalState.id)
            assertEquals(120, finalState.bottleAmountMl)
            assertEquals("Good feed", finalState.comment)
        }
    }

    @Test
    fun `toggleTimer starts and pauses timers correctly`() = runTest {
        createViewModel()
        
        // Start LEFT
        every { timeProvider.elapsedRealtime() } returns 1000L
        viewModel.toggleTimer(FeedingSide.LEFT)
        assertTrue(viewModel.uiState.value.isLeftRunning)

        // Advance 10s
        every { timeProvider.elapsedRealtime() } returns 11000L
        // Delay inside launch will trigger with advanceTimeBy
        advanceTimeBy(10001) 
        
        assertEquals(10L, viewModel.uiState.value.leftDuration)

        // Pause LEFT
        viewModel.toggleTimer(FeedingSide.LEFT)
        assertFalse(viewModel.uiState.value.isLeftRunning)
    }

    @Test
    fun `submitFeeding saves new feeding successfully`() = runTest {
        createViewModel()
        viewModel.onStartTimeSelected(14, 30)
        viewModel.updateBottleAmount(150)
        viewModel.onCommentChanged("New bottle feed")

        viewModel.events.test {
            viewModel.submitFeeding()
            assertEquals(FeedingEvent.SaveSuccess, awaitItem())
        }

        val savedMonth = fakeService.fetchMonthDocument(userId, "2026-08")
        assertNotNull(savedMonth)
        val days = savedMonth!!["days"] as Map<*, *>
        val today = viewModel.uiState.value.date
        val dayEvents = days[today] as List<*>
        assertTrue(dayEvents.any { (it as Map<*, *>)["bottleAmountMl"].toString().startsWith("150") })
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
}
