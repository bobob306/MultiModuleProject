package com.bsdevs.network.dto

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@Keep
data class ShoppingListDto(
    val id: String? = null,
    val name: String? = null,
    val isCompleted: Boolean = false
)

@IgnoreExtraProperties
@Serializable
@Keep
data class ShoppingListDoc(
    val items: Map<String, ShoppingListDto> = emptyMap()
)
