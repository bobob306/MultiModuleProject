package com.bsdevs.network.repository

import com.bsdevs.common.result.Result

interface FormDeleter {
    suspend fun delete(userId: String, target: String, entityId: String): Result<Unit>
}
