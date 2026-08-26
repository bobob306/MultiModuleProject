package com.bsdevs.coffeescreen.screens.detailscreen

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.cash.turbine.test
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.inputscreen.FakeAccountService
import com.bsdevs.coffeescreen.screens.inputscreen.FakeCoffeeApiService
import com.bsdevs.coffeescreen.navigation.CoffeeDetailScreenRoute
import com.bsdevs.common.result.Result
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CoffeeDetailsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var fakeService: FakeCoffeeApiService
    private lateinit var accountService: FakeAccountService
    private lateinit var viewModel: CoffeeDetailsViewModel

    private val userId = "testUser"
    private val coffeeId = "c1"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        fakeService = FakeCoffeeApiService()
        accountService = FakeAccountService(userId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel() {
        val savedStateHandle = mockk<SavedStateHandle>(relaxed = true)
        
        // Mock the top-level extension function container for toRoute
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every { savedStateHandle.toRoute<CoffeeDetailScreenRoute>() } returns CoffeeDetailScreenRoute(coffeeId)
        
        viewModel = CoffeeDetailsViewModel(savedStateHandle, accountService, fakeService)
    }

    private suspend fun ensureReady() {
        // Collect once to trigger onStart and get to Success state
        viewModel.viewData.filterIsInstance<Result.Success<CoffeeDetailsViewData>>().first()
        testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun `initial load fetches coffee and shots`() = runTest {
        // Given
        val coffee = CoffeeDto(id = coffeeId, userId = userId, label = "Pact Brazil", roastDate = "2026-08-26")
        fakeService.uploadedCoffees.add(coffee)
        
        val shot = ShotDto(id = "s1", date = "26/08/2026", rating = 5)
        fakeService.uploadShot("Pact Brazil", shot)

        // When
        createViewModel()
        ensureReady()

        // Then
        val result = viewModel.viewData.value
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("Pact Brazil", data.coffeeDto.label)
        assertEquals(1, data.shotList?.size)
        assertEquals("s1", data.shotList?.first()?.id)
    }

    @Test
    fun `submitShot adds new shot and refreshes list`() = runTest {
        // Given
        val coffee = CoffeeDto(id = coffeeId, userId = userId, label = "Pact Brazil")
        fakeService.uploadedCoffees.add(coffee)
        createViewModel()
        ensureReady()

        val newShot = EspressoShotDetails(
            id = "s2",
            date = LocalDate.of(2026, 8, 26),
            weightInGrams = 180, // 18.0g
            weightOutGrams = 360, // 36.0g
            timeInSeconds = 30,
            rating = 4
        )

        // When
        viewModel.processIntent(CoffeeDetailsIntent.SubmitShot(newShot))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = (viewModel.viewData.value as Result.Success).data
        assertEquals(1, state.shotList?.size)
        assertEquals("s2", state.shotList?.first()?.id)
        assertFalse(state.showSheet)
    }

    @Test
    fun `shots are sorted by date descending`() = runTest {
        // Given
        val coffee = CoffeeDto(id = coffeeId, userId = userId, label = "Pact Brazil")
        fakeService.uploadedCoffees.add(coffee)
        
        fakeService.uploadShot("Pact Brazil", ShotDto(id = "old", date = "01/08/2026"))
        fakeService.uploadShot("Pact Brazil", ShotDto(id = "new", date = "26/08/2026"))

        // When
        createViewModel()
        ensureReady()

        // Then
        val state = (viewModel.viewData.value as Result.Success).data
        assertEquals("new", state.shotList?.get(0)?.id)
        assertEquals("old", state.shotList?.get(1)?.id)
    }

    @Test
    fun `toggling sheet visibility works`() = runTest {
        val coffee = CoffeeDto(id = coffeeId, userId = userId, label = "Pact Brazil")
        fakeService.uploadedCoffees.add(coffee)
        createViewModel()
        ensureReady()

        viewModel.processIntent(CoffeeDetailsIntent.ShowSheet)
        testDispatcher.scheduler.runCurrent()
        assertTrue((viewModel.viewData.value as Result.Success).data.showSheet)

        viewModel.processIntent(CoffeeDetailsIntent.HideSheet)
        testDispatcher.scheduler.runCurrent()
        assertFalse((viewModel.viewData.value as Result.Success).data.showSheet)
    }
}
