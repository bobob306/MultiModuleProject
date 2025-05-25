package com.bsdevs.coffeescreen.network

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
@Parcelize
data class CoffeeDto(
    val roastDate: String? = null,
    val beanTypes: List<String>? = null,
    val originCountries: List<String>? = null,
    val tastingNotes: List<String>? = null,
    val beanPreparationMethod: List<String>? = null,
    val roaster: String? = null,
    val isDecaf: Boolean? = null,
    val label: String? = null,
) : Parcelable

val CoffeeDtoType = object : NavType<CoffeeDto>(
    isNullableAllowed = false
) {
    override fun get(bundle: Bundle, key: String): CoffeeDto? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, CoffeeDto::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(key)
        }
    }

    override fun parseValue(value: String): CoffeeDto {
        return Json.decodeFromString<CoffeeDto>(value)
    }

    override fun serializeAsValue(value: CoffeeDto): String {
        return Json.encodeToString(value)
    }

    override fun put(bundle: Bundle, key: String, value: CoffeeDto) {
        bundle.putParcelable(key, value)
    }
}