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

@OptIn(ExperimentalCoroutinesApi::class)
class BabyCareRepositoryImplTest {

    private lateinit var fakeService: FakeBabyCareFirestoreService
    private lateinit var repository: BabyCareRepositoryImpl

    private val userId = "testUser"

    @Before
    fun setUp() {
        fakeService = FakeBabyCareFirestoreService()
        repository = BabyCareRepositoryImpl(fakeService)
    }

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
        // Given
        val monthAug = "2026-08"
        val monthMay = "2026-05"
        fakeService.injectMonth(userId, monthAug, mapOf("days" to emptyMap<String, Any>()))
        fakeService.injectMonth(userId, monthMay, mapOf("days" to emptyMap<String, Any>()))

        // When
        val result = repository.loadInitialData(userId, 20)

        // Then
        assertEquals(YearMonth.of(2026, 5), result.nextAnchorMonth)
        assertTrue(result.hasMoreData)
    }

    @Test
    fun `loadMoreData fetches next month and updates anchor`() = runTest {
        // Given
        val monthAug = "2026-08"
        val monthMay = "2026-05"
        val monthJan = "2026-01"
        fakeService.injectMonth(userId, monthAug, mapOf("days" to emptyMap<String, Any>()))
        fakeService.injectMonth(userId, monthMay, mapOf("days" to emptyMap<String, Any>()))
        fakeService.injectMonth(userId, monthJan, mapOf("days" to emptyMap<String, Any>()))

        repository.loadInitialData(userId, 20) // sets anchor to May

        // When
        val result = repository.loadMoreData(userId, 20)

        // Then
        assertEquals(YearMonth.of(2026, 1), result.nextAnchorMonth)
        assertTrue(result.hasMoreData)
    }

    @Test
    fun `saveActivityEvent updates existing document and cache`() = runTest {
        val date = "2026-08-26"
        val event = UnifiedEventDto(id = "e1", type = "FEEDING", time = "10:00", dateTimeString = "$date 10:00")

        // When
        repository.saveActivityEvent(userId, date, event)

        // Then
        repository.cachedDays.test {
            val cached = awaitItem()
            assertTrue(cached.any { it.date == date && it.events.any { e -> e.id == "e1" } })
        }
        val stored = fakeService.fetchMonthDocument(userId, "2026-08")
        assertNotNull(stored)
    }

    @Test
    fun `deleteActivityEvent updates cache`() = runTest {
        val date = "2026-08-26"
        val event = UnifiedEventDto(id = "e1", type = "FEEDING", time = "10:00", dateTimeString = "$date 10:00")
        repository.saveActivityEvent(userId, date, event)

        // When
        repository.deleteActivityEvent(userId, date, "e1")

        // Then
        repository.cachedDays.test {
            val cached = awaitItem()
            assertTrue(cached.first { it.date == date }.events.isEmpty())
        }
    }

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
        fakeService.injectMonth(userId, monthId, rawData as Map<String, Any>)

        repository.loadInitialData(userId, 20)

        val cachedEvents = repository.cachedDays.value.first().events
        assertEquals(37.5, cachedEvents.first { it.id == "temp1" }.temperature!!, 0.01)
        assertEquals(37.0, cachedEvents.first { it.id == "temp2" }.temperature!!, 0.01)
        assertEquals(38.2, cachedEvents.first { it.id == "temp3" }.temperature!!, 0.01)
    }
}
