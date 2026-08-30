package com.bsdevs.coffeescreen.data

import com.bsdevs.coffeescreen.network.CoffeeApiService
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.detailscreen.ShotDto
import com.bsdevs.coffeescreen.screens.inputscreen.CoffeeInputScreenDto
import com.bsdevs.common.DispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface CoffeeRepository {
    val allCoffee: StateFlow<List<CoffeeDto>>
    suspend fun loadInitialData(userId: String)
    suspend fun uploadCoffee(userId: String, coffee: CoffeeDto)
    suspend fun getCoffeeById(userId: String, coffeeId: String): CoffeeDto?
    suspend fun getShotsForCoffee(coffeeLabel: String): List<ShotDto>
    suspend fun uploadShot(coffeeLabel: String, shot: ShotDto)
    suspend fun getCoffeeInputScreenData(): CoffeeInputScreenDto?
}

@Singleton
class CoffeeRepositoryImpl @Inject constructor(
    private val apiService: CoffeeApiService,
    private val dispatchers: DispatcherProvider
) : CoffeeRepository {

    private val _allCoffee = MutableStateFlow<List<CoffeeDto>>(emptyList())
    override val allCoffee: StateFlow<List<CoffeeDto>> = _allCoffee.asStateFlow()

    override suspend fun loadInitialData(userId: String) = withContext(dispatchers.io) {
        val coffee = apiService.getAllCoffee(userId)
        _allCoffee.value = coffee.sortedByDescending { it.roastDate }
    }

    override suspend fun uploadCoffee(userId: String, coffee: CoffeeDto) = withContext(dispatchers.io) {
        apiService.uploadCoffee(userId, coffee)
        // Refresh cache
        loadInitialData(userId)
    }

    override suspend fun getCoffeeById(userId: String, coffeeId: String): CoffeeDto? = withContext(dispatchers.io) {
        val cached = _allCoffee.value.firstOrNull { it.id == coffeeId }
        if (cached != null) return@withContext cached
        
        apiService.getCoffeeById(userId, coffeeId)
    }

    override suspend fun getShotsForCoffee(coffeeLabel: String): List<ShotDto> = withContext(dispatchers.io) {
        apiService.getShotsForCoffee(coffeeLabel)
    }

    override suspend fun uploadShot(coffeeLabel: String, shot: ShotDto) = withContext(dispatchers.io) {
        apiService.uploadShot(coffeeLabel, shot)
    }

    override suspend fun getCoffeeInputScreenData(): CoffeeInputScreenDto? = withContext(dispatchers.io) {
        apiService.getCoffeeInputScreenData()
    }
}
