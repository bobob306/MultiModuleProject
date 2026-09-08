package com.bsdevs.network.dto

import com.bsdevs.network.dto.ButtonType.PRIMARY
import com.bsdevs.network.dto.LocationType.INTERNAL
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ScreenDto {
    abstract val index: Int

    @Serializable
    data class Unknown(@SerialName("index") override val index: Int) : ScreenDto()

    @Serializable
    @SerialName("TITLE")
    data class TitleDto(
        @SerialName("index") override val index: Int,
        @SerialName("content") val content: String,
    ) : ScreenDto()

    @Serializable
    @SerialName("SUBTITLE")
    data class SubtitleDto(
        @SerialName("index") override val index: Int,
        @SerialName("content") val content: String,
    ) : ScreenDto()

    @Serializable
    @SerialName("SPACER")
    data class SpacerDto(
        @SerialName("index") override val index: Int,
        @SerialName("size") val size: SizeDto,
    ) : ScreenDto()

    @Serializable
    @SerialName("IMAGE")
    data class ImageDto(
        @SerialName("index") override val index: Int,
        @SerialName("url") val url: String,
        @SerialName("contentDescription") val contentDescription: String? = null,
        @SerialName("height") val height: Int,
        @SerialName("width") val width: Int,
    ) : ScreenDto()

    @Serializable
    @SerialName("CARD")
    data class CardDto(
        @SerialName("index") override val index: Int,
        @SerialName("image") val image: ImageDto,
        @SerialName("title") val title: String,
        @SerialName("subtitle") val subtitle: String,
        @SerialName("backgroundColor") val backgroundColor: Int?,
    ) : ScreenDto()

    @Serializable
    @SerialName("NAVIGATION_BUTTON")
    data class NavigationButtonDto(
        @SerialName("index") override val index: Int,
        @SerialName("label") val label: String,
        @SerialName("location") val location: LocationType? = INTERNAL,
        @SerialName("destination") val destination: String,
        @SerialName("sort") val sort: ButtonType? = PRIMARY,
    ) : ScreenDto()

    @Serializable
    @SerialName("SMALL_TITLE")
    data class SmallTitleDto(
        @SerialName("index") override val index: Int,
        @SerialName("content") val content: String,
    ) : ScreenDto()

    @Serializable
    @SerialName("ACTIVITY_FEED")
    data class ActivityFeedDto(
        @SerialName("index") override val index: Int,
    ) : ScreenDto()

    @Serializable
    @SerialName("TILE_ROW")
    data class TileRowDto(
        @SerialName("index") override val index: Int,
        @SerialName("tiles") val tiles: List<TileDto> = emptyList(),
    ) : ScreenDto()

    @Serializable
    @SerialName("GROWTH_CHART")
    data class GrowthChartDto(
        @SerialName("index") override val index: Int,
        @SerialName("title") val title: String,
        @SerialName("dataType") val dataType: String,
    ) : ScreenDto()

    @Serializable
    @SerialName("MEASUREMENT_HISTORY")
    data class MeasurementHistoryDto(
        @SerialName("index") override val index: Int,
    ) : ScreenDto()

    @Serializable
    @SerialName("VACCINATION_HISTORY")
    data class VaccinationHistoryDto(
        @SerialName("index") override val index: Int,
    ) : ScreenDto()

    @Serializable
    @SerialName("TEMPERATURE_HISTORY")
    data class TemperatureHistoryDto(
        @SerialName("index") override val index: Int,
    ) : ScreenDto()

    @Serializable
    @SerialName("TEMPERATURE_CHART")
    data class TemperatureChartDto(
        @SerialName("index") override val index: Int,
    ) : ScreenDto()

    @Serializable
    @SerialName("FEEDING_FREQUENCY_CHART")
    data class FeedingFrequencyChartDto(
        @SerialName("index") override val index: Int,
    ) : ScreenDto()

    @Serializable
    @SerialName("FEEDING_GAP_CHART")
    data class FeedingGapChartDto(
        @SerialName("index") override val index: Int,
    ) : ScreenDto()

    @Serializable
    @SerialName("FEEDING_INSIGHT_CARD")
    data class FeedingInsightCardDto(
        @SerialName("index") override val index: Int,
    ) : ScreenDto()

    @Serializable
    @SerialName("SHOPPING_LIST")
    data class ShoppingListDto(
        @SerialName("index") override val index: Int,
    ) : ScreenDto()

    @Serializable
    data class TileDto(
        @SerialName("index") val index: Int,
        @SerialName("title") val title: String,
        @SerialName("iconName") val iconName: String,
        @SerialName("destination") val destination: String,
        @SerialName("subtitleType") val subtitleType: String? = null,
        @SerialName("sharedElementKey") val sharedElementKey: String? = null,
    )
}

@Serializable
data class SizeDto(
    @SerialName("spacerType") val type: SpacerType,
    @SerialName("size") val size: Int? = null,
    @SerialName("weight") val weight: Float? = null,
)

@Serializable
enum class SpacerType {
    HEIGHT, WEIGHT,
}

@Serializable
enum class LocationType {
    INTERNAL, EXTERNAL
}

@Serializable
enum class ButtonType {
    PRIMARY, SECONDARY, TERTIARY
}
