package com.bsdevs.coffeescreen.network

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@IgnoreExtraProperties
@Serializable
@Parcelize
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
) : Parcelable