package com.bsdevs.babycare.data.repository

import app.cash.turbine.test
import com.bsdevs.babycare.network.DailyLogDto
import com.bsdevs.babycare.network.UnifiedEventDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.YearMonth
import com.bsdevs.babycare.network.BabyCareFirestoreService
import com.bsdevs.common.DispatcherProvider
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class BabyCareRepositoryImplTest {

    private lateinit var fakeService: FakeBabyCareFirestoreService
    private lateinit var repository: BabyCareRepositoryImpl
    private lateinit var dispatchers: DispatcherProvider

    private val userId = "testUser"

    @Before
    fun setUp() {
        val testDispatcher = UnconfinedTestDispatcher()
        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }
        fakeService = FakeBabyCareFirestoreService()
        repository = BabyCareRepositoryImpl(fakeService, dispatchers)
    }

    // --- INITIAL LOAD TESTS ---

    @Test
    fun `loadInitialData handles no data case`() = runTest {
        val result = repository.loadInitialData(userId, 20)

        assertNull(result.nextAnchorMonth)
        assertFalse(result.hasMoreData)
        repository.cachedDays.test {
            assertEquals(emptyList<DailyLogDto>(), awaitItem())
        }
    }

    @Test
    fun `loadInitialData fetches data and calculates next anchor`() = runTest {
        val monthAug = "2026-08"
        val monthMay = "2026-05"
        fakeService.injectMonth(userId, monthAug, mapOf("days" to emptyMap<String, Any>()))
        fakeService.injectMonth(userId, monthMay, mapOf("days" to emptyMap<String, Any>()))

        val result = repository.loadInitialData(userId, 20)

        assertEquals(YearMonth.of(2026, 5), result.nextAnchorMonth)
        assertTrue(result.hasMoreData)
    }

    // --- PAGINATION TESTS ---

    @Test
    fun `loadMoreData fetches next month and updates anchor`() = runTest {
        val monthAug = "2026-08"
        val monthMay = "2026-05"
        val monthJan = "2026-01"
        fakeService.injectMonth(userId, monthAug, mapOf("days" to emptyMap<String, Any>()))
        fakeService.injectMonth(userId, monthMay, mapOf("days" to emptyMap<String, Any>()))
        fakeService.injectMonth(userId, monthJan, mapOf("days" to emptyMap<String, Any>()))

        repository.loadInitialData(userId, 20) // Current month is Aug, next is May

        val result = repository.loadMoreData(userId, 20) // Loads May, next is Jan

        assertEquals(YearMonth.of(2026, 1), result.nextAnchorMonth)
        assertTrue(result.hasMoreData)
    }

    @Test
    fun `loadMoreData returns hasMoreData false when no older months exist`() = runTest {
        val monthAug = "2026-08"
        fakeService.injectMonth(userId, monthAug, mapOf("days" to emptyMap<String, Any>()))

        repository.loadInitialData(userId, 20)
        val result = repository.loadMoreData(userId, 20)

        assertNull(result.nextAnchorMonth)
        assertFalse(result.hasMoreData)
    }

    @Test
    fun `refreshData resets anchor and fetches initial data`() = runTest {
        val monthAug = "2026-08"
        fakeService.injectMonth(userId, monthAug, mapOf("days" to mapOf("2026-08-01" to emptyList<Any>())))
        
        val result = repository.refreshData(userId, 20)
        
        assertNotNull(result)
        // Verify cache was populated
        assertEquals(1, repository.cachedDays.value.size)
    }

    // --- CRUD TESTS ---

    @Test
    fun `saveActivityEvent updates existing document and cache`() = runTest {
        val date = "2026-08-26"
        val event = UnifiedEventDto(id = "e1", type = "FEEDING", time = "10:00", dateTimeString = "$date 10:00")

        repository.saveActivityEvent(userId, date, event)

        repository.cachedDays.test {
            val cached = awaitItem()
            assertTrue(cached.any { it.date == date && it.events.any { e -> e.id == "e1" } })
        }
        assertNotNull(fakeService.fetchMonthDocument(userId, "2026-08"))
    }

    @Test
    fun `updateActivityEvent updates document and cache`() = runTest {
        val date = "2026-08-26"
        val event = UnifiedEventDto(id = "e1", type = "FEEDING", comment = "Old")
        repository.saveActivityEvent(userId, date, event)

        val updated = event.copy(comment = "New")
        repository.updateActivityEvent(userId, date, "e1", updated)

        repository.cachedDays.test {
            val cached = awaitItem()
            assertEquals("New", cached.first().events.first().comment)
        }
    }

    @Test
    fun `deleteActivityEvent updates cache`() = runTest {
        val date = "2026-08-26"
        val event = UnifiedEventDto(id = "e1", type = "FEEDING", time = "10:00", dateTimeString = "$date 10:00")
        repository.saveActivityEvent(userId, date, event)

        repository.deleteActivityEvent(userId, date, "e1")

        repository.cachedDays.test {
            val cached = awaitItem()
            assertTrue(cached.first { it.date == date }.events.isEmpty())
        }
    }

    // --- CACHE & DATA PARSING TESTS ---

    @Test
    fun `cache maintains descending chronological order`() = runTest {
        val date1 = "2026-08-01"
        val date2 = "2026-08-15"
        val event1 = UnifiedEventDto(id = "e1", type = "FEEDING", time = "10:00", dateTimeString = "$date1 10:00")
        val event2 = UnifiedEventDto(id = "e2", type = "FEEDING", time = "10:00", dateTimeString = "$date2 10:00")
        
        repository.saveActivityEvent(userId, date1, event1)
        repository.saveActivityEvent(userId, date2, event2)

        repository.cachedDays.test {
            val cached = awaitItem()
            assertEquals(date2, cached[0].date)
            assertEquals(date1, cached[1].date)
        }
    }

    @Test
    fun `parseUnifiedEvent handles various temperature formats`() = runTest {
        val date = "2026-08-26"
        val monthId = "2026-08"
        val rawData = mapOf("days" to mapOf(date to listOf(
            mapOf("id" to "temp1", "type" to "TEMPERATURE", "temperature" to 37.5),
            mapOf("id" to "temp2", "type" to "TEMPERATURE", "temperature" to 37),
            mapOf("id" to "temp3", "type" to "TEMPERATURE", "temperature" to 38.2f)
        )))
        fakeService.injectMonth(userId, monthId, rawData)

        repository.loadInitialData(userId, 20)

        val cachedEvents = repository.cachedDays.value.first().events
        assertEquals(37.5, cachedEvents.first { it.id == "temp1" }.temperature!!, 0.01)
        assertEquals(37.0, cachedEvents.first { it.id == "temp2" }.temperature!!, 0.01)
        assertEquals(38.2, cachedEvents.first { it.id == "temp3" }.temperature!!, 0.01)
    }

    // --- LOOKUP TESTS ---

    @Test
    fun `getFeedingEventById returns from cache when available`() = runTest {
        val event = UnifiedEventDto(id = "cache_id", type = "FEEDING")
        repository.saveActivityEvent(userId, "2026-08-26", event)

        val result = repository.getFeedingEventById(userId, "cache_id")

        assertEquals("cache_id", result?.id)
    }

    @Test
    fun `getFeedingEventById falls back to network on cache miss`() = runTest {
        val eventId = "network_id"
        val rawData = mapOf("days" to mapOf("2026-08-01" to listOf(mapOf("id" to eventId, "type" to "FEEDING"))))
        fakeService.injectMonth(userId, "2026-08", rawData)

        // Cache is empty initially
        val result = repository.getFeedingEventById(userId, eventId)

        assertEquals(eventId, result?.id)
    }

    // --- EDGE CASE ERROR HANDLING ---

    @Test
    fun `fetchMonthDocument handles missing days field gracefully`() = runTest {
        val monthId = "2026-08"
        fakeService.injectMonth(userId, monthId, emptyMap()) // Document exists but is empty

        repository.loadInitialData(userId, 20)

        repository.cachedDays.test {
            assertEquals(emptyList<DailyLogDto>(), awaitItem())
        }
    }

    @Test(expected = RuntimeException::class)
    fun `loadInitialData rethrows unexpected exceptions`() = runTest {
        // Create a fake that throws
        val crashingService = object : BabyCareFirestoreService by fakeService {
            override suspend fun getLatestMonthId(userId: String) = throw RuntimeException("Firestore Down")
        }
        val repo = BabyCareRepositoryImpl(crashingService, dispatchers)

        // When/Then
        repo.loadInitialData(userId, 20)
    }
}
