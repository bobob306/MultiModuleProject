package com.bsdevs.babycare.network

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@Parcelize
data class TemperatureDto(
    val id: String? = null,
    val userId: String? = null,
    val date: String? = null, // ISO Local Date (yyyy-MM-dd)
    val time: String? = null, // HH:mm
    val dateTime: String = "",
    val temperature: Double? = null,
    val comment: String? = null,
) : Parcelable
