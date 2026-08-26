package com.bsdevs.coffeescreen.di

import com.bsdevs.coffeescreen.network.CoffeeApiService
import com.bsdevs.coffeescreen.network.FirestoreCoffeeApiService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoffeeModule {
    @Binds
    @Singleton
    abstract fun bindCoffeeApiService(
        firestoreCoffeeApiService: FirestoreCoffeeApiService
    ): CoffeeApiService
}
