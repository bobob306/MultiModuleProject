package com.bsdevs.coffeescreen.network

import com.bsdevs.network.dto.CoffeeInputScreenDto
import com.bsdevs.network.dto.CoffeeDto
import com.bsdevs.network.dto.ShotDto

interface CoffeeApiService {
    suspend fun getCoffeeInputScreenData(): CoffeeInputScreenDto?
    suspend fun uploadCoffee(userId: String, coffee: CoffeeDto)
    suspend fun getCoffeeById(userId: String, coffeeId: String): CoffeeDto?
    suspend fun getAllCoffee(userId: String): List<CoffeeDto>
    suspend fun getShotsForCoffee(coffeeLabel: String): List<ShotDto>
    suspend fun uploadShot(coffeeLabel: String, shot: ShotDto)
}
