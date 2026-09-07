package com.bsdevs.network.dto

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

@IgnoreExtraProperties
@Serializable
@Keep
data class ShotDto(
    val id: String? = null,
    val date: String? = null,
    val weightIn: Double? = null,
    val weightOut: Double? = null,
    val time: Int? = null,
    val rating: Int? = null,
)

@Serializable
@Keep
data class CoffeeInputScreenDto(
    @get:PropertyName("BEANS") val BEANS: List<String> = emptyList(),
    @get:PropertyName("CAFFEINE") val CAFFEINE: List<String> = emptyList(),
    @get:PropertyName("METHOD") val METHOD: List<String> = emptyList(),
    @get:PropertyName("ORIGIN") val ORIGIN: List<String> = emptyList(),
    @get:PropertyName("ROASTER") val ROASTER: List<String> = emptyList(),
    @get:PropertyName("TASTE") val TASTE: List<String> = emptyList(),
)
