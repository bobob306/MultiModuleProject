package com.bsdevs.network

import com.bsdevs.network.repository.ScreenRepository
import com.bsdevs.network.repository.ScreenRepositoryImpl
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseApiModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    @Singleton
    fun provideFirebaseFirestoreCollection(): CollectionReference =
        FirebaseFirestore.getInstance().collection("screens")

    @Provides
    fun provideMapper(): ScreenDtoMapper {
        return ScreenDtoMapperImpl()
    }

    @Provides
    fun provideScreenRepository(scr: CollectionReference, mapper: ScreenDtoMapper): ScreenRepository {
        return ScreenRepositoryImpl(scr, mapper)
    }
}