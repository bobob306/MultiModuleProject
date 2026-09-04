package com.bsdevs.babycare.network

import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
data class FeedingDto(
    val id: String? = null,
    val userId: String? = null,
    val date: String? = null, // ISO Local Date (yyyy-MM-dd)
    val startTime: String? = null, // HH:mm
    val dateTime: String = "",
    val leftDuration: Long = 0, // seconds
    val rightDuration: Long = 0, // seconds
    val totalDuration: Long = 0, // seconds
    val mainFeedingSide: String? = null, // "Left", "Right", "Both", "Bottle"
    val bottleAmountMl: Int? = null,
    val comment: String? = null,
    val hasVitaminD: Boolean = false,
)
