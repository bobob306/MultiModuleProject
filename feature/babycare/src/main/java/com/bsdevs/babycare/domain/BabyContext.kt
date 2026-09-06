package com.bsdevs.babycare.domain

import com.bsdevs.babycare.network.UnifiedEventDto

data class BabyContext(
    val birthDate: String? = null,
    val gender: String? = null,
    val measurements: List<UnifiedEventDto> = emptyList(),
    val nappyEvents: List<UnifiedEventDto> = emptyList()
)
