package com.bsdevs.data

sealed class NetworkScreenData(
    open val index: Int,
) {
    data class Unknown(override val index: Int) : NetworkScreenData(index)
    data class SmallTitleDataNetwork(
        override val index: Int,
        val content: String,
    ) : NetworkScreenData(index)

    data class MediumTitleDataNetwork(
        override val index: Int,
        val content: String,
    ) : NetworkScreenData(index)

    data class LargeTitleDataNetwork(
        override val index: Int,
        val content: String,
    ) : NetworkScreenData(index)

    data class SubtitleDataNetwork(
        override val index: Int,
        val content: String,
    ) : NetworkScreenData(index)

    data class SpacerDataNetwork(
        override val index: Int,
        val size: SizeData,
    ) : NetworkScreenData(index)

    data class ImageDataNetwork(
        override val index: Int,
        val url: String,
        val contentDescription: String?,
        val height: Int,
        val width: Int,
    ) : NetworkScreenData(index)

    data class CardDataNetwork(
        val image: ImageDataNetwork,
        val title: String,
        val subtitle: String,
        val backgroundColor: Int?,
        val sort: CardTypeData?,
        val buttonRow: List<NavigationButtonDataNetwork>?,
        val subheading: String?,
        val iconButtonRow: List<IconButtonDataNetwork>?,
        override val index: Int,
    ) : NetworkScreenData(index)

    data class NavigationButtonDataNetwork(
        val label: String,
        val destination: String,
        val location: LocationTypeData,
        val sort: ButtonTypeData,
        override val index: Int,
    ) : NetworkScreenData(index)

    data class IconButtonDataNetwork(
        val label: String?,
        val destination: String,
        val location: LocationTypeData,
        val url: String,
        override val index: Int,
    ) : NetworkScreenData(index)

    data class ProgressBarDataNetwork(
        override val index: Int,
        val maxProgress: Int,
        val startProgress: Int?,
    ) : NetworkScreenData(index)

    data class DividerDataNetwork(
        override val index: Int,
    ) : NetworkScreenData(index)

    data class CarouselDataNetwork(
        val cards: List<CardDataNetwork>,
        override val index: Int,
    ) : NetworkScreenData(index)

    data class CheckboxDataNetwork(
        val label: String,
        val checked: Boolean,
        val imageUrl: String?,
        override val index: Int,
    ) : NetworkScreenData(index)

    data class CheckboxListDataNetwork(
        override val index: Int,
        val checkboxes: List<CheckboxDataNetwork>,
    ) : NetworkScreenData(index)

    data class RadioButtonDataNetwork(
        override val index: Int,
        val labels: List<RadioButtonLabelData>,
    ) : NetworkScreenData(index)

    data class ChipDataNetwork(
        override val index: Int,
        val label: String,
        val imageUrl: String?,
    ) : NetworkScreenData(index)

    data class SwitchDataNetwork(
        override val index: Int,
        val label: String,
        val imageUrl: String?,
        val checked: Boolean,
    ) : NetworkScreenData(index)

    data class BabyDashboardTilesNetwork(
        override val index: Int,
        val content: List<BabyDashboardTileNetwork>
    ) : NetworkScreenData(index)

    // ➕ 2. The sticky day summary header with aggregate counts
    data class BabyFeedHeaderNetwork(
        override val index: Int,
        val title: String,
        val feedingCount: Int,
        val nappyCount: Int
    ) : NetworkScreenData(index)

    // ➕ 3. The actual activity log row tracking card item
    data class BabyFeedRowNetwork(
        override val index: Int,
        val id: String,
        val activityType: String, // "NAPPY" or "FEEDING"
        val title: String,
        val subtitle: String,
        val time: String,
        val rawDate: String
    ) : NetworkScreenData(index)
    data class ActivityFeedNetwork(
        override val index: Int,
        val content: List<NetworkScreenData>
    ) : NetworkScreenData(index)
}

data class BabyDashboardTileNetwork(
    val lastNappyChange: String?,
    val lastFeeding: String?,
    val destination: String,
    val locationTypeData: LocationTypeData,
    val label: String,
)

data class RadioButtonLabelData(
    val label: String?,
    val imageUrl: String?,
    val index: Int,
)

data class SizeData(
    val type: SpacerTypeData,
    val height: Int?,
    val weight: Float?,
)

enum class SpacerTypeData {
    HEIGHT, WEIGHT,
}

enum class ButtonTypeData {
    PRIMARY, SECONDARY, TERTIARY
}

enum class LocationTypeData {
    INTERNAL, EXTERNAL
}

enum class CardTypeData {
    HEADER_IMAGE, TEXT, BUTTON_IMAGE, SQUARE_IMAGE,
}