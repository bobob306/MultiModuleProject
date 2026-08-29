package com.bsdevs.babycare.presentation.measurement

sealed class MeasurementEvent {
    object SaveSuccess : MeasurementEvent()
    object DeleteSuccess : MeasurementEvent()
    data class SaveError(val message: String) : MeasurementEvent()
}
