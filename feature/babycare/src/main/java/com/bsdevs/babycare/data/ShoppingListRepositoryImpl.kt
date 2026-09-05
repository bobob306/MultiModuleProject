package com.bsdevs.babycare.data

import com.bsdevs.babycare.domain.ShoppingListRepository
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.dto.ShoppingListDto
import com.bsdevs.network.dto.ShoppingListDoc
import com.bsdevs.network.repository.Clearable
import com.bsdevs.network.repository.UserRepository
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
    private val dispatchers: DispatcherProvider
) : ShoppingListRepository, Clearable {

    private val firestore get() = firestoreHolder.firestore
    
    private val repositoryScope = CoroutineScope(dispatchers.io + SupervisorJob())
    private var listenerJob: Job? = null

    private val _shoppingList = MutableStateFlow<List<ShoppingListDto>>(emptyList())
    override val shoppingList: StateFlow<List<ShoppingListDto>> = _shoppingList.asStateFlow()

    init {
        userRepository.registerClearable(this)
    }

    override suspend fun startListening(babyId: String) {
        stopListening()
        listenerJob = repositoryScope.launch {
            firestore.collection("shoppingLists")
                .document(babyId)
                .snapshots()
                .collect { snapshot ->
                    val doc = snapshot.toObject<ShoppingListDoc>()
                    _shoppingList.value = doc?.items?.values?.toList() ?: emptyList()
                }
        }
    }

    override suspend fun stopListening() {
        listenerJob?.cancel()
        listenerJob = null
    }

    override suspend fun addShoppingItem(babyId: String, item: ShoppingListDto) {
        withContext(dispatchers.io) {
            val itemId = UUID.randomUUID().toString()
            val finalItem = item.copy(id = itemId)
            
            // Use set with merge to ensure the document and the 'items' map exist
            firestore.collection("shoppingLists")
                .document(babyId)
                .set(mapOf("items" to mapOf(itemId to finalItem)), SetOptions.merge())
                .await()
        }
    }

    override suspend fun updateShoppingItem(babyId: String, item: ShoppingListDto) {
        withContext(dispatchers.io) {
            val itemId = item.id ?: return@withContext
            firestore.collection("shoppingLists")
                .document(babyId)
                .update("items.$itemId", item)
                .await()
        }
    }

    override suspend fun deleteShoppingItem(babyId: String, itemId: String) {
        withContext(dispatchers.io) {
            firestore.collection("shoppingLists")
                .document(babyId)
                .update("items.$itemId", FieldValue.delete())
                .await()
        }
    }

    override fun clearCache() {
        listenerJob?.cancel()
        listenerJob = null
        _shoppingList.value = emptyList()
    }
}
