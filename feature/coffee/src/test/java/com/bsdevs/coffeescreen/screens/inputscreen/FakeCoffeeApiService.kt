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
}
