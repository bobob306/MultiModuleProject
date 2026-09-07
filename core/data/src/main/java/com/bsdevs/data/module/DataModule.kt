package com.bsdevs.data.module

import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.FormDataMapper
import com.bsdevs.data.FormDataMapperImpl
import com.bsdevs.data.ScreenDataMapper
import com.bsdevs.data.ScreenDataMapperImpl
import com.bsdevs.data.SyncManager
import com.bsdevs.network.connectivity.ConnectivityObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSyncManager(
        connectivityObserver: ConnectivityObserver,
        dispatchers: DispatcherProvider
    ): SyncManager = SyncManager(connectivityObserver, dispatchers)

    @Provides
    fun provideScreenDataMapper(): ScreenDataMapper = ScreenDataMapperImpl()

    @Provides
    fun provideFormDataMapper(): FormDataMapper = FormDataMapperImpl()
}
