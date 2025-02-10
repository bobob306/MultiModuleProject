package com.bsdevs.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ScreenDto(
    open val index: Int
) {
    data class Unknown(override val index: Int) : ScreenDto(index)
    @SerialName("TITLE")
    data class TitleDto(
        @SerialName("index") override val index: Int,
        @SerialName("content") val content: String,
    ) : ScreenDto(index)

    @SerialName("SUBTITLE")
    data class SubtitleDto(
        @SerialName("index") override val index: Int,
        @SerialName("content") val content: String,
    ) : ScreenDto(index)

    @SerialName("SPACER")
    data class SpacerDto(
        @SerialName("index") override val index: Int,
        @SerialName("size") val size: SizeDto,
    ) : ScreenDto(index)

    @SerialName("IMAGE")
    data class ImageDto(
        @SerialName("index") override val index: Int,
        @SerialName("url") val url: String,
        @SerialName("contentDescription") val contentDescription: String? = null,
        @SerialName("height") val height: Int,
        @SerialName("width") val width: Int,
    ) : ScreenDto(index)
}

data class SizeDto(
    @SerialName("spacerType") val type: SpacerType,
    @SerialName("size") val size: Int? = null,
    @SerialName("weight") val weight: Float? = null,
)

enum class SpacerType {
    HEIGHT, WEIGHT,
}