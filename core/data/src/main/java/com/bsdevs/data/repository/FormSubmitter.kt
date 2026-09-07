package com.bsdevs.data.repository

import com.bsdevs.common.result.Result

interface FormSubmitter {
    suspend fun submit(userId: String, target: String, entityId: String?, values: Map<String, Any>): Result<Unit>
}
