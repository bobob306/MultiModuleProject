package com.bsdevs.babycare.presentation.measurement

sealed class MeasurementEvent {
    object SaveSuccess : MeasurementEvent()
    object DeleteSuccess : MeasurementEvent()
}
