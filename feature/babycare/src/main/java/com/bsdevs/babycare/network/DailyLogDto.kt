package com.bsdevs.babycare.network

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class DailyLogDto(
    val date: String = "", // Document ID (e.g., "2026-08-19")
    val userId: String = "",
    val events: List<UnifiedEventDto> = emptyList()
)

@IgnoreExtraProperties
data class UnifiedEventDto(
    val id: String = "",
    val type: String = "", // "NAPPY", "FEEDING", or "TEMPERATURE"
    val time: String = "",
    val dateTimeString: String = "",
    val comment: String? = null,

    // Nappy-specific fields (nullable)
    val nappyType: String? = null, // "Wet", "Dirty", "Both"

    // Feeding-specific fields (nullable)
    val mainFeedingSide: String? = null,
    val leftDuration: Long = 0,
    val rightDuration: Long = 0,
    val totalDuration: Long = 0,
    val bottleAmountMl: Int? = null,

    // Temperature-specific fields (nullable)
    val temperature: Double? = null
)
