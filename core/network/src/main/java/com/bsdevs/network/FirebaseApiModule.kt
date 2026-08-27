package com.bsdevs.network

import com.bsdevs.network.repository.ScreenRepository
import com.bsdevs.network.repository.ScreenRepositoryImpl
import com.bsdevs.network.repository.UserRepository
import com.bsdevs.network.repository.UserRepositoryImpl
import com.bsdevs.common.DispatcherProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestoreSettings.*
import com.google.firebase.firestore.firestoreSettings
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
    fun provideFirestore(): FirebaseFirestore {
        val firestore = Firebase.firestore
        val settings = firestoreSettings {
            isPersistenceEnabled = true
            cacheSizeBytes = CACHE_SIZE_UNLIMITED
        }
        firestore.firestoreSettings = settings
        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestoreCollection(): CollectionReference =
        FirebaseFirestore.getInstance().collection("screens")

    @Provides
    fun provideMapper(): ScreenDtoMapper {
        return ScreenDtoMapperImpl()
    }

    @Provides
    @Singleton
    fun provideScreenRepository(
        scr: CollectionReference,
        mapper: ScreenDtoMapper,
        dispatchers: DispatcherProvider
    ): ScreenRepository {
        return ScreenRepositoryImpl(scr, mapper, dispatchers)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        dispatchers: DispatcherProvider
    ): UserRepository {
        return UserRepositoryImpl(firestore, dispatchers)
    }
}