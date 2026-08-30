package com.bsdevs.coffeescreen.screens.inputscreen

import app.cash.turbine.test
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.InputType
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.InputViewData.InputVD
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CoffeeInputScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var fakeService: FakeCoffeeApiService
    private lateinit var accountService: FakeAccountService
    private lateinit var viewModel: CoffeeInputScreenViewModel
    private lateinit var dispatchers: DispatcherProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

        fakeService = FakeCoffeeApiService()
        accountService = FakeAccountService()
        // We create the viewModel inside tests to allow configuring the fakeService first
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load fetches data and populates viewData`() = runTest {
        // Given
        val dto = CoffeeInputScreenDto(BEANS = listOf("Arabica", "Robusta"))
        fakeService.screenData = dto

        // When
        viewModel = CoffeeInputScreenViewModel(accountService, fakeService, dispatchers)

        // Then
        viewModel.viewData.test {
            // First item might be Loading if initialization hasn't finished
            var lastResult = awaitItem()
            if (lastResult is Result.Loading) {
                lastResult = awaitItem()
            }
            
            assertTrue(lastResult is Result.Success)
            val data = (lastResult as Result.Success).data
            val beansInput = data.inputs.filterIsInstance<InputVD>().first { it.inputType == InputType.BEANS }
            assertEquals(listOf("Arabica", "Robusta"), beansInput.inputList)
        }
    }

    @Test
    fun `isButtonEnabled updates correctly based on inputs`() = runTest {
        viewModel = CoffeeInputScreenViewModel(accountService, fakeService, dispatchers)
        
        // Initial state: disabled
        assertFalse(viewModel.isButtonEnabled.value)

        // Fill all required fields
        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdownSelection(InputType.BEANS, "Arabica"))
        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdownSelection(InputType.ORIGIN, "Brazil"))
        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdownSelection(InputType.TASTE, "Sweet"))
        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdownSelection(InputType.METHOD, "Espresso"))
        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdownSelection(InputType.ROASTER, "Pact"))
        viewModel.processIntent(CoffeeInputScreenIntent.UpdateRoastDate(LocalDate.now()))

        assertTrue(viewModel.isButtonEnabled.value)
    }

    @Test
    fun `submitCoffee uploads data and navigates home`() = runTest {
        viewModel = CoffeeInputScreenViewModel(accountService, fakeService, dispatchers)

        // Setup valid data
        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdownSelection(InputType.BEANS, "Arabica"))
        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdownSelection(InputType.ORIGIN, "Brazil"))
        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdownSelection(InputType.TASTE, "Sweet"))
        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdownSelection(InputType.METHOD, "Espresso"))
        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdownSelection(InputType.ROASTER, "Pact"))
        viewModel.processIntent(CoffeeInputScreenIntent.UpdateRoastDate(LocalDate.of(2026, 8, 26)))

        viewModel.navigationEvent.test {
            viewModel.processIntent(CoffeeInputScreenIntent.SubmitCoffee)
            
            assertEquals(1, fakeService.uploadedCoffees.size)
            assertEquals("Pact Brazil Espresso 2026-08-26", fakeService.uploadedCoffees.first().label)
            assertEquals(NavigationEvent.NavigateToHome, awaitItem())
        }
    }

    @Test
    fun `toggleDropdownSelection for singleInput replaces selection`() = runTest {
        viewModel = CoffeeInputScreenViewModel(accountService, fakeService, dispatchers)

        // ROASTER is singleInput
        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdownSelection(InputType.ROASTER, "Roaster A"))
        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdownSelection(InputType.ROASTER, "Roaster B"))

        val data = (viewModel.viewData.value as Result.Success).data
        val roasterInput = data.inputs.filterIsInstance<InputVD>().first { it.inputType == InputType.ROASTER }
        assertEquals(setOf("Roaster B"), roasterInput.selectedSet)
    }

    @Test
    fun `toggleDropdownSelection for multiInput appends selection`() = runTest {
        viewModel = CoffeeInputScreenViewModel(accountService, fakeService, dispatchers)

        // BEANS is multiInput
        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdownSelection(InputType.BEANS, "Arabica"))
        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdownSelection(InputType.BEANS, "Robusta"))

        val data = (viewModel.viewData.value as Result.Success).data
        val beansInput = data.inputs.filterIsInstance<InputVD>().first { it.inputType == InputType.BEANS }
        assertEquals(setOf("Arabica", "Robusta"), beansInput.selectedSet)
    }

    @Test
    fun `updateSearchText updates correctly`() = runTest {
        viewModel = CoffeeInputScreenViewModel(accountService, fakeService, dispatchers)

        viewModel.processIntent(CoffeeInputScreenIntent.UpdateSearchText(InputType.TASTE, "Fruit"))

        val data = (viewModel.viewData.value as Result.Success).data
        val tasteInput = data.inputs.filterIsInstance<InputVD>().first { it.inputType == InputType.TASTE }
        assertEquals("Fruit", tasteInput.searchText)
    }

    @Test
    fun `toggleDropdown updates expandedInputType`() = runTest {
        viewModel = CoffeeInputScreenViewModel(accountService, fakeService, dispatchers)

        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdown(InputType.BEANS))
        assertEquals(InputType.BEANS, (viewModel.viewData.value as Result.Success).data.expandedInputType)

        viewModel.processIntent(CoffeeInputScreenIntent.ToggleDropdown(null))
        assertNull((viewModel.viewData.value as Result.Success).data.expandedInputType)
    }

    @Test
    fun `setDatePickerVisibility updates isDatePickerVisible`() = runTest {
        viewModel = CoffeeInputScreenViewModel(accountService, fakeService, dispatchers)

        viewModel.processIntent(CoffeeInputScreenIntent.SetDatePickerVisibility(true))
        assertTrue((viewModel.viewData.value as Result.Success).data.isDatePickerVisible)

        viewModel.processIntent(CoffeeInputScreenIntent.SetDatePickerVisibility(false))
        assertFalse((viewModel.viewData.value as Result.Success).data.isDatePickerVisible)
    }
}
