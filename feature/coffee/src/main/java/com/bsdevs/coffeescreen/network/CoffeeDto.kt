package com.bsdevs.coffeescreen.network

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavType
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Date
import javax.annotation.processing.Generated

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
    val id: String? =  null,
    val rating: Int? = null,
) : Parcelable