package com.bsdevs.babycare.presentation.nappy

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class NappyChangeUiState(
    val id: String? = null,
    val originalDocId: String? = null,
    val date: String = LocalDate.now().toString(),
    val time: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val type: String = "Wet",
    val isLoading: Boolean = false,
    val comment: String = "",
    val error: String? = null,
)
