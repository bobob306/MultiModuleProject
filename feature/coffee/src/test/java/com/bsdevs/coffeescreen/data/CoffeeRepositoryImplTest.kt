package com.bsdevs.coffeescreen.data

import com.bsdevs.coffeescreen.network.CoffeeApiService
import com.bsdevs.network.dto.CoffeeDto
import com.bsdevs.network.dto.ShotDto
import com.bsdevs.network.dto.UserDto
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.SyncManager
import com.bsdevs.data.local.dao.CoffeeDao
import com.bsdevs.data.local.entities.CoffeeEntity
import com.bsdevs.data.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoffeeRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var apiService: CoffeeApiService
    private lateinit var coffeeDao: CoffeeDao
    private lateinit var userRepository: UserRepository
    private lateinit var syncManager: SyncManager
    private lateinit var repository: CoffeeRepositoryImpl
    private lateinit var dispatchers: DispatcherProvider

    private val userId = "u1"

    @Before
    fun setUp() {
        apiService = mockk(relaxed = true)
        coffeeDao = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        
        every { userRepository.userProfile } returns MutableStateFlow(UserDto(id = userId))
        every { coffeeDao.getCoffeeLogs(userId) } returns flowOf(emptyList())

        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }
        repository = CoffeeRepositoryImpl(apiService, dispatchers, coffeeDao, userRepository, syncManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `loadInitialData fetches from network and saves to DB`() = runTest {
        val coffeeList = listOf(CoffeeDto(id = "c1", roastDate = "2026-08-25"))
        coEvery { apiService.getAllCoffee(userId) } returns coffeeList

        repository.loadInitialData(userId)

        coVerify { coffeeDao.insertCoffee(match { it.first().id == "c1" }) }
        assertEquals(1, repository.allCoffee.value.size)
    }

    @Test
    fun `uploadCoffee updates local state immediately even if network fails`() = runTest {
        val coffee = CoffeeDto(id = "new_coffee", label = "Pact")
        coEvery { apiService.uploadCoffee(userId, coffee) } throws RuntimeException("Offline")

        repository.uploadCoffee(userId, coffee)

        // 1. Memory updated
        assertEquals(1, repository.allCoffee.value.size)
        assertEquals("new_coffee", repository.allCoffee.value.first().id)

        // 2. DB updated with pending flag
        coVerify { coffeeDao.insertCoffee(match { it.first().id == "new_coffee" && it.first().isPendingSync }) }
    }

    @Test
    fun `sync pushes pending coffee to API`() = runTest {
        val coffee = CoffeeDto(id = "p1", roastDate = "2026-08-25")
        val entity = CoffeeEntity("p1", userId, coffee, isPendingSync = true)
        coEvery { coffeeDao.getPendingSync() } returns listOf(entity)

        repository.sync()

        coVerify { apiService.uploadCoffee(userId, coffee) }
        coVerify { coffeeDao.insertCoffee(match { !it.first().isPendingSync }) }
    }
}
