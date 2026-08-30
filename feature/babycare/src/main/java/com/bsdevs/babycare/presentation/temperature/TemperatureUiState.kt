package com.bsdevs.babycare.presentation.temperature

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class TemperatureUiState(
    val id: String? = null,
    val date: String = LocalDate.now().toString(),
    val originalDate: String? = null,
    val time: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val temperature: String = "37.0",
    val temperatureValue: Int = 370,
    val comment: String = "",
    val dates: List<String> = emptyList(),
    val dailyReadings: Map<String, List<TemperatureItem>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showSheet: Boolean = false,
    val showTimePicker: Boolean = false,
    val showDatePicker: Boolean = false,
    val showDeleteConfirmation: Boolean = false
)

data class TemperatureItem(
    val id: String,
    val date: String,
    val time: String,
    val temperature: Double,
    val comment: String?
)
