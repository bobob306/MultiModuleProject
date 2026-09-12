package com.bsdevs.network.dto

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@Keep
data class TaskDto(
    val id: String? = null,
    val name: String? = null,
    val isCompleted: Boolean = false
)

@IgnoreExtraProperties
@Serializable
@Keep
data class TaskDoc(
    val items: Map<String, TaskDto> = emptyMap()
)
