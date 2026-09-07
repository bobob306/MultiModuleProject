package com.bsdevs.data.repository

import android.util.Log
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.FormDtoMapper
import com.bsdevs.network.dto.FormSchemaDto
import com.bsdevs.network.dto.FormSubmissionDto
import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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

    private val firestore get() = firestoreHolder.firestore
    private val schemas get() = firestore.collection("formSchemas")

    override suspend fun getFormSchema(formId: String): Flow<Result<FormSchemaDto>> = flow {
        emit(Result.Loading)
        try {
            val document = schemas.document(formId).get().await().data
            if (document != null) {
                emit(Result.Success(mapper.mapToDto(document as HashMap<*, *>)))
            } else {
                emit(Result.Error(Exception("Form schema not found")))
            }
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override suspend fun submitForm(userId: String, formId: String, values: Map<String, Any>): Result<Unit> = withContext(dispatchers.io) {
        try {
            val submission = mapOf(
                "submittedAt" to Timestamp.now(),
                "values" to values
            )
            firestore.collection("users").document(userId).collection("formSubmissions").document(formId).set(submission).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getPreviousSubmission(userId: String, formId: String): FormSubmissionDto? = withContext(dispatchers.io) {
        try {
            val document = firestore.collection("users").document(userId).collection("formSubmissions").document(formId).get().await()
            document.toObject<FormSubmissionDto>()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun seedFormIfAbsent(formId: String, data: Map<String, Any>) {
        try {
            val doc = schemas.document(formId).get().await()
            if (!doc.exists()) {
                schemas.document(formId).set(data).await()
            }
        } catch (e: Exception) {
            Log.e("FORM_REPO", "Failed to seed form", e)
        }
    }

    override suspend fun updateForm(formId: String, data: Map<String, Any>) {
        try {
            schemas.document(formId).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("FORM_REPO", "Failed to update form", e)
        }
    }

    override suspend fun deleteForm(formId: String) {
        try {
            schemas.document(formId).delete().await()
        } catch (e: Exception) {
            Log.e("FORM_REPO", "Failed to delete form", e)
        }
    }

    override suspend fun getDynamicOptions(type: String): Flow<List<String>> = flow {
        try {
            val document = firestore.collection("dynamicOptions").document(type).get().await()
            val options = (document.data?.get("options") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            emit(options)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}
