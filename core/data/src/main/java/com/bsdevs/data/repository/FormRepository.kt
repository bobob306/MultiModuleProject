package com.bsdevs.data.repository

import android.util.Log
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.SyncManager
import com.bsdevs.data.Syncable
import com.bsdevs.data.local.dao.FormDao
import com.bsdevs.data.local.entities.FormSchemaEntity
import com.bsdevs.data.local.entities.FormSubmissionEntity
import com.bsdevs.data.local.entities.DynamicOptionsEntity
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.common.FirebaseLogger
import com.bsdevs.network.FormDtoMapper
import com.bsdevs.network.dto.FormSchemaDto
import com.bsdevs.network.dto.FormSubmissionDto
import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import javax.inject.Inject

interface FormRepository {
    suspend fun getFormSchema(formId: String, forceRefresh: Boolean = false): Flow<Result<FormSchemaDto>>
    suspend fun submitForm(userId: String, formId: String, values: Map<String, Any>): Result<Unit>
    suspend fun getPreviousSubmission(userId: String, formId: String): FormSubmissionDto?
    suspend fun seedFormIfAbsent(formId: String, data: Map<String, Any>)
    suspend fun updateForm(formId: String, data: Map<String, Any>)
    suspend fun deleteForm(formId: String)
    suspend fun getDynamicOptions(type: String): Flow<List<String>>
}

