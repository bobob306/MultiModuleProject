package com.bsdevs.network.dto

import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
data class DailyLogDto(
    val date: String = "", // Document ID (e.g., "2026-08-19")
    val userId: String = "",
    val events: List<BabyEvent> = emptyList()
)
