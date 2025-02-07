package com.bsdevs.network

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
    fun provideScreenRepository(scr: CollectionReference): ScreenRepository {
        return ScreenRepositoryImpl(scr)
    }
}