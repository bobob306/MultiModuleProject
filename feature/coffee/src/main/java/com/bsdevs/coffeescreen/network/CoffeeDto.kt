package com.bsdevs.coffeescreen.network

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@Keep
data class CoffeeDto(
    val roastDate: String? = null,
    val beanTypes: List<String>? = null,
    val originCountries: List<String>? = null,
    val tastingNotes: List<String>? = null,
    val beanPreparationMethod: List<String>? = null,
    val roaster: String? = null,
    @get:PropertyName("isDecaf") val isDecaf: Boolean? = null,
    val label: String? = null,
    val userId: String? = null,
    val id: String? = null,
    val rating: Int? = null,
)
