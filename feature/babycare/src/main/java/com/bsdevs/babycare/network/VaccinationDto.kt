package com.bsdevs.babycare.network

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
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
