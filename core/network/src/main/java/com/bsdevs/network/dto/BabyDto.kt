package com.bsdevs.network.dto

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@Keep
data class BabyDto(
    val id: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    @get:PropertyName("birthDate")
    @set:PropertyName("birthDate")
    var birthDate: String? = null,
    @get:PropertyName("birth_date")
    @set:PropertyName("birth_date")
    var birth_date: String? = null,
    @get:PropertyName("dateOfBirth")
    @set:PropertyName("dateOfBirth")
    var dateOfBirth: String? = null,
    val gender: String? = null // "male" or "female"
) {
    val effectiveBirthDate: String?
        get() = birthDate ?: birth_date ?: dateOfBirth
}