class FormRepositoryImpl @Inject constructor(
    private val firestoreHolder: FirestoreHolder,
    private val userRepository: UserRepository,
    private val mapper: FormDtoMapper,
    private val dispatchers: DispatcherProvider,
    private val formDao: FormDao,
    syncManager: SyncManager,
) : FormRepository, Syncable, Clearable {

    private val firestore get() = firestoreHolder.firestore
    private val schemas get() = firestore.collection("formSchemas")
    private val json = Json { ignoreUnknownKeys = true }

    init {
        userRepository.registerClearable(this)
        syncManager.registerSyncable(this)
    }

    override fun clearCache() {
        // Clear memory cache if needed. Room cache is managed by UserRepository.clearCache()
    }

    override suspend fun sync() {
        val pending = formDao.getPendingSubmissions()
        for (entity in pending) {
            try {
                val values = entity.submission.values.mapValues { (_, value) ->
                    jsonToAny(value)
                }
                
                submitFormToFirestore(entity.userId, entity.formId, values)
                formDao.insertSubmission(entity.copy(isPendingSync = false))
            } catch (_: Exception) {
                Log.e("FORM_REPO", "Sync failed for submission ${entity.id}")
            }
        }
    }

    override suspend fun getFormSchema(formId: String, forceRefresh: Boolean): Flow<Result<FormSchemaDto>> = flow {
        val cached = formDao.getSchema(formId)
        cached?.let {
            emit(Result.Success(it.schema))
        } ?: emit(Result.Loading)

        val shouldRefresh = forceRefresh || cached == null || CacheConstants.isStale(cached.lastUpdated)

        if (shouldRefresh) {
            try {
                FirebaseLogger.logCall("Read Form Schema: $formId (Force: $forceRefresh, Stale: ${cached?.let { CacheConstants.isStale(it.lastUpdated) } ?: "null"})")
                val snapshot = schemas.document(formId).get().await()
                val document = snapshot.data
                if (document != null) {
                    val dto = mapper.mapToDto(document as Map<*, *>)
                    formDao.insertSchema(FormSchemaEntity(formId, dto))
                    emit(Result.Success(dto))
                } else {
                    if (cached == null) {
                        emit(Result.Error(Exception("Form schema not found")))
                    }
                }
            } catch (e: Exception) {
                Log.e("FORM_REPO", "Error fetching schema for $formId", e)
                if (cached == null) emit(Result.Error(e))
            }
        }
    }

    override suspend fun submitForm(userId: String, formId: String, values: Map<String, Any>): Result<Unit> = withContext(dispatchers.io) {
        val submissionDto = FormSubmissionDto(
            submittedAt = System.currentTimeMillis(),
            values = values.mapValues { anyToJson(it.value) }
        )
        
        val entityId = "${userId}_$formId"
        formDao.insertSubmission(FormSubmissionEntity(entityId, userId, formId, submissionDto, isPendingSync = true))

        try {
            submitFormToFirestore(userId, formId, values)
            formDao.insertSubmission(FormSubmissionEntity(entityId, userId, formId, submissionDto, isPendingSync = false))
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("FORM_REPO", "Failed to sync submission", e)
            Result.Success(Unit)
        }
    }

    private suspend fun submitFormToFirestore(userId: String, formId: String, values: Map<String, Any>) {
        val submission = mapOf(
            "submittedAt" to Timestamp.now(),
            "values" to values
        )
        FirebaseLogger.logCall("Submit Form: $formId for User: $userId")
        firestore.collection("users").document(userId).collection("formSubmissions").document(formId).set(submission).await()
    }

    override suspend fun getPreviousSubmission(userId: String, formId: String): FormSubmissionDto? = withContext(dispatchers.io) {
        formDao.getSubmission(userId, formId)?.let { return@withContext it.submission }

        try {
            FirebaseLogger.logCall("Read Previous Submission: $formId for User: $userId")
            val document = firestore.collection("users").document(userId).collection("formSubmissions").document(formId).get().await()
            val dto = document.toObject<FormSubmissionDto>()
            dto?.let {
                val entityId = "${userId}_$formId"
                formDao.insertSubmission(FormSubmissionEntity(entityId, userId, formId, it))
            }
            dto
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun seedFormIfAbsent(formId: String, data: Map<String, Any>) {
        try {
            // ONLY seed into Room for immediate offline availability if missing
            val existingCached = formDao.getSchema(formId)
            if (existingCached == null) {
                Log.d("FORM_REPO", "First time use: Seeding $formId into local Room cache")
                val dto = mapper.mapToDto(data)
                formDao.insertSchema(FormSchemaEntity(formId, dto))
            }
        } catch (e: Exception) {
            Log.e("FORM_REPO", "Failed to seed local form $formId", e)
        }
    }

    override suspend fun updateForm(formId: String, data: Map<String, Any>) {
        try {
            FirebaseLogger.logCall("Update Form Schema: $formId")
            schemas.document(formId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("FORM_REPO", "Failed to update form", e)
        }
    }

    override suspend fun deleteForm(formId: String) {
        try {
            FirebaseLogger.logCall("Delete Form Schema: $formId")
            schemas.document(formId).delete().await()
        } catch (e: Exception) {
            Log.e("FORM_REPO", "Failed to delete form", e)
        }
    }

    override suspend fun getDynamicOptions(type: String): Flow<List<String>> = flow {
        val cached = formDao.getDynamicOptions(type)
        if (cached != null && !CacheConstants.isStale(cached.lastUpdated)) {
            emit(cached.options)
            return@flow
        }

        try {
            FirebaseLogger.logCall("Read Dynamic Options: $type (Stale: ${cached?.let { CacheConstants.isStale(it.lastUpdated) } ?: "null"})")
            val document = firestore.collection("dynamicOptions").document(type).get().await()
            val options = (document.data?.get("options") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            
            formDao.insertDynamicOptions(DynamicOptionsEntity(type, options))
            emit(options)
        } catch (e: Exception) {
            cached?.let { emit(it.options) } ?: emit(emptyList())
        }
    }

    private fun anyToJson(any: Any): JsonElement {
        return when (any) {
            is String -> json.encodeToJsonElement(any)
            is Boolean -> json.encodeToJsonElement(any)
            is Int -> json.encodeToJsonElement(any)
            is Long -> json.encodeToJsonElement(any)
            is Double -> json.encodeToJsonElement(any)
            is Float -> json.encodeToJsonElement(any)
            else -> json.encodeToJsonElement(any.toString())
        }
    }

    private fun jsonToAny(element: JsonElement): Any {
        if (element is JsonPrimitive) {
            if (element.isString) return element.content
            return element.booleanOrNull ?: element.intOrNull ?: element.longOrNull ?: element.doubleOrNull ?: element.content
        }
        return element.toString()
    }
}
