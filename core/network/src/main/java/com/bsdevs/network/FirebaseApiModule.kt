package com.bsdevs.network

import com.bsdevs.network.repository.FormRepository
import com.bsdevs.network.repository.FormRepositoryImpl
import com.bsdevs.network.repository.ScreenRepository
import com.bsdevs.network.repository.ScreenRepositoryImpl
import com.bsdevs.network.repository.UserRepository
import com.bsdevs.network.repository.UserRepositoryImpl
import com.bsdevs.common.DispatcherProvider
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseApiModule {

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
    @Singleton
    fun provideScreenRepository(
        firestoreHolder: FirestoreHolder,
        userRepository: UserRepository,
        mapper: ScreenDtoMapper,
        dispatchers: DispatcherProvider
    ): ScreenRepository {
        return ScreenRepositoryImpl(firestoreHolder, userRepository, mapper, dispatchers)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        firestoreHolder: FirestoreHolder,
        dispatchers: DispatcherProvider
    ): UserRepository {
        return UserRepositoryImpl(firestoreHolder, dispatchers)
    }

    @Provides
    fun provideFormDtoMapper(): FormDtoMapper = FormDtoMapperImpl()

    @Provides
    @Singleton
    fun provideFormRepository(
        firestoreHolder: FirestoreHolder,
        mapper: FormDtoMapper,
        dispatchers: DispatcherProvider,
    ): FormRepository = FormRepositoryImpl(firestoreHolder, mapper, dispatchers)
}
