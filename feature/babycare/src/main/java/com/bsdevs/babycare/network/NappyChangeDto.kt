package com.bsdevs.babycare.network

import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
data class NappyChangeDto(
    val id: String? = null,
    val userId: String? = null,
    val date: String? = null, // ISO Local Date (yyyy-MM-dd)
    val time: String? = null, // HH:mm
    val dateTime: String = "",
    val comment: String? = null,
    val type: String? = null   // "Wet", "Dirty", "Both"
)
