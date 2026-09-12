package com.bsdevs.data.module

import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.local.dao.FormDao
import com.bsdevs.data.local.dao.ScreenDao
import com.bsdevs.data.SyncManager
import com.bsdevs.data.local.dao.UserBabyDao
import com.bsdevs.data.repository.FormRepository
import com.bsdevs.data.repository.FormRepositoryImpl
import com.bsdevs.data.repository.MetadataRepository
import com.bsdevs.data.repository.MetadataRepositoryImpl
import com.bsdevs.data.repository.ScreenRepository
import com.bsdevs.data.repository.ScreenRepositoryImpl
import com.bsdevs.data.repository.UserRepository
import com.bsdevs.data.repository.UserRepositoryImpl
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.FormDtoMapper
import com.bsdevs.network.MetadataDtoMapper
import com.bsdevs.network.ScreenDtoMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMetadataRepository(
        firestoreHolder: FirestoreHolder,
        mapper: MetadataDtoMapper,
        screenDao: ScreenDao,
        formDao: FormDao,
        dispatchers: DispatcherProvider
    ): MetadataRepository {
        return MetadataRepositoryImpl(firestoreHolder, mapper, screenDao, formDao, dispatchers)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        firestoreHolder: FirestoreHolder,
        dispatchers: DispatcherProvider,
        userBabyDao: UserBabyDao,
        formDao: FormDao,
        screenDao: ScreenDao
    ): UserRepository {
        return UserRepositoryImpl(firestoreHolder, dispatchers, userBabyDao, formDao, screenDao)
    }

    @Provides
    @Singleton
    fun provideScreenRepository(
        firestoreHolder: FirestoreHolder,
        userRepository: UserRepository,
        mapper: ScreenDtoMapper,
        dispatchers: DispatcherProvider,
        screenDao: ScreenDao
    ): ScreenRepository {
        return ScreenRepositoryImpl(firestoreHolder, userRepository, mapper, dispatchers, screenDao)
    }

    @Provides
    @Singleton
    fun provideFormRepository(
        firestoreHolder: FirestoreHolder,
        userRepository: UserRepository,
        mapper: FormDtoMapper,
        dispatchers: DispatcherProvider,
        formDao: FormDao,
        syncManager: SyncManager
    ): FormRepository {
        return FormRepositoryImpl(firestoreHolder, userRepository, mapper, dispatchers, formDao, syncManager)
    }
}
