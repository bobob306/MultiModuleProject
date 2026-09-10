package com.bsdevs.data.repository

import android.util.Log
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.local.dao.UserBabyDao
import com.bsdevs.data.local.entities.BabyEntity
import com.bsdevs.data.local.entities.UserEntity
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.dto.BabyDto
import com.bsdevs.network.dto.UserDto
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
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
    fun getBabyFlow(babyId: String): Flow<BabyDto?>
    suspend fun deleteUserData(userId: String)
    suspend fun clearCache()
    fun registerClearable(clearable: Clearable)
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestoreHolder: FirestoreHolder,
    private val dispatchers: DispatcherProvider,
    private val userBabyDao: UserBabyDao
) : UserRepository {

    private val firestore get() = firestoreHolder.firestore

    private val _userProfile = MutableStateFlow<UserDto?>(null)
    override val userProfile: StateFlow<UserDto?> = _userProfile.asStateFlow()

    private val babyCache = ConcurrentHashMap<String, BabyDto>()
    private val clearables = mutableListOf<Clearable>()

    override suspend fun saveUser(user: UserDto): Unit = withContext(dispatchers.io) {
        user.id?.let { id ->
            Log.d("FIREBASE_CALL", "Write User: $id")
            firestore.collection("users").document(id).set(user).await()
            userBabyDao.insertUser(UserEntity(id, user))
            _userProfile.value = user
        }
    }

    override suspend fun saveBaby(baby: BabyDto): Unit = withContext(dispatchers.io) {
        baby.id?.let { id ->
            Log.d("FIREBASE_CALL", "Write Baby: $id")
            firestore.collection("babies").document(id).set(baby).await()
            userBabyDao.insertBaby(BabyEntity(id, baby))
            babyCache[id] = baby
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
        if (!forceRefresh) {
            userBabyDao.getUser(userId)?.let { cached ->
                _userProfile.value = cached.profile
                return@withContext cached.profile
            }
        }

        try {
            Log.d("FIREBASE_CALL", "Read User: $userId (Force: $forceRefresh)")
            // Use DEFAULT to allow Firestore to handle offline fallback gracefully.
            val source = Source.DEFAULT
            val snapshot = firestore.collection("users").document(userId).get(source).await()
            val userDto = snapshot.toObject<UserDto>()
            val updatedUser = userDto?.copy(id = snapshot.id)
            val uid = updatedUser?.id
            if (uid != null) {
                userBabyDao.insertUser(UserEntity(uid, updatedUser))
            }
            _userProfile.value = updatedUser
            updatedUser
        } catch (e: Exception) {
            Log.w("USER_REPO", "Failed to fetch user $userId", e)
            // Fallback to Room cache if Firestore fetch fails
            userBabyDao.getUser(userId)?.profile
        }
    }

    override suspend fun getBaby(babyId: String, forceRefresh: Boolean): BabyDto? = withContext(dispatchers.io) {
        if (!forceRefresh) {
            userBabyDao.getBaby(babyId)?.let { cached ->
                babyCache[babyId] = cached.data
                return@withContext cached.data
            }
        }
        
        try {
            Log.d("FIREBASE_CALL", "Read Baby: $babyId (Force: $forceRefresh)")
            val source = Source.DEFAULT
            val snapshot = firestore.collection("babies").document(babyId).get(source).await()
            val babyDto = snapshot.toObject<BabyDto>()
            val updatedBaby = babyDto?.copy(id = snapshot.id)
            val bid = updatedBaby?.id
            if (bid != null) {
                userBabyDao.insertBaby(BabyEntity(bid, updatedBaby))
                babyCache[bid] = updatedBaby
            }
            updatedBaby
        } catch (e: Exception) {
            Log.w("USER_REPO", "Failed to fetch baby $babyId", e)
            userBabyDao.getBaby(babyId)?.data
        }
    }

    override fun getBabyFlow(babyId: String): Flow<BabyDto?> = callbackFlow {
        Log.d("FIREBASE_CALL", "Listen Baby: $babyId")
        val listener = firestore.collection("babies").document(babyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    launch {
                        val cached = userBabyDao.getBaby(babyId)
                        trySend(cached?.data)
                    }
                    return@addSnapshotListener
                }
                val babyDto = snapshot?.toObject<BabyDto>()
                val updatedBaby = (babyDto as? BabyDto)?.copy(id = snapshot.id ?: babyId)
                updatedBaby?.let { 
                    babyCache[babyId] = it
                    launch { userBabyDao.insertBaby(BabyEntity(babyId, it)) }
                }
                trySend(updatedBaby)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun deleteUserData(userId: String): Unit = withContext(dispatchers.io) {
        val user = getUser(userId) ?: return@withContext
        val babyIds = (user.babyIds ?: emptyList()) + listOfNotNull(user.babyId)

        for (babyId in babyIds) {
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
                Log.d("FIREBASE_CALL", "Delete Baby: $babyId")
                firestore.collection("babies").document(babyId).delete().await()
                userBabyDao.clearBabies()

                Log.d("FIREBASE_CALL", "Delete Baby Logs for: $babyId")
                val months = firestore.collection("babyLogs").document(babyId).collection("months").get().await()
                months.documents.forEach { it.reference.delete().await() }
                firestore.collection("babyLogs").document(babyId).delete().await()

                Log.d("FIREBASE_CALL", "Delete Shopping List for: $babyId")
                firestore.collection("shoppingLists").document(babyId).delete().await()
            }
        }

        Log.d("FIREBASE_CALL", "Delete Coffee Logs for: $userId")
        val coffeeUploads = firestore.collection("coffeeUploads")
            .whereEqualTo("userId", userId)
            .get().await()

        for (doc in coffeeUploads.documents) {
            val shots = doc.reference.collection("shots").get().await()
            shots.documents.forEach { it.reference.delete().await() }
            doc.reference.delete().await()
        }

        Log.d("FIREBASE_CALL", "Delete User: $userId")
        firestore.collection("users").document(userId).delete().await()
        userBabyDao.clearUsers()
        _userProfile.value = null
    }

    override suspend fun clearCache(): Unit = withContext(dispatchers.io) {
        try {
            firestoreHolder.reset()
            clearables.forEach { it.clearCache() }
            userBabyDao.clearUsers()
            userBabyDao.clearBabies()
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to clear cache", e)
        } finally {
            _userProfile.value = null
            babyCache.clear()
        }
    }

    override fun registerClearable(clearable: Clearable) {
        clearables.add(clearable)
    }
}
