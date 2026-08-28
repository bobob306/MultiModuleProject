package com.bsdevs.coffeescreen.network

import com.bsdevs.coffeescreen.screens.inputscreen.CoffeeInputScreenDto

interface CoffeeApiService {
    suspend fun getCoffeeInputScreenData(): CoffeeInputScreenDto?
    suspend fun uploadCoffee(userId: String, coffee: CoffeeDto)
    suspend fun getCoffeeById(userId: String, coffeeId: String): CoffeeDto?
    suspend fun getAllCoffee(userId: String): List<CoffeeDto>
    suspend fun getShotsForCoffee(coffeeLabel: String): List<com.bsdevs.coffeescreen.screens.detailscreen.ShotDto>
    suspend fun uploadShot(coffeeLabel: String, shot: com.bsdevs.coffeescreen.screens.detailscreen.ShotDto)
}
