package com.bsdevs.babycare.presentation.nappy

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.cash.turbine.test
import com.bsdevs.babycare.data.repository.BabyCareRepositoryImpl
import com.bsdevs.babycare.data.repository.FakeBabyCareFirestoreService
import com.bsdevs.babycare.presentation.home.FakeAccountService
import com.bsdevs.babycare.presentation.navigation.NappyChangeRoute
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.repository.UserRepository
import java.util.UUID
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
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
class NappyChangeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var fakeService: FakeBabyCareFirestoreService
    private lateinit var repository: BabyCareRepositoryImpl
    private lateinit var accountService: FakeAccountService
    private lateinit var viewModel: NappyChangeViewModel
    private lateinit var dispatchers: DispatcherProvider

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

        fakeService = FakeBabyCareFirestoreService()
        val userRepo = mockk<UserRepository>(relaxed = true)
        repository = BabyCareRepositoryImpl(fakeService, userRepo, dispatchers)
        accountService = FakeAccountService(userId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private suspend fun createViewModel(activityId: String? = null) {
        val savedStateHandle = SavedStateHandle()
        every { savedStateHandle.toRoute<NappyChangeRoute>() } returns NappyChangeRoute(activityId)
        
        repository.loadInitialData(userId, 20)
        
        viewModel = NappyChangeViewModel(accountService, repository, dispatchers, savedStateHandle)
    }

    @Test
    fun `init with activityId loads nappy change data`() = runTest {
        // Given
        val eventId = UUID.randomUUID().toString()
        val date = "2026-08-26"
        val correctData = mapOf("days" to mapOf(date to listOf(
            mapOf(
                "id" to eventId, 
                "type" to "NAPPY",
                "nappyType" to "Wet",
                "time" to "11:00", 
                "dateTimeString" to "$date 11:00",
                "comment" to "Quick change"
            )
        )))
        fakeService.injectMonth(userId, "2026-08", correctData)

        // When
        createViewModel(activityId = eventId)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            val finalState = if (state.id == null) awaitItem() else state

            assertEquals(eventId, finalState.id)
            assertEquals("Wet", finalState.type)
            assertEquals("11:00", finalState.time)
            assertEquals("Quick change", finalState.comment)
        }
    }

    @Test
    fun `onTypeChanged updates uiState`() = runTest {
        createViewModel()
        viewModel.onTypeChanged("Dirty")
        assertEquals("Dirty", viewModel.uiState.value.type)
    }

    @Test
    fun `submitNappyChange saves new record successfully`() = runTest {
        createViewModel()
        viewModel.onTimeSelected(9, 0)
        viewModel.onTypeChanged("Both")
        viewModel.onCommentChanged("Morning change")

        viewModel.events.test {
            viewModel.submitNappyChange()
            assertEquals(NappyChangeEvent.SaveSuccess, awaitItem())
        }

        val savedMonth = fakeService.fetchMonthDocument(userId, "2026-08")
        assertNotNull(savedMonth)
        val days = savedMonth!!["days"] as Map<*, *>
        val today = viewModel.uiState.value.date
        val dayEvents = days[today] as List<*>
        assertTrue(dayEvents.any { (it as Map<*, *>)["nappyType"] == "Both" })
    }

    @Test
    fun `deleteNappyChange removes record successfully`() = runTest {
        // Given
        val eventId = UUID.randomUUID().toString()
        val date = "2026-08-26"
        fakeService.injectMonth(userId, "2026-08", mapOf("days" to mapOf(date to listOf(
            mapOf("id" to eventId, "type" to "NAPPY", "time" to "11:00", "dateTimeString" to "$date 11:00")
        ))))
        
        createViewModel(activityId = eventId)
        
        // Wait for data to load
        viewModel.uiState.filter { it.id == eventId }.first()

        // When
        viewModel.events.test {
            viewModel.deleteNappyChange()
            assertEquals(NappyChangeEvent.DeleteSuccess, awaitItem())
        }

        // Then
        assertNull(repository.getNappyEventById(userId, eventId))
    }

    @Test
    fun `setShowTimePicker updates uiState`() = runTest {
        createViewModel()
        viewModel.setShowTimePicker(true)
        assertTrue(viewModel.uiState.value.showTimePicker)
    }

    @Test
    fun `setIsPlayingTurdAnimation updates uiState`() = runTest {
        createViewModel()
        viewModel.setIsPlayingTurdAnimation(true)
        assertTrue(viewModel.uiState.value.isPlayingTurdAnimation)
    }
}
