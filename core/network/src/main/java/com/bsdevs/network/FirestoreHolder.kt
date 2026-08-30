package com.bsdevs.network

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreHolder @Inject constructor(
    private val firestoreInstance: FirebaseFirestore
) {
    private var _firestore: FirebaseFirestore = setupFirestore(firestoreInstance)

    val firestore: FirebaseFirestore
        get() = _firestore

    private fun setupFirestore(firestore: FirebaseFirestore): FirebaseFirestore {
        val settings = firestoreSettings {
            setLocalCacheSettings(
                persistentCacheSettings {
                    setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                }
            )
        }
        firestore.firestoreSettings = settings
        return firestore
    }

    suspend fun reset() {
        try {
            _firestore.terminate().await()
            _firestore.clearPersistence().await()
        } finally {
            _firestore = setupFirestore(firestoreInstance)
        }
    }
}
