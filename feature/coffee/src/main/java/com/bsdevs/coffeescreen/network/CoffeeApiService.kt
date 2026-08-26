package com.bsdevs.coffeescreen.network

import com.bsdevs.coffeescreen.screens.inputscreen.CoffeeInputScreenDto

interface CoffeeApiService {
    suspend fun getCoffeeInputScreenData(): CoffeeInputScreenDto?
    suspend fun uploadCoffee(userId: String, coffee: CoffeeDto)
}
