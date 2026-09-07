package com.bsdevs.babycare.presentation.feeding

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bsdevs.babycare.data.BabyCareRepositoryImpl
import com.bsdevs.babycare.data.repository.FakeBabyCareFirestoreService
import com.bsdevs.babycare.presentation.common.TimeProvider
import com.bsdevs.babycare.presentation.home.FakeAccountService
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.SyncManager
import com.bsdevs.data.local.dao.BabyEventDao
import com.bsdevs.data.repository.UserRepository
import com.bsdevs.network.dto.BabyDto
import com.bsdevs.network.dto.UserDto
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.TimeZone
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class FeedingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeService: FakeBabyCareFirestoreService
    private lateinit var repository: BabyCareRepositoryImpl
    private lateinit var accountService: FakeAccountService
    private lateinit var userRepository: UserRepository
    private lateinit var userProfileFlow: MutableStateFlow<UserDto?>
    private lateinit var timerManager: FeedingTimerManager
    private lateinit var viewModel: FeedingViewModel
    private lateinit var dispatchers: DispatcherProvider
    private lateinit var timeProvider: TimeProvider
    private val context = mockk<Context>(relaxed = true)

    private val userId = "testUser"
    private val babyId = "baby1"
    private val fixedTestDate = LocalDate.of(2026, 9, 1)
    private val fixedTestTime = LocalTime.of(12, 0)

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
        userRepository = mockk<UserRepository>(relaxed = true)
        userProfileFlow = MutableStateFlow<UserDto?>(UserDto(id = userId, babyId = babyId))
        every { userRepository.userProfile } returns userProfileFlow

        timeProvider = mockk {
            every { currentLocalDate() } returns fixedTestDate
            every { currentLocalTime() } returns fixedTestTime
        }

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
        accountService = FakeAccountService(userId)

        timerManager = mockk(relaxed = true)
        every { timerManager.timerState } returns MutableStateFlow(FeedingTimerState())
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
        activityId?.let { savedStateHandle["activityId"] = it }
        startSide?.let { savedStateHandle["startSide"] = it }

        repository.loadInitialData(userId, 1)

        viewModel = FeedingViewModel(
            accountService,
            repository,
            userRepository,
            timerManager,
            timeProvider,
            context,
            savedStateHandle
        )
    }

    @Test
    fun `init with activityId loads feeding data`() = runTest {
        val eventId = UUID.randomUUID().toString()
        val date = fixedTestDate.toString()
        val monthId = date.substring(0, 7)
        val rawData = mapOf(
            "days" to mapOf(
                date to listOf(
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
                )
            )
        )
        fakeService.injectMonth(userId, monthId, rawData)

        createViewModel(activityId = eventId)

        viewModel.uiState.filter { (it.id == eventId && !it.isLoading) }.test {
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

        viewModel.uiState.filter { it.bottleAmountMl == 150 && it.startTime == "14:30" && it.comment == "New bottle feed" }
            .test {
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
        assertTrue(dayEvents.any { (it as Map<*, *>)["bottleAmountMl"].toString() == "150" })
    }

    @Test
    fun `submitFeeding updates existing feeding successfully`() = runTest {
        val eventId = UUID.randomUUID().toString()
        val date = fixedTestDate.toString()
        val monthId = date.substring(0, 7)
        val rawData = mapOf(
            "days" to mapOf(
                date to listOf(
                    mapOf(
                        "id" to eventId,
                        "type" to "FEEDING",
                        "time" to "10:00",
                        "dateTimeString" to "$date 10:00"
                    )
                )
            )
        )
        fakeService.injectMonth(userId, monthId, rawData)

        createViewModel(activityId = eventId)
        viewModel.uiState.filter { it.id == eventId }.first()

        viewModel.onCommentChanged("Updated comment")

        viewModel.events.test {
            viewModel.submitFeeding()
            assertEquals(FeedingEvent.SaveSuccess, awaitItem())
        }

        val feeding = repository.getFeedingEventById(userId, eventId)
        assertEquals("Updated comment", feeding?.comment)
    }

    @Test
    fun `deleteFeeding removes event and triggers success`() = runTest {
        val eventId = UUID.randomUUID().toString()
        val date = fixedTestDate.toString()
        val monthId = date.substring(0, 7)
        fakeService.injectMonth(
            userId, monthId, mapOf(
                "days" to mapOf(
                    date to listOf(
                        mapOf(
                            "id" to eventId,
                            "type" to "FEEDING",
                            "time" to "10:00",
                            "dateTimeString" to "$date 10:00"
                        )
                    )
                )
            )
        )

        createViewModel(activityId = eventId)
        viewModel.uiState.filter { it.id == eventId }.first()

        viewModel.events.test {
            viewModel.deleteFeeding()
            assertEquals(FeedingEvent.DeleteSuccess, awaitItem())
        }

        assertNull(repository.getFeedingEventById(userId, eventId))
    }

    @Test
    fun `cancelFeeding resets timer and triggers success`() = runTest {
        createViewModel()
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
    }

    @Test
    fun `re-entering screen with running timer restores startTime and date`() = runTest {
        val lockedStartTime = "09:45"
        val lockedDate = "2026-08-31"

        val runningState = FeedingTimerState(
            startTime = lockedStartTime,
            date = lockedDate,
            isLeftRunning = true
        )
        every { timerManager.timerState } returns MutableStateFlow(runningState)

        createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(lockedStartTime, state.startTime)
            assertEquals(lockedDate, state.date)
        }
    }

    @Test
    fun `manual start time change while timer running updates manager`() = runTest {
        createViewModel()
        every { timerManager.isAnyTimerRunning() } returns true
        viewModel.toggleTimer(FeedingSide.LEFT)
        viewModel.onStartTimeSelected(11, 45)
        verify { timerManager.setSessionMetadata("11:45", any()) }
    }

    @Test
    fun `loading existing feed updates manager metadata`() = runTest {
        val eventId = "existing-id"
        val historicalTime = "07:15"
        val historicalDate = fixedTestDate.toString()
        val monthId = historicalDate.substring(0, 7)
        fakeService.injectMonth(
            userId, monthId, mapOf(
                "days" to mapOf(
                    historicalDate to listOf(
                        mapOf(
                            "id" to eventId,
                            "type" to "FEEDING",
                            "time" to historicalTime,
                            "dateTimeString" to "$historicalDate $historicalTime"
                        )
                    )
                )
            )
        )

        createViewModel(activityId = eventId)

        viewModel.uiState.filter { (it.id == eventId && !it.isLoading) }.test {
            awaitItem()
            verify(timeout = 2000) {
                timerManager.setSessionMetadata(
                    historicalTime,
                    historicalDate
                )
            }
        }
    }

    @Test
    fun `setIsPlayingSplodge updates uiState`() = runTest {
        createViewModel()
        viewModel.setIsPlayingSplodge(true)
        viewModel.uiState.filter { it.isPlayingSplodge }.test {
            assertTrue(awaitItem().isPlayingSplodge)
        }
    }

    @Test
    fun `submitFeeding calculates and saves prediction gap from baby profile`() = runTest {
        createViewModel()

        // Wait for UI state to be populated with defaults from timeProvider
        viewModel.uiState.filter { it.date.isNotEmpty() }.first()

        val todayDate = fixedTestDate.toString()

        val baby = BabyDto(
            id = babyId,
            nextFeedingTime = "14:00"
        )
        coEvery { userRepository.getBaby(babyId, any()) } returns baby

        viewModel.onStartTimeSelected(14, 30)

        viewModel.submitFeeding()

        val monthId = todayDate.substring(0, 7)
        val savedMonth = fakeService.fetchMonthDocument(userId, monthId)
        assertNotNull("Month document not found for $monthId", savedMonth)
        val days = savedMonth!!["days"] as Map<*, *>
        val dayEvents = days[todayDate] as? List<*> ?: emptyList<Any?>()

        val savedEvent = dayEvents.firstOrNull() as? Map<String, Any?>
        assertNotNull("Event not found for date $todayDate in $days", savedEvent)

        val actualGap = (savedEvent!!["predictionGapMinutes"] as? Number)?.toLong()
        assertEquals(30L, actualGap)
    }
}
