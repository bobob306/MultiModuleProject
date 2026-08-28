package com.bsdevs.babycare.network

import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
data class TemperatureDto(
    val id: String? = null,
    val userId: String? = null,
    val date: String? = null, // ISO Local Date (yyyy-MM-dd)
    val time: String? = null, // HH:mm
    val dateTime: String = "",
    val temperature: Double = 37.0,
    val comment: String? = null,
)
