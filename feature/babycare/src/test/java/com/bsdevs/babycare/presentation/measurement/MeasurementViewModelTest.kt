package com.bsdevs.babycare.presentation.measurement

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.cash.turbine.test
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.babycare.presentation.navigation.MeasurementRoute
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var accountService: AccountService
    private lateinit var repository: BabyCareRepository
    private lateinit var dispatchers: DispatcherProvider
    private lateinit var viewModel: MeasurementViewModel

    private val userId = "testUser"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("androidx.navigation.SavedStateHandleKt")

        accountService = mockk {
            every { currentUserId } returns userId
        }
        repository = mockk(relaxed = true)
        every { repository.measurements } returns MutableStateFlow(emptyList<UnifiedEventDto>())
        
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
        every { savedStateHandle.toRoute<MeasurementRoute>() } returns MeasurementRoute(activityId)
        viewModel = MeasurementViewModel(accountService, repository, dispatchers, savedStateHandle)
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
        assertEquals(52.0, viewModel.uiState.value.height!!, 0.01)
    }

    @Test
    fun `onWeightChanged updates weight in state`() = runTest {
        createViewModel()

        // When: Weight changed to 350 (represents 3.50kg)
        viewModel.onWeightChanged(350)

        // Then
        assertEquals(3.5, viewModel.uiState.value.weight!!, 0.01)
    }

    @Test
    fun `submitMeasurement saves new measurement to repository`() = runTest {
        // Given
        createViewModel()
        viewModel.onHeightChanged(520)
        viewModel.onWeightChanged(350)
        viewModel.onIsMedicalChanged(true)

        // When
        viewModel.submitMeasurement()

        // Then
        coVerify {
            repository.saveActivityEvent(userId, any(), withArg {
                assertEquals("MEASUREMENT", it.type)
                assertEquals(52.0, it.height!!, 0.01)
                assertEquals(3.5, it.weight!!, 0.01)
                assertTrue(it.isMedical!!)
            })
        }
        viewModel.events.test {
            assertEquals(MeasurementEvent.SaveSuccess, awaitItem())
        }
    }

    @Test
    fun `submitMeasurement updates existing measurement in repository`() = runTest {
        // Given
        val activityId = "m1"
        val event = UnifiedEventDto(id = activityId, type = "MEASUREMENT", dateTimeString = "2026-08-26 10:00")
        coEvery { repository.getMeasurementEventById(any(), any()) } returns event
        
        createViewModel(activityId)
        viewModel.onWeightChanged(360)

        // When
        viewModel.submitMeasurement()

        // Then
        coVerify {
            repository.updateActivityEvent(userId, "2026-08-26", activityId, any())
        }
    }

    @Test
    fun `deleteMeasurement removes measurement from repository`() = runTest {
        // Given
        val activityId = "m1"
        val event = UnifiedEventDto(id = activityId, type = "MEASUREMENT", dateTimeString = "2026-08-26 10:00")
        coEvery { repository.getMeasurementEventById(any(), any()) } returns event
        
        createViewModel(activityId)

        // When
        viewModel.deleteMeasurement()

        // Then
        coVerify {
            repository.deleteActivityEvent(userId, "2026-08-26", activityId)
        }
        viewModel.events.test {
            assertEquals(MeasurementEvent.DeleteSuccess, awaitItem())
        }
    }

    @Test
    fun `setShowSheet updates uiState`() = runTest {
        createViewModel()
        viewModel.setShowSheet(true)
        assertTrue(viewModel.uiState.value.showSheet)
    }

    @Test
    fun `setShowDatePicker updates uiState`() = runTest {
        createViewModel()
        viewModel.setShowDatePicker(true)
        assertTrue(viewModel.uiState.value.showDatePicker)
    }
}
