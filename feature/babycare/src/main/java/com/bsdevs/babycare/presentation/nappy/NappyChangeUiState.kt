package com.bsdevs.babycare.presentation.nappy

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class NappyChangeUiState(
    val id: String? = null,
    val originalDocId: String? = null,
    val date: String = "",
    val time: String = "",
    val type: String = "Wet",
    val isLoading: Boolean = false,
    val comment: String = "",
    val error: String? = null,
    val showTimePicker: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val isPlayingTurdAnimation: Boolean = false
)
