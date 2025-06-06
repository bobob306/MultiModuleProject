package com.bsdevs.authentication

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
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
    fun provideAccountService(impl: AccountServiceImpl): AccountService {
        return impl
    }
}

