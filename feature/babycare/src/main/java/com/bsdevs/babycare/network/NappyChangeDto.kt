package com.bsdevs.babycare.network

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class NappyChangeDto(
    val id: String? = null,
    val userId: String? = null,
    val date: String? = null, // ISO Local Date (yyyy-MM-dd)
    val time: String? = null, // HH:mm
    val type: String? = null   // "Wet", "Dirty", "Both"
) : Parcelable
