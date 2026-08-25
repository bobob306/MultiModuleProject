package com.bsdevs.babycare.presentation.temperature

sealed class TemperatureUiEffect {
    object SaveSuccess : TemperatureUiEffect()
    object DeleteSuccess : TemperatureUiEffect()
    data class SaveError(val message: String) : TemperatureUiEffect()
}
