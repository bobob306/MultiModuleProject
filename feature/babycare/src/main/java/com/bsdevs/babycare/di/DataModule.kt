package com.bsdevs.babycare.di

import com.bsdevs.babycare.data.repository.BabyCareRepositoryImpl
import com.bsdevs.babycare.domain.BabyCareRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.bsdevs.babycare.presentation.common.TimeProvider
import com.bsdevs.babycare.presentation.common.DefaultTimeProvider
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindBabyCareRepository(
        babyCareRepositoryImpl: BabyCareRepositoryImpl
    ): BabyCareRepository

    @Binds
    @Singleton
    abstract fun bindBabyCareFirestoreService(
        firestoreBabyCareService: com.bsdevs.babycare.network.FirestoreBabyCareService
    ): com.bsdevs.babycare.network.BabyCareFirestoreService

    @Binds
    @Singleton
    abstract fun bindTimeProvider(
        defaultTimeProvider: DefaultTimeProvider
    ): TimeProvider
}