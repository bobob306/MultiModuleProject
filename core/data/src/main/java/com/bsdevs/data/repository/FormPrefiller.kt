package com.bsdevs.data.repository

interface FormPrefiller {
    suspend fun loadExistingValues(userId: String, target: String, entityId: String): Map<String, Any>?
}
