package com.bsdevs.homescreen

import com.bsdevs.common.result.Result
import com.bsdevs.network.dto.FormSchemaDto
import com.bsdevs.network.dto.FormSubmissionDto
import com.bsdevs.network.repository.FormRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeFormRepository : FormRepository {
    val seededForms = mutableMapOf<String, Map<String, Any>>()

    override suspend fun getFormSchema(formId: String): Flow<Result<FormSchemaDto>> =
        flowOf(Result.Error(UnsupportedOperationException("Not used in ViewModel tests")))

    override suspend fun submitForm(userId: String, formId: String, values: Map<String, Any>): Result<Unit> =
        Result.Error(UnsupportedOperationException("Not used in ViewModel tests"))

    override suspend fun getPreviousSubmission(userId: String, formId: String): FormSubmissionDto? = null

    override suspend fun seedFormIfAbsent(formId: String, data: Map<String, Any>) {
        seededForms.putIfAbsent(formId, data)
    }
}
