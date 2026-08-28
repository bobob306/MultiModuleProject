package com.bsdevs.babycare.di

import com.bsdevs.babycare.data.repository.BabyCareRepositoryImpl
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.BabyCareFirestoreService
import com.bsdevs.babycare.network.FirestoreBabyCareService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.bsdevs.babycare.presentation.common.TimeProvider
import com.bsdevs.babycare.presentation.common.DefaultTimeProvider
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.DefaultDispatcherProvider
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
        firestoreBabyCareService: FirestoreBabyCareService
    ): BabyCareFirestoreService

    @Binds
    @Singleton
    abstract fun bindTimeProvider(
        defaultTimeProvider: DefaultTimeProvider
    ): TimeProvider

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(
        defaultDispatcherProvider: DefaultDispatcherProvider
    ): DispatcherProvider
}
