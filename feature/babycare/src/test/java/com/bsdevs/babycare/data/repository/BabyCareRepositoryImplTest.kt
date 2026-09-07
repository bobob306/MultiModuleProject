package com.bsdevs.babycare.data.repository

import app.cash.turbine.test
import com.bsdevs.babycare.data.BabyCareRepositoryImpl
import com.bsdevs.network.dto.DailyLogDto
import com.bsdevs.network.dto.UnifiedEventDto
import com.bsdevs.babycare.presentation.common.TimeProvider
import com.bsdevs.data.repository.UserRepository
import com.bsdevs.data.SyncManager
import com.bsdevs.data.local.dao.BabyEventDao
import com.bsdevs.data.local.entities.BabyEventEntity
import com.bsdevs.network.dto.UserDto
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var babyEventDao: BabyEventDao
    private lateinit var syncManager: SyncManager
    private var testDate = LocalDate.of(2026, 8, 26)

    private val userId = "testUser"
    private val babyId = "baby123"

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
        every { userRepository.userProfile } returns MutableStateFlow(UserDto(id = userId, babyId = babyId))
        
        timeProvider = mockk {
            every { currentLocalDate() } answers { testDate }
        }
        babyEventDao = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        
        repository = BabyCareRepositoryImpl(
            apiService = fakeService,
            userRepository = userRepository,
            dispatchers = dispatchers,
            timeProvider = timeProvider,
            babyEventDao = babyEventDao,
            syncManager = syncManager
        )
    }

    @Test
    fun `loadInitialData loads from DB when offline or starting`() = runTest {
        val localEvent = UnifiedEventDto(id = "local1", type = "FEEDING")
        val entity = BabyEventEntity("local1", babyId, "2026-08-26", localEvent)
        coEvery { babyEventDao.getEvents(babyId) } returns flowOf(listOf(entity))

        repository.loadInitialData(userId, 2)

        val cached = repository.cachedDays.value
        assertEquals(1, cached.size)
        assertEquals("local1", cached.first().events.first().id)
    }

    @Test
    fun `saveActivityEvent inserts into DB with pending sync flag`() = runTest {
        val date = "2026-08-26"
        val event = UnifiedEventDto(id = "e1", type = "FEEDING")

        repository.saveActivityEvent(userId, date, event)

        coVerify { babyEventDao.insertEvents(match { it.first().isPendingSync }) }
    }

    @Test
    fun `sync pushes pending events to firestore`() = runTest {
        val event = UnifiedEventDto(id = "pending1", type = "FEEDING")
        val entity = BabyEventEntity("pending1", babyId, "2026-08-26", event, isPendingSync = true)
        coEvery { babyEventDao.getPendingSync() } returns listOf(entity)

        repository.sync()

        // Verify it was saved to firestore (via fake service)
        assertNotNull(fakeService.fetchMonthDocument(userId, "2026-08"))
        
        // Verify sync flag is cleared in DB
        coVerify { babyEventDao.insertEvents(match { !it.first().isPendingSync }) }
    }
}
