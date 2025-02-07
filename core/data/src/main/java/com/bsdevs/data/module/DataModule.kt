package com.bsdevs.data.module

import com.bsdevs.data.ScreenDataMapper
import com.bsdevs.data.ScreenDataMapperImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    fun provideScreenDataMapper(): ScreenDataMapper {
        return ScreenDataMapperImpl()
    }
}