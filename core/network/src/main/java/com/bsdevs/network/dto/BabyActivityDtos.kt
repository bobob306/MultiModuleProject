package com.bsdevs.network.dto

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
    val predictionGapMinutes: Long? = null
)

@IgnoreExtraProperties
@Serializable
data class MeasurementDto(
    val id: String? = null,
    val date: String? = null,
    val time: String? = null,
    val dateTime: String = "",
    val height: Double? = null,
    val weight: Double? = null,
    val headCircumference: Double? = null,
    val isMedical: Boolean = false,
    val comment: String? = null
)

@IgnoreExtraProperties
@Serializable
data class NappyChangeDto(
    val id: String? = null,
    val date: String? = null,
    val time: String? = null,
    val dateTime: String = "",
    val type: String? = null, // "Wet", "Dirty", "Both"
    val comment: String? = null
)

@IgnoreExtraProperties
@Serializable
data class TemperatureDto(
    val id: String? = null,
    val date: String? = null,
    val time: String? = null,
    val dateTime: String = "",
    val temperature: Double = 37.0,
    val comment: String? = null
)

@IgnoreExtraProperties
@Serializable
data class VaccinationDto(
    val id: String? = null,
    val date: String? = null,
    val time: String? = null,
    val dateTime: String = "",
    val vaccinationNames: List<String> = emptyList(),
    val location: String? = null,
    val seriesId: String? = null,
    val comment: String? = null
)
