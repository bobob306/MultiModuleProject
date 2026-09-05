package com.bsdevs.babycare.presentation.measurement

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class MeasurementUiState(
    val id: String? = null,
    val date: String = LocalDate.now().toString(),
    val time: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val height: Double? = null,
    val weight: Double? = null,
    val headCircumference: Double? = null,
    val isMedical: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val comment: String = "",
    
    // Growth Chart Data
    val allMeasurements: List<com.bsdevs.babycare.network.MeasurementDto> = emptyList(),
    val showMedicalOnly: Boolean = false,
    val showWhoOverlay: Boolean = false,
    val birthDate: String? = null,
    val babyGender: String? = null,

    // Form Toggles
    val recordHeight: Boolean = false,
    val recordWeight: Boolean = false,
    val recordHeadCircumference: Boolean = false,
    val showSheet: Boolean = false,
    val showTimePicker: Boolean = false,
    val showDatePicker: Boolean = false,
    val showDeleteConfirmation: Boolean = false
)
