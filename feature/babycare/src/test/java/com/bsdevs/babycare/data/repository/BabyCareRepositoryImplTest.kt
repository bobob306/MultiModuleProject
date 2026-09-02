package com.bsdevs.babycare.data.repository

import app.cash.turbine.test
import com.bsdevs.babycare.network.DailyLogDto
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.babycare.presentation.common.TimeProvider
import com.bsdevs.network.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import com.bsdevs.babycare.network.BabyCareFirestoreService
import com.bsdevs.common.DispatcherProvider
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class BabyCareRepositoryImplTest {

    private lateinit var fakeService: FakeBabyCareFirestoreService
    private lateinit var repository: BabyCareRepositoryImpl
    private lateinit var userRepository: UserRepository
    private lateinit var dispatchers: DispatcherProvider
    private lateinit var timeProvider: TimeProvider
    private var testDate = LocalDate.of(2026, 8, 26)

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
        userRepository = mockk(relaxed = true)
        timeProvider = mockk {
            every { currentLocalDate() } answers { testDate }
        }
        repository = BabyCareRepositoryImpl(
            apiService = fakeService,
            userRepository = userRepository,
            dispatchers = dispatchers,
            timeProvider = timeProvider
        )
    }

    // --- INITIAL LOAD TESTS ---

    @Test
    fun `loadInitialData handles no data case`() = runTest {
        val result = repository.loadInitialData(userId, 2)

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

        val result = repository.loadInitialData(userId, 2)

        // Aug is sparse, so it pulls May. Next anchor is null because nothing is before May.
        assertNull(result.nextAnchorMonth)
        assertFalse(result.hasMoreData)
    }

    @Test
    fun `loadInitialData fetches two months when day is 8th or less and current month has data`() = runTest {
        // Setup: Date is Sep 5th
        testDate = LocalDate.of(2026, 9, 5)

        // Inject data for Sep and Aug
        val monthSep = "2026-09"
        val monthAug = "2026-08"
        val monthJuly = "2026-07"
        fakeService.injectMonth(userId, monthSep, mapOf("days" to mapOf("2026-09-01" to listOf(
            mapOf("id" to "f1", "type" to "FEEDING", "dateTimeString" to "2026-09-01 10:00")
        ))))
        fakeService.injectMonth(userId, monthAug, mapOf("days" to mapOf("2026-08-01" to emptyList<Any>())))
        fakeService.injectMonth(userId, monthJuly, mapOf("days" to mapOf("2026-07-01" to emptyList<Any>())))

        val result = repository.loadInitialData(userId, 20)

        // Verify: Both Sep and Aug should be in cache
        val cached = repository.cachedDays.value
        assertTrue("Should contain Sep data", cached.any { it.date == "2026-09-01" })
        assertTrue("Should contain Aug data", cached.any { it.date == "2026-08-01" })

        // Anchor should be July
        assertEquals(YearMonth.of(2026, 7), result.nextAnchorMonth)
        assertTrue(result.hasMoreData)
    }

    @Test
    fun `loadInitialData fetches two months when data is sparse even after 8th day`() = runTest {
        // Setup: Date is Sep 15th (after 8th)
        testDate = LocalDate.of(2026, 9, 15)

        val monthSep = "2026-09"
        val monthAug = "2026-08"
        // Only 2 feeds in Sep (sparse)
        fakeService.injectMonth(userId, monthSep, mapOf("days" to mapOf("2026-09-01" to listOf(
            mapOf("id" to "f1", "type" to "FEEDING", "dateTimeString" to "2026-09-01 10:00"),
            mapOf("id" to "f2", "type" to "FEEDING", "dateTimeString" to "2026-09-01 11:00")
        ))))
        fakeService.injectMonth(userId, monthAug, mapOf("days" to mapOf("2026-08-31" to listOf(
            mapOf("id" to "f3", "type" to "FEEDING", "dateTimeString" to "2026-08-31 10:00")
        ))))

        val result = repository.loadInitialData(userId, 20)

        val cached = repository.cachedDays.value
        assertTrue("Should contain Sep data", cached.any { it.date == "2026-09-01" })
        assertTrue("Should proactively fetch Aug data due to sparsity", cached.any { it.date == "2026-08-31" })
    }

    @Test
    fun `loadInitialData fetches only one month when day is after 8th and data is NOT sparse`() = runTest {
        // Setup: Date is Sep 9th
        testDate = LocalDate.of(2026, 9, 9)

        val monthSep = "2026-09"
        val monthAug = "2026-08"
        // 6 feeds in Sep (NOT sparse)
        val denseEvents = (1..6).map { 
            mapOf("id" to "f$it", "type" to "FEEDING", "dateTimeString" to "2026-09-01 10:0$it")
        }
        fakeService.injectMonth(userId, monthSep, mapOf("days" to mapOf("2026-09-01" to denseEvents)))
        fakeService.injectMonth(userId, monthAug, mapOf("days" to mapOf("2026-08-01" to emptyList<Any>())))

        val result = repository.loadInitialData(userId, 20)

        // Verify: Only Sep should be in cache
        val cached = repository.cachedDays.value
        assertTrue("Should contain Sep data", cached.any { it.date == "2026-09-01" })
        assertFalse("Should NOT contain Aug data", cached.any { it.date == "2026-08-01" })

        // Anchor should be Aug
        assertEquals(YearMonth.of(2026, 8), result.nextAnchorMonth)
    }

    @Test
    fun `loadInitialData fetches current and previous when current is empty but exists`() = runTest {
        // Setup: Date is Sep 1st
        testDate = LocalDate.of(2026, 9, 1)

        val monthSep = "2026-09"
        val monthAug = "2026-08"
        // Sep exists but has no days
        fakeService.injectMonth(userId, monthSep, mapOf("days" to emptyMap<String, Any>()))
        fakeService.injectMonth(userId, monthAug, mapOf("days" to mapOf("2026-08-31" to emptyList<Any>())))

        val result = repository.loadInitialData(userId, 20)

        val cached = repository.cachedDays.value
        assertTrue("Should contain Aug data", cached.any { it.date == "2026-08-31" })

        // Anchor should be month before Aug if it exists, but here it doesn't
        assertNull(result.nextAnchorMonth)
    }

    // --- PAGINATION TESTS ---

    @Test
    fun `loadMoreData fetches next month and updates anchor`() = runTest {
        val monthAug = "2026-08"
        val monthMay = "2026-05"
        val monthJan = "2026-01"
        val monthOctPrev = "2025-10"
        fakeService.injectMonth(userId, monthAug, mapOf("days" to emptyMap<String, Any>()))
        fakeService.injectMonth(userId, monthMay, mapOf("days" to emptyMap<String, Any>()))
        fakeService.injectMonth(userId, monthJan, mapOf("days" to emptyMap<String, Any>()))
        fakeService.injectMonth(userId, monthOctPrev, mapOf("days" to emptyMap<String, Any>()))

        repository.loadInitialData(userId, 2) // Aug sparse, pulls May. Anchor is Jan.

        val result = repository.loadMoreData(userId, 2) // Loads Jan, next is Oct 2025

        assertEquals(YearMonth.of(2025, 10), result.nextAnchorMonth)
        assertTrue(result.hasMoreData)
    }

    @Test
    fun `loadMoreData returns hasMoreData false when no older months exist`() = runTest {
        val monthAug = "2026-08"
        fakeService.injectMonth(userId, monthAug, mapOf("days" to emptyMap<String, Any>()))

        repository.loadInitialData(userId, 2)
        val result = repository.loadMoreData(userId, 2)

        assertNull(result.nextAnchorMonth)
        assertFalse(result.hasMoreData)
    }

    @Test
    fun `refreshData resets anchor and fetches initial data bypassing cache`() = runTest {
        val monthAug = "2026-08"
        fakeService.injectMonth(userId, monthAug, mapOf("days" to mapOf("2026-08-01" to emptyList<Any>())))
        
        repository.loadInitialData(userId, 2)
        assertEquals(1, repository.cachedDays.value.size)

        // Add more data and refresh
        val monthJuly = "2026-07"
        fakeService.injectMonth(userId, monthJuly, mapOf("days" to mapOf("2026-07-01" to emptyList<Any>())))
        
        val result = repository.refreshData(userId, 2)
        
        assertNotNull(result)
        // Verify cache was reset. 
        // Aug is sparse, so it also fetches July. Total 2 days.
        assertEquals(2, repository.cachedDays.value.size)
        assertEquals("2026-08-01", repository.cachedDays.value.first().date)
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
    fun `saveActivityEvent for measurement updates separate collection and cache`() = runTest {
        val date = "2026-08-26"
        val event = UnifiedEventDto(id = "m1", type = "MEASUREMENT", weight = 3.5, dateTimeString = "$date 10:00")

        repository.saveActivityEvent(userId, date, event)

        // Check cache
        repository.cachedDays.test {
            val cached = awaitItem()
            assertTrue(cached.any { it.date == date && it.events.any { e -> e.id == "m1" } })
        }
        
        // Check measurements flow
        repository.measurements.test {
            val measurements = awaitItem()
            assertEquals(1, measurements.size)
            assertEquals("m1", measurements.first().id)
        }

        // Check fake database
        val measurements = fakeService.fetchAllMeasurements(userId)
        assertEquals(1, measurements.size)
        assertEquals("m1", measurements.first()["id"])
    }

    @Test
    fun `saveActivityEvent for vaccination updates separate collection and cache`() = runTest {
        val date = "2026-08-26"
        val event = UnifiedEventDto(id = "v1", type = "VACCINATION", vaccinationNames = listOf("HepB"), dateTimeString = "$date 10:00")

        repository.saveActivityEvent(userId, date, event)

        // Check cache
        repository.cachedDays.test {
            val cached = awaitItem()
            assertTrue(cached.any { it.date == date && it.events.any { e -> e.id == "v1" } })
        }
        
        // Check vaccinations flow
        repository.vaccinations.test {
            val vaccines = awaitItem()
            assertEquals(1, vaccines.size)
            assertEquals("v1", vaccines.first().id)
        }

        // Check fake database
        val vaccines = fakeService.fetchAllVaccinations(userId)
        assertEquals(1, vaccines.size)
        assertEquals("v1", vaccines.first()["id"])
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

    @Test
    fun `deleteActivityEvent for measurement updates separate collection and cache`() = runTest {
        val date = "2026-08-26"
        val event = UnifiedEventDto(id = "m1", type = "MEASUREMENT", weight = 3.5, dateTimeString = "$date 10:00")
        repository.saveActivityEvent(userId, date, event)

        repository.deleteActivityEvent(userId, date, "m1")

        // Check cache
        repository.cachedDays.test {
            val cached = awaitItem()
            assertTrue(cached.isEmpty() || cached.none { day -> day.events.any { it.id == "m1" } })
        }

        // Check measurements flow
        repository.measurements.test {
            val measurements = awaitItem()
            assertTrue(measurements.isEmpty())
        }
        
        // Check fake database
        val measurements = fakeService.fetchAllMeasurements(userId)
        assertTrue(measurements.isEmpty())
    }

    @Test
    fun `deleteActivityEvent for vaccination updates separate collection and cache`() = runTest {
        val date = "2026-08-26"
        val event = UnifiedEventDto(id = "v1", type = "VACCINATION", dateTimeString = "$date 10:00")
        repository.saveActivityEvent(userId, date, event)

        repository.deleteActivityEvent(userId, date, "v1")

        // Check cache
        repository.cachedDays.test {
            val cached = awaitItem()
            assertTrue(cached.isEmpty() || cached.none { day -> day.events.any { it.id == "v1" } })
        }

        // Check vaccinations flow
        repository.vaccinations.test {
            val vaccines = awaitItem()
            assertTrue(vaccines.isEmpty())
        }
        
        // Check fake database
        val vaccines = fakeService.fetchAllVaccinations(userId)
        assertTrue(vaccines.isEmpty())
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

        repository.loadInitialData(userId, 2)

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
    fun `loadInitialData merges measurements from separate collection`() = runTest {
        val date = "2026-08-26"
        val monthId = "2026-08"
        // Feeding event in months collection
        val monthData = mapOf("days" to mapOf(date to listOf(
            mapOf("id" to "feed1", "type" to "FEEDING", "dateTimeString" to "$date 10:00")
        )))
        fakeService.injectMonth(userId, monthId, monthData)

        // Measurement in measurements collection
        val measurement = mapOf("id" to "m1", "type" to "MEASUREMENT", "weight" to 3.5, "dateTimeString" to "$date 11:00")
        fakeService.saveMeasurement(userId, "m1", measurement)

        // Vaccination in vaccinations collection
        val vaccination = mapOf("id" to "v1", "type" to "VACCINATION", "vaccinationNames" to listOf("HepB"), "dateTimeString" to "$date 12:00")
        fakeService.saveVaccination(userId, "v1", vaccination)

        repository.loadInitialData(userId, 2)

        repository.cachedDays.test {
            val cached = awaitItem()
            val day = cached.first { it.date == date }
            assertEquals(3, day.events.size)
            assertTrue(day.events.any { it.id == "feed1" })
            assertTrue(day.events.any { it.id == "m1" })
            assertTrue(day.events.any { it.id == "v1" })
        }
    }

    @Test
    fun `fetchMonthDocument handles missing days field gracefully`() = runTest {
        val monthId = "2026-08"
        fakeService.injectMonth(userId, monthId, emptyMap()) // Document exists but is empty

        repository.loadInitialData(userId, 2)

        repository.cachedDays.test {
            assertEquals(emptyList<DailyLogDto>(), awaitItem())
        }
    }

    @Test(expected = RuntimeException::class)
    fun `loadInitialData rethrows unexpected exceptions`() = runTest {
        // Create a fake that throws
        val crashingService = object : BabyCareFirestoreService by fakeService {
            override suspend fun getLatestMonthId(userId: String, forceRefresh: Boolean) = throw RuntimeException("Firestore Down")
        }
        val repo = BabyCareRepositoryImpl(crashingService, userRepository, dispatchers, timeProvider)

        // When/Then
        repo.loadInitialData(userId, 2)
    }

    // --- ADDITIONAL ROBUSTNESS TESTS ---

    @Test
    fun `clearCache resets all flows and anchor`() = runTest {
        // Setup: Inject some data and load it
        fakeService.injectMonth(userId, "2026-08", mapOf("days" to mapOf("2026-08-01" to emptyList<Any>())))
        repository.loadInitialData(userId, 2)
        
        assertTrue(repository.cachedDays.value.isNotEmpty())
        
        // When
        repository.clearCache()
        
        // Then
        assertTrue(repository.cachedDays.value.isEmpty())
        assertTrue(repository.measurements.value.isEmpty())
        
        // Verify anchor is reset by checking loadMoreData
        val result = repository.loadMoreData(userId, 2)
        assertNull(result.nextAnchorMonth)
        assertFalse(result.hasMoreData)
    }

    @Test
    fun `loadInitialData skips network when cache is populated and forceRefresh is false`() = runTest {
        // Setup: Mock service to count calls if possible, or use a flag. 
        // Since we have a Fake, we can just check if it was called.
        val callCount = mutableMapOf<String, Int>()
        val countingService = object : BabyCareFirestoreService by fakeService {
            override suspend fun getLatestMonthId(userId: String, forceRefresh: Boolean): String? {
                callCount["getLatestMonthId"] = (callCount["getLatestMonthId"] ?: 0) + 1
                return fakeService.getLatestMonthId(userId, forceRefresh)
            }
        }
        val repo = BabyCareRepositoryImpl(countingService, userRepository, dispatchers, timeProvider)
        
        // First call - should hit network
        fakeService.injectMonth(userId, "2026-08", mapOf("days" to mapOf("2026-08-01" to emptyList<Any>())))
        repo.loadInitialData(userId, 2)
        assertEquals(1, callCount["getLatestMonthId"])
        
        // Second call - should skip network
        repo.loadInitialData(userId, 2, forceRefresh = false)
        assertEquals(1, callCount["getLatestMonthId"])
        
        // Third call with forceRefresh - should hit network again
        repo.loadInitialData(userId, 2, forceRefresh = true)
        assertEquals(2, callCount["getLatestMonthId"])
    }

    @Test
    fun `loadMoreData returns immediately when anchor is null`() = runTest {
        // Setup: Load initial with no data so anchor is null
        repository.loadInitialData(userId, 2)
        
        val result = repository.loadMoreData(userId, 2)
        
        assertNull(result.nextAnchorMonth)
        assertFalse(result.hasMoreData)
    }

    @Test
    fun `getFeedingEventById returns null on total cache and network miss`() = runTest {
        val result = repository.getFeedingEventById(userId, "non_existent")
        assertNull(result)
    }

    @Test
    fun `saveActivityEvent does not update cache if network fails`() = runTest {
        val crashingService = object : BabyCareFirestoreService by fakeService {
            override suspend fun saveEvent(userId: String, monthId: String, date: String, event: Map<String, Any?>) {
                throw RuntimeException("Network Error")
            }
        }
        val repo = BabyCareRepositoryImpl(crashingService, userRepository, dispatchers, timeProvider)
        val date = "2026-08-26"
        val event = UnifiedEventDto(id = "e1", type = "FEEDING", time = "10:00", dateTimeString = "$date 10:00")

        try {
            repo.saveActivityEvent(userId, date, event)
        } catch (e: Exception) {
            // Expected
        }

        // Cache should still be empty
        assertTrue(repo.cachedDays.value.isEmpty())
    }
}
