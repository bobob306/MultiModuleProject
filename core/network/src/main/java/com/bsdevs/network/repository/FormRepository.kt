package com.bsdevs.network.repository

import android.util.Log
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.FormDtoMapper
import com.bsdevs.network.dto.FormSchemaDto
import com.bsdevs.network.dto.FormSubmissionDto
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

interface FormRepository {
    suspend fun getFormSchema(formId: String): Flow<Result<FormSchemaDto>>
    suspend fun submitForm(userId: String, formId: String, values: Map<String, Any>): Result<Unit>
    suspend fun getPreviousSubmission(userId: String, formId: String): FormSubmissionDto?
    suspend fun seedFormIfAbsent(formId: String, data: Map<String, Any>)
    suspend fun updateForm(formId: String, data: Map<String, Any>)
    suspend fun deleteForm(formId: String)
    suspend fun getDynamicOptions(type: String): Flow<List<String>>
}

class FormRepositoryImpl @Inject constructor(
    private val firestoreHolder: FirestoreHolder,
    private val mapper: FormDtoMapper,
    private val dispatchers: DispatcherProvider,
) : FormRepository {

    private val schemaCache = ConcurrentHashMap<String, FormSchemaDto>()
    private val seededFormIds = mutableSetOf<String>()

    private val forms get() = firestoreHolder.firestore.collection("forms")
    private val submissions get() = firestoreHolder.firestore.collection("formSubmissions")

    override suspend fun getFormSchema(formId: String): Flow<Result<FormSchemaDto>> = withContext(dispatchers.io) {
        schemaCache[formId]?.let { return@withContext flowOf(Result.Success(it)) }
        try {
            Log.d("FIREBASE_CALL", "Read Form Schema: $formId")
            val data = forms.document(formId).get().await().data
                ?: return@withContext flowOf(Result.Error(Exception("Form $formId not found")))
            val dto = mapper.mapToDto(data as HashMap)
            schemaCache[formId] = dto
            flowOf(Result.Success(dto))
        } catch (e: Exception) {
            flowOf(Result.Error(e))
        }
    }

    override suspend fun submitForm(userId: String, formId: String, values: Map<String, Any>): Result<Unit> = withContext(dispatchers.io) {
        try {
            Log.d("FIREBASE_CALL", "Submit Form: $formId for user: $userId")
            val submission = mapOf(
                "submittedAt" to Timestamp.now(),
                "values" to values,
            )
            submissions.document(userId).collection("responses").document(formId).set(submission).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun seedFormIfAbsent(formId: String, data: Map<String, Any>) = withContext(dispatchers.io) {
        if (seededFormIds.contains(formId)) return@withContext
        val doc = forms.document(formId).get().await()
        if (!doc.exists()) {
            Log.d("FIREBASE_CALL", "Seeding Form: $formId")
            forms.document(formId).set(data).await()
        } else {
            schemaCache[formId] = mapper.mapToDto(doc.data as HashMap)
        }
        seededFormIds.add(formId)
    }

    override suspend fun updateForm(formId: String, data: Map<String, Any>) = withContext(dispatchers.io) {
        Log.d("FIREBASE_CALL", "Updating Form: $formId")
        forms.document(formId).set(data).await()
        schemaCache[formId] = mapper.mapToDto(data as HashMap)
        seededFormIds.add(formId)
        Unit
    }

    override suspend fun deleteForm(formId: String) = withContext(dispatchers.io) {
        Log.d("FIREBASE_CALL", "Deleting Form: $formId")
        forms.document(formId).delete().await()
        schemaCache.remove(formId)
        seededFormIds.remove(formId)
        Unit
    }

    override suspend fun getPreviousSubmission(userId: String, formId: String): FormSubmissionDto? = withContext(dispatchers.io) {
        try {
            Log.d("FIREBASE_CALL", "Read Previous Submission: $formId for user: $userId")
            val doc = submissions.document(userId).collection("responses").document(formId).get().await()
            if (!doc.exists()) return@withContext null
            @Suppress("UNCHECKED_CAST")
            FormSubmissionDto(
                submittedAt = doc.getTimestamp("submittedAt"),
                values = (doc.get("values") as? Map<String, Any>) ?: emptyMap(),
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getDynamicOptions(type: String): Flow<List<String>> = when (type) {
        "VACCINATION_SERIES" -> getVaccinationSeries()
        else -> flowOf(emptyList())
    }

    private fun getVaccinationSeries(): Flow<List<String>> = flow {
        try {
            val snapshot = firestoreHolder.firestore.collectionGroup("vaccinations").get().await()
            val allSeries = snapshot.documents.flatMap { doc ->
                val items = doc.get("items") as? Map<String, Map<String, Any?>> ?: emptyMap()
                items.values.mapNotNull { it["seriesId"] as? String }
            }.distinct().sorted()
            emit(allSeries)
        } catch (e: Exception) {
            emit(emptyList<String>())
        }
    }
}
