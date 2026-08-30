package com.bsdevs.coffeescreen.screens.inputscreen

import com.bsdevs.coffeescreen.data.CoffeeRepository
import com.bsdevs.coffeescreen.network.CoffeeApiService
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.detailscreen.ShotDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeCoffeeApiService : CoffeeApiService, CoffeeRepository {
    var screenData: CoffeeInputScreenDto? = null
    val uploadedCoffees = mutableListOf<CoffeeDto>()
    private val _allCoffee = MutableStateFlow<List<CoffeeDto>>(emptyList())
    override val allCoffee: StateFlow<List<CoffeeDto>> = _allCoffee.asStateFlow()

    override suspend fun loadInitialData(userId: String) {
        _allCoffee.value = uploadedCoffees.filter { it.userId == userId }
    }

    override suspend fun getCoffeeInputScreenData(): CoffeeInputScreenDto? {
        return screenData
    }

    override suspend fun uploadCoffee(userId: String, coffee: CoffeeDto) {
        uploadedCoffees.add(coffee.copy(userId = userId))
        _allCoffee.value = uploadedCoffees.filter { it.userId == userId }
    }

    override suspend fun getCoffeeById(userId: String, coffeeId: String): CoffeeDto? {
        return uploadedCoffees.firstOrNull { it.userId == userId && it.id == coffeeId }
    }

    override suspend fun getAllCoffee(userId: String): List<CoffeeDto> {
        return uploadedCoffees.filter { it.userId == userId }
    }

    private val shotsDatabase = mutableMapOf<String, MutableList<ShotDto>>()

    override suspend fun getShotsForCoffee(coffeeLabel: String): List<ShotDto> {
        return shotsDatabase[coffeeLabel] ?: emptyList()
    }

    override suspend fun uploadShot(coffeeLabel: String, shot: ShotDto) {
        shotsDatabase.getOrPut(coffeeLabel) { mutableListOf() }.add(shot)
    }
}
