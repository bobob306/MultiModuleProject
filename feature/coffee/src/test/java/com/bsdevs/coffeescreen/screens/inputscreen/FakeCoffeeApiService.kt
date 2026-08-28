package com.bsdevs.coffeescreen.screens.inputscreen

import com.bsdevs.coffeescreen.network.CoffeeApiService
import com.bsdevs.coffeescreen.network.CoffeeDto

class FakeCoffeeApiService : CoffeeApiService {
    var screenData: CoffeeInputScreenDto? = null
    val uploadedCoffees = mutableListOf<CoffeeDto>()

    override suspend fun getCoffeeInputScreenData(): CoffeeInputScreenDto? {
        return screenData
    }

    override suspend fun uploadCoffee(userId: String, coffee: CoffeeDto) {
        uploadedCoffees.add(coffee)
    }

    override suspend fun getCoffeeById(userId: String, coffeeId: String): CoffeeDto? {
        return uploadedCoffees.firstOrNull { it.userId == userId && it.id == coffeeId }
    }

    override suspend fun getAllCoffee(userId: String): List<CoffeeDto> {
        return uploadedCoffees.filter { it.userId == userId }
    }

    private val shotsDatabase = mutableMapOf<String, MutableList<com.bsdevs.coffeescreen.screens.detailscreen.ShotDto>>()

    override suspend fun getShotsForCoffee(coffeeLabel: String): List<com.bsdevs.coffeescreen.screens.detailscreen.ShotDto> {
        return shotsDatabase[coffeeLabel] ?: emptyList()
    }

    override suspend fun uploadShot(coffeeLabel: String, shot: com.bsdevs.coffeescreen.screens.detailscreen.ShotDto) {
        shotsDatabase.getOrPut(coffeeLabel) { mutableListOf() }.add(shot)
    }
}
