package com.bsdevs.network.repository

interface FormPrefiller {
    suspend fun loadExistingValues(userId: String, target: String, entityId: String): Map<String, Any>?
}
