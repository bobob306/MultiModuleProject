package com.bsdevs.network.repository

import android.util.Log
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.dto.BabyDto
import com.bsdevs.network.dto.UserDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    val userProfile: StateFlow<UserDto?>
    suspend fun saveUser(user: UserDto)
    suspend fun saveBaby(baby: BabyDto)
    suspend fun babyExists(babyId: String): Boolean
    suspend fun getUser(userId: String): UserDto?
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : UserRepository {

    private val _userProfile = MutableStateFlow<UserDto?>(null)
    override val userProfile: StateFlow<UserDto?> = _userProfile.asStateFlow()

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

    override suspend fun getUser(userId: String): UserDto? = withContext(dispatchers.io) {
        if (_userProfile.value?.id == userId) return@withContext _userProfile.value

        try {
            Log.d("FIREBASE_CALL", "Read User: $userId")
            val user = firestore.collection("users").document(userId).get().await()
                .toObject(UserDto::class.java)
            _userProfile.value = user
            user
        } catch (e: Exception) {
            null
        }
    }
}
