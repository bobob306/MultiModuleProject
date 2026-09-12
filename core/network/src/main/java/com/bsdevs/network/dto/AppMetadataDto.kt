package com.bsdevs.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AppMetadataDto(
    val screens: Map<String, List<ScreenDto>> = emptyMap(),
    val forms: Map<String, FormSchemaDto> = emptyMap()
)
