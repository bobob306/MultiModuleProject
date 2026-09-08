package com.bsdevs.babycare.core.di

import com.bsdevs.babycare.core.data.BabyCareRepositoryImpl
import com.bsdevs.babycare.core.data.ShoppingListRepositoryImpl
import com.bsdevs.babycare.core.domain.BabyCareRepository
import com.bsdevs.babycare.core.domain.ShoppingListRepository
import com.bsdevs.babycare.core.network.BabyCareFirestoreService
import com.bsdevs.babycare.core.network.FirestoreBabyCareService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BabyCareDataModule {

    @Binds
    @Singleton
    abstract fun bindBabyCareRepository(
        babyCareRepositoryImpl: BabyCareRepositoryImpl
    ): BabyCareRepository

    @Binds
    @Singleton
    abstract fun bindShoppingListRepository(
        shoppingListRepositoryImpl: ShoppingListRepositoryImpl
    ): ShoppingListRepository

    @Binds
    @Singleton
    abstract fun bindBabyCareFirestoreService(
        firestoreBabyCareService: FirestoreBabyCareService
    ): BabyCareFirestoreService
}
