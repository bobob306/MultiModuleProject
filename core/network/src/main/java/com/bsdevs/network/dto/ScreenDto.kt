package com.bsdevs.network.dto

import com.bsdevs.network.dto.ButtonType.PRIMARY
import com.bsdevs.network.dto.LocationType.INTERNAL
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

    @SerialName("CARD")
    data class CardDto(
        @SerialName("index") override val index: Int,
        @SerialName("image") val image: ImageDto,
        @SerialName("title") val title: String,
        @SerialName("subtitle") val subtitle: String,
        @SerialName("backgroundColor") val backgroundColor: Int?,
    ) : ScreenDto(index)

    @SerialName("BUTTON")
    data class ButtonDto(
        @SerialName("index") override val index: Int,
        @SerialName("label") val label: String,
        @SerialName("location") val location: LocationType? = INTERNAL,
        @SerialName("destination") val destination: String,
        @SerialName("sort") val sort: ButtonType? = PRIMARY,
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

enum class LocationType {
    INTERNAL, EXTERNAL
}

enum class ButtonType {
    PRIMARY, SECONDARY, TERTIARY
}