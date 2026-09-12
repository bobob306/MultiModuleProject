package com.bsdevs.network

import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.connectivity.ConnectivityObserver
import com.bsdevs.network.connectivity.ConnectivityObserverImpl
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.Context

@Module
@InstallIn(SingletonComponent::class)
object FirebaseApiModule {

    @Provides
    @Singleton
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver {
        return ConnectivityObserverImpl(context)
    }

    @Provides
    fun provideMapper(): ScreenDtoMapper {
        return ScreenDtoMapperImpl()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }

    @Provides
    fun provideFormDtoMapper(): FormDtoMapper = FormDtoMapperImpl()

    @Provides
    fun provideMetadataDtoMapper(
        screenMapper: ScreenDtoMapper,
        formMapper: FormDtoMapper
    ): MetadataDtoMapper = MetadataDtoMapperImpl(screenMapper, formMapper)
}
