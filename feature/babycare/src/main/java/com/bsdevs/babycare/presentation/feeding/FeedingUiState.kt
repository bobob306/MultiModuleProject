package com.bsdevs.babycare.presentation.feeding

data class FeedingUiState(
    val id: String? = null,
    val originalDocId: String? = null,
    val date: String = "",
    val startTime: String = "",
    val leftDuration: Long = 0,
    val rightDuration: Long = 0,
    val bottleAmountMl: Int? = null,
    val isLeftRunning: Boolean = false,
    val isRightRunning: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val comment: String = "",
    val showBottleDialog: Boolean = false,
    val showTimePicker: Boolean = false,
    val showDurationDialogForSide: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val showCancelConfirmation: Boolean = false,
    val isPlayingSplodge: Boolean = false,
    val hasVitaminD: Boolean = false
)
