package com.bsdevs.coffeescreen.screens.homescreen

import app.cash.turbine.test
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.homescreen.viewdata.CoffeeHomeScreenViewDatas
import com.bsdevs.coffeescreen.screens.inputscreen.FakeAccountService
import com.bsdevs.coffeescreen.screens.inputscreen.FakeCoffeeApiService
import com.bsdevs.coffeescreen.screens.inputscreen.NavigationEvent
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoffeeHomeScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var fakeService: FakeCoffeeApiService
    private lateinit var accountService: FakeAccountService
    private lateinit var viewModel: CoffeeHomeScreenViewModel
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

        fakeService = FakeCoffeeApiService()
        accountService = FakeAccountService(userId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load fetches coffee list from network`() = runTest {
        // Given
        val coffee = CoffeeDto(id = "c1", label = "Test Coffee", userId = userId)
        fakeService.uploadedCoffees.add(coffee)

        // When
        viewModel = CoffeeHomeScreenViewModel(accountService, fakeService, dispatchers)

        // Then
        viewModel.viewData.test {
            // Wait for success
            var result = awaitItem()
            while (result !is Result.Success) {
                result = awaitItem()
            }
            
            val data = result.data
            val coffeeList = data.viewData.filterIsInstance<CoffeeHomeScreenViewDatas.CoffeeList>().first().coffeeList
            assertNotNull(coffeeList)
            assertEquals(1, coffeeList!!.size)
            assertEquals("Test Coffee", coffeeList.first().label)
        }
    }

    @Test
    fun `NavigateToInput intent emits correct navigation event`() = runTest {
        viewModel = CoffeeHomeScreenViewModel(accountService, fakeService, dispatchers)
        
        viewModel.navigationEvent.test {
            viewModel.processIntent(CoffeeHomeScreenIntent.NavigateToInput)
            assertEquals(NavigationEvent.NavigateToInput, awaitItem())
        }
    }

    @Test
    fun `Logout intent signs out and navigates to login`() = runTest {
        viewModel = CoffeeHomeScreenViewModel(accountService, fakeService, dispatchers)
        
        viewModel.navigationEvent.test {
            viewModel.processIntent(CoffeeHomeScreenIntent.Logout)
            assertEquals(NavigationEvent.NavigateToLogin, awaitItem())
        }
        
        assertFalse(accountService.hasUser())
    }

    @Test
    fun `NavigateToDetail intent emits correct navigation event with coffeeId`() = runTest {
        viewModel = CoffeeHomeScreenViewModel(accountService, fakeService, dispatchers)
        
        viewModel.navigationEvent.test {
            viewModel.processIntent(CoffeeHomeScreenIntent.NavigateToDetail("c123"))
            val event = awaitItem() as NavigationEvent.NavigateToDetail
            assertEquals("c123", event.coffeeId)
        }
    }

    @Test
    fun `start failure navigates to login`() = runTest {
        accountService.signOut() // Clear user
        
        viewModel = CoffeeHomeScreenViewModel(accountService, fakeService, dispatchers)
        
        viewModel.navigationEvent.test {
            backgroundScope.launch {
                 viewModel.viewData.collect {}
            }
            assertEquals(NavigationEvent.NavigateToLogin, awaitItem())
        }
    }
}
