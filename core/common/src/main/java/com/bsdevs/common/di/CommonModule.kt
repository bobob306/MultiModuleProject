package com.bsdevs.common.di

import com.bsdevs.common.DispatcherProviderImpl
import com.bsdevs.common.TimeProviderImpl
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonModule {
    @Binds
    @Singleton
    abstract fun bindTimeProvider(timeProviderImpl: TimeProviderImpl): TimeProvider

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(dispatcherProviderImpl: DispatcherProviderImpl): DispatcherProvider
}
