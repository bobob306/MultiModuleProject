package com.bsdevs.babycare.presentation.home

import app.cash.turbine.test
import com.bsdevs.babycare.data.repository.BabyCareRepositoryImpl
import com.bsdevs.babycare.data.repository.FakeBabyCareFirestoreService
import java.time.LocalDate

import com.bsdevs.babycare.network.BabyCareFirestoreService
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.repository.UserRepository
import com.bsdevs.common.result.Result
import com.bsdevs.data.ScreenDataMapper
import com.bsdevs.network.repository.ScreenRepository
import io.mockk.*
import com.bsdevs.babycare.presentation.common.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BabyCareHomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var fakeService: FakeBabyCareFirestoreService
    private lateinit var repository: BabyCareRepositoryImpl
    private lateinit var accountService: FakeAccountService
    private lateinit var screenRepository: ScreenRepository
    private lateinit var mapper: ScreenDataMapper
    private lateinit var viewModel: BabyCareHomeViewModel
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
        val userRepo = mockk<UserRepository>(relaxed = true)
        val timeProvider = mockk<TimeProvider>(relaxed = true)
        every { timeProvider.currentLocalDate() } returns LocalDate.of(2026, 9, 1)
        repository = BabyCareRepositoryImpl(fakeService, userRepo, dispatchers, timeProvider)
        accountService = FakeAccountService(userId)
        
        screenRepository = mockk(relaxed = true)
        mapper = mockk(relaxed = true)
        
        coEvery { screenRepository.getScreenFlow("baby_home", any()) } returns flowOf(Result.Success(emptyList()))
        every { mapper.mapToData(any()) } returns emptyList()
        
        // viewModel init triggers initialLoad which uses repository
        viewModel = BabyCareHomeViewModel(repository, accountService, screenRepository, mapper, dispatchers)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial load moves from Loading to Success when data exists`() = runTest {
        // Given
        val date = "2026-08-26"
        val event = mapOf("id" to "e1", "type" to "FEEDING", "time" to "10:00", "dateTimeString" to "$date 10:00")
        fakeService.injectMonth(userId, "2026-08", mapOf("days" to mapOf(date to listOf(event))))

        // Add a measurement
        val measurement = mapOf("id" to "m1", "type" to "MEASUREMENT", "weight" to 3.5, "dateTimeString" to "$date 11:00")
        fakeService.saveMeasurement(userId, "m1", measurement)

        // When - triggering a refresh or just observing (init already triggered it)
        viewModel.refreshData()

        // Then
        viewModel.viewData.test {
            val result = awaitItem()
            assertTrue(result is Result.Success)
            val data = (result as Result.Success).data
            assertEquals(2, data.activityFeed.filterIsInstance<HomeFeedItem.ActivityRow>().size)
            assertEquals("Last feed: 10:00", data.lastFeeding)
            assertEquals("Last: 3.50kg", data.lastMeasurement)
        }
    }

    @Test
    fun `viewModel loads dynamic UI configuration on init`() = runTest {
        // Given
        val mockData = mockk<com.bsdevs.data.NetworkScreenData>()
        val dynamicUi = listOf(mockData)
        every { mapper.mapToData(any()) } returns dynamicUi
        coEvery { screenRepository.getScreenFlow("baby_home", any()) } returns flowOf(Result.Success(listOf(mockk())))
        
        // When recreating VM to trigger init
        val vm = BabyCareHomeViewModel(repository, accountService, screenRepository, mapper, dispatchers)
        
        // Then
        vm.viewData.test {
            // It should eventually reach a Success state containing our dynamic UI
            // We use a timeout-safe check by waiting for the Success state
            var result = awaitItem()
            while (result !is Result.Success) {
                result = awaitItem()
            }
            
            assertEquals(dynamicUi, (result).data.dynamicUi)
        }
    }

    @Test
    fun `toggling filter updates viewData correctly`() = runTest {
        // Given
        val date = "2026-08-26"
        val feeding = mapOf("id" to "f1", "type" to "FEEDING", "time" to "10:00", "dateTimeString" to "$date 10:00")
        val nappy = mapOf("id" to "n1", "type" to "NAPPY", "nappyType" to "Wet", "time" to "11:00", "dateTimeString" to "$date 11:00")
        fakeService.injectMonth(userId, "2026-08", mapOf("days" to mapOf(date to listOf(feeding, nappy))))
        
        viewModel.refreshData()

        // When
        viewModel.toggleActivityFilter(ActivityFilter.NAPPY)

        // Then
        val result = viewModel.viewData.value as Result.Success
        val rows = result.data.activityFeed.filterIsInstance<HomeFeedItem.ActivityRow>()
        assertEquals(1, rows.size)
        assertTrue(rows.first().activity is com.bsdevs.babycare.presentation.common.BabyActivity.Nappy)
        assertEquals(ActivityFilter.NAPPY, result.data.currentFilter)
    }

    @Test
    fun `toggling header collapse hides activity rows`() = runTest {
        // Given
        val today = java.time.LocalDate.now().toString()
        val feeding = mapOf("id" to "f1", "type" to "FEEDING", "time" to "10:00", "dateTimeString" to "$today 10:00")
        fakeService.injectMonth(userId, today.substring(0, 7), mapOf("days" to mapOf(today to listOf(feeding))))
        
        viewModel.refreshData()
        
        val headerTitle = "Today"

        // When
        viewModel.toggleHeaderCollapse(headerTitle)

        // Then
        val result = viewModel.viewData.value as Result.Success
        val rows = result.data.activityFeed.filterIsInstance<HomeFeedItem.ActivityRow>()
        assertEquals(0, rows.size)
        assertTrue(result.data.collapsedHeaders.contains(headerTitle))
    }

    @Test
    fun `loadMore appends data and updates loading state`() = runTest {
        // Given
        val monthAug = "2026-08"
        val monthJuly = "2026-07"
        val eventAug = mapOf("id" to "aug1", "type" to "FEEDING", "time" to "10:00", "dateTimeString" to "2026-08-01 10:00")
        val eventJuly = mapOf("id" to "july1", "type" to "FEEDING", "time" to "10:00", "dateTimeString" to "2026-07-01 10:00")
        
        fakeService.injectMonth(userId, monthAug, mapOf("days" to mapOf("2026-08-01" to listOf(eventAug))))
        fakeService.injectMonth(userId, monthJuly, mapOf("days" to mapOf("2026-07-01" to listOf(eventJuly))))

        viewModel.refreshData() // Loads August

        // When
        viewModel.loadMore()

        // Then
        viewModel.viewData.filter { it is Result.Success && it.data.activityFeed.filterIsInstance<HomeFeedItem.ActivityRow>().size == 2 }.test {
            val state = awaitItem() as Result.Success
            assertEquals(2, state.data.activityFeed.filterIsInstance<HomeFeedItem.ActivityRow>().size)
        }
    }

    @Test
    fun `refreshData resets state and reloads from scratch`() = runTest {
        // Given
        fakeService.injectMonth(userId, "2026-08", mapOf("days" to mapOf("2026-08-01" to listOf(mapOf("id" to "e1", "type" to "FEEDING")))))
        viewModel.refreshData()
        
        // Modify service
        fakeService.injectMonth(userId, "2026-08", mapOf("days" to mapOf("2026-08-01" to listOf(mapOf("id" to "e2", "type" to "FEEDING")))))

        // When
        viewModel.refreshData()

        // Then
        val result = viewModel.viewData.value as Result.Success
        val rows = result.data.activityFeed.filterIsInstance<HomeFeedItem.ActivityRow>()
        assertEquals(1, rows.size)
        assertEquals("e2", (rows.first().activity as com.bsdevs.babycare.presentation.common.BabyActivity.Feeding).dto.id)
    }

    @Test
    fun `header counts correctly summarize day activities`() = runTest {
        // Given
        val date = "2026-08-26"
        val events = listOf(
            mapOf("id" to "f1", "type" to "FEEDING", "time" to "10:00", "dateTimeString" to "$date 10:00"),
            mapOf("id" to "f2", "type" to "FEEDING", "time" to "12:00", "dateTimeString" to "$date 12:00"),
            mapOf("id" to "n1", "type" to "NAPPY", "nappyType" to "Wet", "time" to "11:00", "dateTimeString" to "$date 11:00")
        )
        fakeService.injectMonth(userId, "2026-08", mapOf("days" to mapOf(date to events)))

        // Measurement
        fakeService.saveMeasurement(userId, "m1", mapOf("id" to "m1", "type" to "MEASUREMENT", "dateTimeString" to "$date 13:00"))

        // When
        viewModel.refreshData()

        // Then
        val result = viewModel.viewData.value as Result.Success
        val header = result.data.activityFeed.filterIsInstance<HomeFeedItem.Header>().first()
        assertEquals(2, header.feedingCount)
        assertEquals(1, header.nappyCount)
        assertEquals(0, header.temperatureCount)
        assertEquals(1, header.measurementCount)
    }

    @Test
    fun `mapToBabyActivity recovers corrupted nappy types`() = runTest {
        // Given: type is set to "Dirty" instead of "NAPPY", and nappyType is missing
        val date = "2026-08-26"
        val corruptedNappy = mapOf("id" to "n1", "type" to "Dirty", "time" to "11:00", "dateTimeString" to "$date 11:00")
        fakeService.injectMonth(userId, "2026-08", mapOf("days" to mapOf(date to listOf(corruptedNappy))))

        // When
        viewModel.refreshData()

        // Then
        val result = viewModel.viewData.value as Result.Success
        val nappyActivity = (result.data.activityFeed.filterIsInstance<HomeFeedItem.ActivityRow>().first().activity as com.bsdevs.babycare.presentation.common.BabyActivity.Nappy)
        assertEquals("Dirty", nappyActivity.dto.type)
    }

    @Test
    fun `summary strings are derived from latest events across months`() = runTest {
        // Given data in two months
        val eventAug = mapOf("id" to "aug1", "type" to "TEMPERATURE", "temperature" to 36.6, "time" to "09:00", "dateTimeString" to "2026-08-01 09:00")
        val eventJuly = mapOf("id" to "july1", "type" to "FEEDING", "time" to "23:00", "dateTimeString" to "2026-07-31 23:00")
        
        fakeService.injectMonth(userId, "2026-08", mapOf("days" to mapOf("2026-08-01" to listOf(eventAug))))
        fakeService.injectMonth(userId, "2026-07", mapOf("days" to mapOf("2026-07-31" to listOf(eventJuly))))

        // Initial load only gets Aug
        viewModel.refreshData()
        var state = (viewModel.viewData.value as Result.Success).data
        assertEquals("Last temp: 36.6°C", state.lastTemperature)
        assertNull(state.lastFeeding) // Not loaded yet

        // When loading more (gets July)
        viewModel.loadMore()

        // Then
        state = (viewModel.viewData.value as Result.Success).data
        assertEquals("Last feed: 23:00", state.lastFeeding)
    }

    @Test
    fun `loadMore does nothing if all headers are collapsed`() = runTest {
        // Given
        val date = "2026-08-26"
        fakeService.injectMonth(userId, "2026-08", mapOf("days" to mapOf(date to listOf(mapOf("id" to "e1", "type" to "FEEDING")))))
        viewModel.refreshData()
        
        // Collapse the only header
        viewModel.toggleHeaderCollapse("Today")

        // When
        viewModel.loadMore()

        // Then
        assertFalse((viewModel.viewData.value as Result.Success).data.isLoadingMore)
        // Verify loadMoreData wasn't actually utilized (pagination shouldn't trigger)
        // Since we are using a Fake, we check if the cache size didn't increase if we had more months
        // Let's add an older month to check
        fakeService.injectMonth(userId, "2026-07", mapOf("days" to mapOf("2026-07-01" to listOf(mapOf("id" to "e2", "type" to "FEEDING")))))
        
        viewModel.loadMore()
        assertEquals(1, repository.cachedDays.value.size) // Still just August
    }

    @Test
    fun `viewModel handles repository error correctly`() = runTest {
        // Given a repo that fails during the initial load triggered by ViewModel init
        val userRepo = mockk<UserRepository>(relaxed = true)
        val crashingService = object : BabyCareFirestoreService by fakeService {
            override suspend fun getLatestMonthId(userId: String, forceRefresh: Boolean) = throw RuntimeException("Network Error")
        }
        val timeProvider = mockk<TimeProvider>(relaxed = true)
        every { timeProvider.currentLocalDate() } returns LocalDate.of(2026, 9, 1)
        val errorRepo = BabyCareRepositoryImpl(crashingService, userRepo, dispatchers, timeProvider)
        
        // We need to wait for the viewModelScope to finish the initialLoad call
        val errorViewModel = BabyCareHomeViewModel(errorRepo, accountService, screenRepository, mapper, dispatchers)

        // Then
        errorViewModel.viewData.test {
            // It might emit Loading first, then Error
            var lastResult = awaitItem()
            if (lastResult is Result.Loading) {
                lastResult = awaitItem()
            }
            assertTrue("Expected Result.Error but got $lastResult", lastResult is Result.Error)
        }
    }
}
