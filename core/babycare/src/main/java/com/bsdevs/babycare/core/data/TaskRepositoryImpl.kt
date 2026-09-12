package com.bsdevs.babycare.core.data

import android.util.Log
import com.bsdevs.babycare.core.domain.TaskRepository
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.SyncManager
import com.bsdevs.data.Syncable
import com.bsdevs.data.local.dao.TaskDao
import com.bsdevs.data.local.entities.TaskEntity
import com.bsdevs.data.repository.Clearable
import com.bsdevs.data.repository.UserRepository
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.common.FirebaseLogger
import com.bsdevs.network.dto.TaskDoc
import com.bsdevs.network.dto.TaskDto
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
class TaskRepositoryImpl @Inject constructor(
    private val firestoreHolder: FirestoreHolder,
    private val userRepository: UserRepository,
    private val dispatchers: DispatcherProvider,
    private val taskDao: TaskDao,
    syncManager: SyncManager
) : TaskRepository, Clearable, Syncable {

    private val firestore get() = firestoreHolder.firestore
    
    private val repositoryScope = CoroutineScope(dispatchers.io + SupervisorJob())
    private var networkListenerJob: Job? = null
    private var localListenerJob: Job? = null

    private val _taskList = MutableStateFlow<List<TaskDto>>(emptyList())
    override val taskList: StateFlow<List<TaskDto>> = _taskList.asStateFlow()

    init {
        userRepository.registerClearable(this)
        syncManager.registerSyncable(this)
    }

    override suspend fun sync() {
        val babyId = userRepository.userProfile.value?.babyId ?: return
        val pending = taskDao.getPendingSync()
        for (entity in pending) {
            try {
                if (entity.isDeleted) {
                    deleteTaskItem(babyId, entity.id)
                } else {
                    addTaskItem(babyId, entity.item)
                }
            } catch (e: Exception) {
                Log.e("TASK_REPO", "Sync failed for item ${entity.id}", e)
            }
        }
    }

    override suspend fun startListening(babyId: String) {
        stopListening()
        
        // Listen to local DB
        localListenerJob = repositoryScope.launch {
            taskDao.getTasks(babyId).collect { entities ->
                _taskList.value = entities.map { it.item }
            }
        }

        // Sync from network
        networkListenerJob = repositoryScope.launch {
            FirebaseLogger.logCall("Listen Tasks List: $babyId")
            firestore.collection("tasks")
                .document(babyId)
                .snapshots()
                .collect { snapshot ->
                    val doc = snapshot.toObject<TaskDoc>()
                    val items = doc?.items?.values?.toList() ?: emptyList()
                    val entities = items.map { TaskEntity(it.id ?: UUID.randomUUID().toString(), babyId, it) }
                    taskDao.insertTasks(entities)
                }
        }
    }

    override suspend fun stopListening() {
        networkListenerJob?.cancel()
        localListenerJob?.cancel()
        networkListenerJob = null
        localListenerJob = null
    }

    override suspend fun addTaskItem(babyId: String, item: TaskDto) {
        withContext(dispatchers.io) {
            val itemId = item.id ?: UUID.randomUUID().toString()
            val finalItem = item.copy(id = itemId)
            
            // Save to local DB first
            taskDao.insertTasks(listOf(TaskEntity(itemId, babyId, finalItem, isPendingSync = true)))

            // Try to save to Firestore
            try {
                FirebaseLogger.logCall("Add Task Item: $itemId for Baby: $babyId")
                firestore.collection("tasks")
                    .document(babyId)
                    .set(mapOf("items" to mapOf(itemId to finalItem)), SetOptions.merge())
                    .await()
                
                // Clear pending sync flag
                taskDao.insertTasks(listOf(TaskEntity(itemId, babyId, finalItem, isPendingSync = false)))
            } catch (e: Exception) {
                Log.e("TASK_REPO", "Failed to sync added item", e)
            }
        }
    }

    override suspend fun updateTaskItem(babyId: String, item: TaskDto) {
        withContext(dispatchers.io) {
            val itemId = item.id ?: return@withContext
            
            // Save to local DB first
            taskDao.insertTasks(listOf(TaskEntity(itemId, babyId, item, isPendingSync = true)))

            try {
                FirebaseLogger.logCall("Update Task Item: $itemId for Baby: $babyId")
                firestore.collection("tasks")
                    .document(babyId)
                    .update("items.$itemId", item)
                    .await()
                
                taskDao.insertTasks(listOf(TaskEntity(itemId, babyId, item, isPendingSync = false)))
            } catch (e: Exception) {
                Log.e("TASK_REPO", "Failed to sync updated item", e)
            }
        }
    }

    override suspend fun deleteTaskItem(babyId: String, itemId: String) {
        withContext(dispatchers.io) {
            // Mark as deleted in local DB
            taskDao.markDeleted(itemId)

            try {
                FirebaseLogger.logCall("Delete Task Item: $itemId for Baby: $babyId")
                firestore.collection("tasks")
                    .document(babyId)
                    .update("items.$itemId", FieldValue.delete())
                    .await()
            } catch (e: Exception) {
                Log.e("TASK_REPO", "Failed to sync deleted item", e)
            }
        }
    }

    override fun clearCache() {
        networkListenerJob?.cancel()
        localListenerJob?.cancel()
        networkListenerJob = null
        localListenerJob = null
        _taskList.value = emptyList()
    }
}
