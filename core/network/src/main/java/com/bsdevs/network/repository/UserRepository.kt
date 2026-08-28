package com.bsdevs.network.repository

import android.util.Log
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.dto.BabyDto
import com.bsdevs.network.dto.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface Clearable {
    fun clearCache()
}

interface UserRepository {
    val userProfile: StateFlow<UserDto?>
    suspend fun saveUser(user: UserDto)
    suspend fun saveBaby(baby: BabyDto)
    suspend fun babyExists(babyId: String): Boolean
    suspend fun getUser(userId: String, forceRefresh: Boolean = false): UserDto?
    suspend fun getBaby(babyId: String, forceRefresh: Boolean = false): BabyDto?
    suspend fun deleteUserData(userId: String)
    suspend fun clearCache()
    fun registerClearable(clearable: Clearable)
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestoreHolder: FirestoreHolder,
    private val dispatchers: DispatcherProvider
) : UserRepository {

    private val firestore get() = firestoreHolder.firestore

    private val _userProfile = MutableStateFlow<UserDto?>(null)
    override val userProfile: StateFlow<UserDto?> = _userProfile.asStateFlow()

    private val clearables = mutableListOf<Clearable>()

    override suspend fun saveUser(user: UserDto): Unit = withContext(dispatchers.io) {
        user.id?.let { id ->
            Log.d("FIREBASE_CALL", "Write User: $id")
            firestore.collection("users").document(id).set(user).await()
            _userProfile.value = user
        }
    }

    override suspend fun saveBaby(baby: BabyDto): Unit = withContext(dispatchers.io) {
        baby.id?.let { id ->
            Log.d("FIREBASE_CALL", "Write Baby: $id")
            firestore.collection("babies").document(id).set(baby).await()
        }
    }

    override suspend fun babyExists(babyId: String): Boolean = withContext(dispatchers.io) {
        try {
            Log.d("FIREBASE_CALL", "Read Baby Exists Check: $babyId")
            firestore.collection("babies").document(babyId).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getUser(userId: String, forceRefresh: Boolean): UserDto? = withContext(dispatchers.io) {
        if (!forceRefresh && _userProfile.value?.id == userId) return@withContext _userProfile.value

        try {
            Log.d("FIREBASE_CALL", "Read User: $userId (Force: $forceRefresh)")
            val source = if (forceRefresh) com.google.firebase.firestore.Source.SERVER else com.google.firebase.firestore.Source.DEFAULT
            val user = firestore.collection("users").document(userId).get(source).await()
                .toObject(UserDto::class.java)
            _userProfile.value = user
            user
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getBaby(babyId: String, forceRefresh: Boolean): BabyDto? = withContext(dispatchers.io) {
        try {
            Log.d("FIREBASE_CALL", "Read Baby: $babyId (Force: $forceRefresh)")
            val source = if (forceRefresh) com.google.firebase.firestore.Source.SERVER else com.google.firebase.firestore.Source.DEFAULT
            firestore.collection("babies").document(babyId).get(source).await()
                .toObject(BabyDto::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteUserData(userId: String): Unit = withContext(dispatchers.io) {
        val user = getUser(userId) ?: return@withContext
        val babyIds = (user.babyIds ?: emptyList()) + listOfNotNull(user.babyId)

        for (babyId in babyIds) {
            // Check if others are linked to this babyId
            val otherParents = firestore.collection("users")
                .whereEqualTo("babyId", babyId)
                .get().await()
                .documents
                .filter { it.id != userId }

            val otherParentsFromList = firestore.collection("users")
                .whereArrayContains("babyIds", babyId)
                .get().await()
                .documents
                .filter { it.id != userId }

            if (otherParents.isEmpty() && otherParentsFromList.isEmpty()) {
                // Delete baby data
                Log.d("FIREBASE_CALL", "Delete Baby: $babyId")
                firestore.collection("babies").document(babyId).delete().await()

                // Delete baby logs
                Log.d("FIREBASE_CALL", "Delete Baby Logs for: $babyId")
                val months = firestore.collection("babyLogs").document(babyId).collection("months").get().await()
                months.documents.forEach { it.reference.delete().await() }
                firestore.collection("babyLogs").document(babyId).delete().await()
            }
        }

        // Delete coffee logs
        Log.d("FIREBASE_CALL", "Delete Coffee Logs for: $userId")
        val coffeeUploads = firestore.collection("coffeeUploads")
            .whereEqualTo("userId", userId)
            .get().await()

        for (doc in coffeeUploads.documents) {
            val shots = doc.reference.collection("shots").get().await()
            shots.documents.forEach { it.reference.delete().await() }
            doc.reference.delete().await()
        }

        // Delete user document
        Log.d("FIREBASE_CALL", "Delete User: $userId")
        firestore.collection("users").document(userId).delete().await()
        _userProfile.value = null
    }

    override suspend fun clearCache(): Unit = withContext(dispatchers.io) {
        try {
            firestoreHolder.reset()
            clearables.forEach { it.clearCache() }
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to clear cache", e)
        } finally {
            _userProfile.value = null
        }
    }

    override fun registerClearable(clearable: Clearable) {
        clearables.add(clearable)
    }
}
