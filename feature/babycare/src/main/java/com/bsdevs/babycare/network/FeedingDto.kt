package com.bsdevs.babycare.network

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@Parcelize
data class FeedingDto(
    val id: String? = null,
    val userId: String? = null,
    val date: String? = null, // ISO Local Date (yyyy-MM-dd)
    val startTime: String? = null, // HH:mm
    val leftDuration: Long = 0, // seconds
    val rightDuration: Long = 0, // seconds
    val totalDuration: Long = 0, // seconds
    val mainFeedingSide: String? = null, // "Left", "Right", "Both", "Bottle"
    val bottleAmountMl: Int? = null
) : Parcelable
