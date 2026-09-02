package com.bsdevs.babycare.presentation.vaccination

import app.cash.turbine.test
import com.bsdevs.babycare.data.repository.BabyCareRepositoryImpl
import com.bsdevs.babycare.data.repository.FakeBabyCareFirestoreService
import com.bsdevs.babycare.presentation.common.TimeProvider
import com.bsdevs.babycare.presentation.home.FakeAccountService
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class VaccinationDataViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var fakeService: FakeBabyCareFirestoreService
    private lateinit var repository: BabyCareRepositoryImpl
    private lateinit var accountService: FakeAccountService
    private lateinit var viewModel: VaccinationDataViewModel
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
        
        viewModel = VaccinationDataViewModel(
            accountService,
            repository,
            dispatchers = dispatchers,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `viewModel groups vaccinations by seriesId correctly`() = runTest {
        // Given
        val date = "2026-08-26"
        val v1 = mapOf("id" to "v1", "type" to "VACCINATION", "vaccinationNames" to listOf("HepB 1"), "seriesId" to "hep_b", "dateTimeString" to "$date 10:00")
        val v2 = mapOf("id" to "v2", "type" to "VACCINATION", "vaccinationNames" to listOf("HepB 2"), "seriesId" to "hep_b", "dateTimeString" to "$date 12:00")
        
        fakeService.saveVaccination(userId, "v1", v1)
        fakeService.saveVaccination(userId, "v2", v2)

        // Force repo to load data
        repository.loadInitialData(userId, 20)

        // Then
        viewModel.groupedVaccinations.test {
            val state = awaitItem()
            assertEquals(1, state.size)
            
            val hepBGroup = state.find { it.seriesId == "hep_b" }
            assertNotNull(hepBGroup)
            assertEquals(2, hepBGroup!!.vaccinations.size)
        }
    }

    @Test
    fun `deleteVaccination calls repository deleteActivityEvent`() = runTest {
        // Given
        val repoSpy = spyk(repository)
        val vm = VaccinationDataViewModel(accountService, repoSpy, dispatchers = dispatchers)
        
        // When
        vm.deleteVaccination("2026-08-26", "v1")
        
        // Then
        coVerify { repoSpy.deleteActivityEvent(userId, "2026-08-26", "v1") }
    }
}
