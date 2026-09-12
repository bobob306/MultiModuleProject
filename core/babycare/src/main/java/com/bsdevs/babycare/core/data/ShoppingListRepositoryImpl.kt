package com.bsdevs.babycare.core.data

import android.util.Log
import com.bsdevs.babycare.core.domain.ShoppingListRepository
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.SyncManager
import com.bsdevs.data.Syncable
import com.bsdevs.data.local.dao.ShoppingDao
import com.bsdevs.data.local.entities.ShoppingItemEntity
import com.bsdevs.data.repository.Clearable
import com.bsdevs.data.repository.UserRepository
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.common.FirebaseLogger
import com.bsdevs.network.dto.ShoppingListDoc
import com.bsdevs.network.dto.ShoppingListDto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingListRepositoryImpl @Inject constructor(
    private val firestoreHolder: FirestoreHolder,
    private val userRepository: UserRepository,
    private val dispatchers: DispatcherProvider,
    private val shoppingDao: ShoppingDao,
    syncManager: SyncManager
) : ShoppingListRepository, Clearable, Syncable {

    private val firestore get() = firestoreHolder.firestore
    
    private val repositoryScope = CoroutineScope(dispatchers.io + SupervisorJob())
    private var networkListenerJob: Job? = null
    private var localListenerJob: Job? = null

    private val _shoppingList = MutableStateFlow<List<ShoppingListDto>>(emptyList())
    override val shoppingList: StateFlow<List<ShoppingListDto>> = _shoppingList.asStateFlow()

    init {
        userRepository.registerClearable(this)
        syncManager.registerSyncable(this)
    }

    override suspend fun sync() {
        val babyId = userRepository.userProfile.value?.babyId ?: return
        val pending = shoppingDao.getPendingSync()
        for (entity in pending) {
            try {
                if (entity.isDeleted) {
                    deleteShoppingItem(babyId, entity.id)
                } else {
                    addShoppingItem(babyId, entity.item)
                }
            } catch (e: Exception) {
                Log.e("SHOPPING_REPO", "Sync failed for item ${entity.id}", e)
            }
        }
    }

    override suspend fun startListening(babyId: String) {
        stopListening()
        
        // Listen to local DB
        localListenerJob = repositoryScope.launch {
            shoppingDao.getShoppingItems(babyId).collect { entities ->
                _shoppingList.value = entities.map { it.item }
            }
        }

        // Sync from network
        networkListenerJob = repositoryScope.launch {
            FirebaseLogger.logCall("Listen Shopping List: $babyId")
            firestore.collection("shoppingLists")
                .document(babyId)
                .snapshots()
                .collect { snapshot ->
                    val doc = snapshot.toObject<ShoppingListDoc>()
                    val items = doc?.items?.values?.toList() ?: emptyList()
                    val entities = items.map { ShoppingItemEntity(it.id ?: UUID.randomUUID().toString(), babyId, it) }
                    shoppingDao.insertItems(entities)
                }
        }
    }

    override suspend fun stopListening() {
        networkListenerJob?.cancel()
        localListenerJob?.cancel()
        networkListenerJob = null
        localListenerJob = null
    }

    override suspend fun addShoppingItem(babyId: String, item: ShoppingListDto) {
        withContext(dispatchers.io) {
            val itemId = item.id ?: UUID.randomUUID().toString()
            val finalItem = item.copy(id = itemId)
            
            // Save to local DB first
            shoppingDao.insertItems(listOf(ShoppingItemEntity(itemId, babyId, finalItem, isPendingSync = true)))

            // Try to save to Firestore
            try {
                FirebaseLogger.logCall("Add Shopping Item: $itemId for Baby: $babyId")
                firestore.collection("shoppingLists")
                    .document(babyId)
                    .set(mapOf("items" to mapOf(itemId to finalItem)), SetOptions.merge())
                    .await()
                
                // Clear pending sync flag
                shoppingDao.insertItems(listOf(ShoppingItemEntity(itemId, babyId, finalItem, isPendingSync = false)))
            } catch (e: Exception) {
                Log.e("SHOPPING_REPO", "Failed to sync added item", e)
            }
        }
    }

    override suspend fun updateShoppingItem(babyId: String, item: ShoppingListDto) {
        withContext(dispatchers.io) {
            val itemId = item.id ?: return@withContext
            
            // Save to local DB first
            shoppingDao.insertItems(listOf(ShoppingItemEntity(itemId, babyId, item, isPendingSync = true)))

            try {
                FirebaseLogger.logCall("Update Shopping Item: $itemId for Baby: $babyId")
                firestore.collection("shoppingLists")
                    .document(babyId)
                    .update("items.$itemId", item)
                    .await()
                
                shoppingDao.insertItems(listOf(ShoppingItemEntity(itemId, babyId, item, isPendingSync = false)))
            } catch (e: Exception) {
                Log.e("SHOPPING_REPO", "Failed to sync updated item", e)
            }
        }
    }

    override suspend fun deleteShoppingItem(babyId: String, itemId: String) {
        withContext(dispatchers.io) {
            // Mark as deleted in local DB
            shoppingDao.markDeleted(itemId)

            try {
                FirebaseLogger.logCall("Delete Shopping Item: $itemId for Baby: $babyId")
                firestore.collection("shoppingLists")
                    .document(babyId)
                    .update("items.$itemId", FieldValue.delete())
                    .await()
            } catch (e: Exception) {
                Log.e("SHOPPING_REPO", "Failed to sync deleted item", e)
            }
        }
    }

    override fun clearCache() {
        networkListenerJob?.cancel()
        localListenerJob?.cancel()
        networkListenerJob = null
        localListenerJob = null
        _shoppingList.value = emptyList()
    }
}
