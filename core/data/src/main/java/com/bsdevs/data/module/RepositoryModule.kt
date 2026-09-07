package com.bsdevs.data.module

import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.local.dao.FormDao
import com.bsdevs.data.local.dao.ScreenDao
import com.bsdevs.data.SyncManager
import com.bsdevs.data.local.dao.UserBabyDao
import com.bsdevs.data.repository.FormRepository
import com.bsdevs.data.repository.FormRepositoryImpl
import com.bsdevs.data.repository.ScreenRepository
import com.bsdevs.data.repository.ScreenRepositoryImpl
import com.bsdevs.data.repository.UserRepository
import com.bsdevs.data.repository.UserRepositoryImpl
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.FormDtoMapper
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
    fun provideUserRepository(
        firestoreHolder: FirestoreHolder,
        dispatchers: DispatcherProvider,
        userBabyDao: UserBabyDao
    ): UserRepository {
        return UserRepositoryImpl(firestoreHolder, dispatchers, userBabyDao)
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
        mapper: FormDtoMapper,
        dispatchers: DispatcherProvider,
        formDao: FormDao,
        syncManager: SyncManager
    ): FormRepository {
        return FormRepositoryImpl(firestoreHolder, mapper, dispatchers, formDao, syncManager)
    }
}
