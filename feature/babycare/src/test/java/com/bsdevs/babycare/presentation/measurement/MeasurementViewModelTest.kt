package com.bsdevs.babycare.presentation.measurement

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var accountService: AccountService
    private lateinit var repository: BabyCareRepository
    private lateinit var userRepository: UserRepository
    private lateinit var dispatchers: DispatcherProvider
    private lateinit var viewModel: MeasurementViewModel

    private val userId = "testUser"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        accountService = mockk {
            every { currentUserId } returns userId
            every { currentUser } returns MutableStateFlow(null)
        }
        repository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        every { repository.measurements } returns MutableStateFlow(emptyList<UnifiedEventDto>())
        every { userRepository.userProfile } returns MutableStateFlow(null)
        coEvery { repository.saveActivityEvent(any(), any(), any()) } just Runs
        coEvery { repository.updateActivityEvent(any(), any(), any(), any()) } just Runs
        
        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel(activityId: String? = null) {
        val savedStateHandle = SavedStateHandle()
        if (activityId != null) {
            savedStateHandle["activityId"] = activityId
        }
        viewModel = MeasurementViewModel(accountService, repository, userRepository, savedStateHandle)
    }

    @Test
    fun `init with activityId loads measurement from repository`() = runTest {
        // Given
        val activityId = "m1"
        val event = UnifiedEventDto(
            id = activityId,
            type = "MEASUREMENT",
            time = "10:00",
            dateTimeString = "2026-08-26 10:00",
            height = 52.0,
            weight = 3.5,
            isMedical = true,
            comment = "Routine check"
        )
        coEvery { repository.getMeasurementEventById(any(), any()) } returns event

        // When
        createViewModel(activityId)
        
        // Then
        viewModel.uiState.test {
            // In Unconfined mode, the load should complete immediately
            val final = awaitItem()
            assertEquals(activityId, final.id)
            assertEquals(52.0, final.height!!, 0.01)
            assertEquals(3.5, final.weight!!, 0.01)
            assertTrue(final.isMedical)
            assertEquals("Routine check", final.comment)
        }
    }

    @Test
    fun `onHeightChanged updates height in state`() = runTest {
        createViewModel()

        // When: Height changed to 520 (represents 52.0cm)
        viewModel.onHeightChanged(520)

        // Then
        viewModel.uiState.filter { it.height != null }.test {
            assertEquals(52.0, awaitItem().height!!, 0.01)
        }
    }

    @Test
    fun `onWeightChanged updates weight in state`() = runTest {
        createViewModel()

        // When: Weight changed to 350 (represents 3.50kg)
        viewModel.onWeightChanged(350)

        // Then
        viewModel.uiState.filter { it.weight != null }.test {
            assertEquals(3.5, awaitItem().weight!!, 0.01)
        }
    }

    @Test
    fun `submitMeasurement saves new measurement to repository`() = runTest {
        // Given
        createViewModel()
        viewModel.onHeightChanged(520)
        viewModel.onWeightChanged(350)
        viewModel.onIsMedicalChanged(true)

        // When & Then
        viewModel.uiState.test {
            // Wait for inputs to be reflected in combined state
            var state = awaitItem()
            while (!state.recordHeight || !state.recordWeight) { state = awaitItem() }
            
            viewModel.events.test {
                viewModel.submitMeasurement()
                assertEquals(MeasurementEvent.SaveSuccess, awaitItem())
            }
        }
    }

    @Test
    fun `submitMeasurement updates existing measurement in repository`() = runTest {
        // Given
        val activityId = "m1"
        val event = UnifiedEventDto(id = activityId, type = "MEASUREMENT", dateTimeString = "2026-08-26 10:00")
        coEvery { repository.getMeasurementEventById(any(), any()) } returns event
        
        createViewModel(activityId)
        
        // ⏳ WAIT for initial load to populate the ID in state.
        viewModel.uiState.filter { it.id == activityId }.test {
            assertEquals(activityId, awaitItem().id)
        }
        
        viewModel.onWeightChanged(360)

        // When
        viewModel.submitMeasurement()

        // Then
        val stateDate = viewModel.uiState.value.date
        coVerify(timeout = 2000) {
            repository.updateActivityEvent(userId, stateDate, activityId, any())
        }
    }

    @Test
    fun `deleteMeasurement removes measurement from repository`() = runTest {
        // Given
        val activityId = "m1"
        val event = UnifiedEventDto(id = activityId, type = "MEASUREMENT", dateTimeString = "2026-08-26 10:00")
        coEvery { repository.getMeasurementEventById(any(), any()) } returns event
        
        createViewModel(activityId)
        
        // ⏳ WAIT for initial load
        viewModel.uiState.filter { it.id == activityId }.test {
            awaitItem()
        }

        // When
        viewModel.deleteMeasurement()

        // Then
        val stateDate = viewModel.uiState.value.date
        coVerify(timeout = 2000) {
            repository.deleteActivityEvent(userId, stateDate, activityId)
        }
        viewModel.events.test {
            assertEquals(MeasurementEvent.DeleteSuccess, awaitItem())
        }
    }

    @Test
    fun `setShowSheet updates uiState`() = runTest {
        createViewModel()
        viewModel.setShowSheet(true)
        viewModel.uiState.filter { it.showSheet }.test {
            assertTrue(awaitItem().showSheet)
        }
    }

    @Test
    fun `setShowDatePicker updates uiState`() = runTest {
        createViewModel()
        viewModel.setShowDatePicker(true)
        viewModel.uiState.filter { it.showDatePicker }.test {
            assertTrue(awaitItem().showDatePicker)
        }
    }
}
