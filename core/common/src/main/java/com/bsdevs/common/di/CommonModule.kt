package com.bsdevs.common.di

import com.bsdevs.common.DefaultDispatcherProvider
import com.bsdevs.common.DefaultTimeProvider
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
    abstract fun bindTimeProvider(defaultTimeProvider: DefaultTimeProvider): TimeProvider

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(defaultDispatcherProvider: DefaultDispatcherProvider): DispatcherProvider
}
