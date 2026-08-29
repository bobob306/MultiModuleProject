package com.bsdevs.babycare.network

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class MeasurementDto(
    val id: String? = null,
    val date: String? = null,
    val time: String? = null,
    val dateTime: String = "",
    val height: Double? = null,
    val weight: Double? = null,
    val isMedical: Boolean = false,
    val comment: String? = null
)
