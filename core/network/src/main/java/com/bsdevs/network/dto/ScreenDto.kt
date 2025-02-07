package com.bsdevs.network.dto

sealed class ScreenDto(
    open val index: Int
) {
    data class Unknown(override val index: Int) : ScreenDto(index)
    data class TitleDto(
        override val index: Int,
        val content: String,
    ) : ScreenDto(index)

    data class SubtitleDto(
        override val index: Int,
        val content: String,
    ) : ScreenDto(index)

    data class SpacerDto(
        override val index: Int,
        val size: SizeDto,
    ) : ScreenDto(index)
}

data class SizeDto(
    val type: SpacerType,
    val size: Int? = null,
    val weight: Float? = null,
)

enum class SpacerType {
    HEIGHT, WEIGHT,
}