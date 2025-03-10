package com.bsdevs.data

sealed class ScreenData(
    open val index: Int,
) {
    data class Unknown(override val index: Int) : ScreenData(index)
    data class TitleData(
        override val index: Int,
        val content: String,
    ) : ScreenData(index)

    data class SubtitleData(
        override val index: Int,
        val content: String,
    ) : ScreenData(index)

    data class SpacerData(
        override val index: Int,
        val size: SizeData,
    ) : ScreenData(index)

    data class ImageData(
        override val index: Int,
        val url: String,
        val contentDescription: String?,
        val height: Int,
        val width: Int,
    ) : ScreenData(index)

    data class CardData(
        val image: ImageData,
        val title: String,
        val subtitle: String,
        val backgroundColor: Int?,
        val sort: CardTypeData?,
        val buttonRow: List<NavigationButtonData>?,
        val subheading: String?,
        val iconButtonRow: List<IconButtonData>?,
        override val index: Int,
    ) : ScreenData(index)

    data class NavigationButtonData(
        val label: String,
        val destination: String,
        val location: LocationTypeData,
        val sort: ButtonTypeData,
        override val index: Int,
    ) : ScreenData(index)

    data class IconButtonData(
        val label: String?,
        val destination: String,
        val location: LocationTypeData,
        val url: String,
        override val index: Int,
    ) : ScreenData(index)

    data class ProgressBarData(
        override val index: Int,
        val maxProgress: Int,
        val startProgress: Int?,
    ) : ScreenData(index)

    data class DividerData(
        override val index: Int,
    ) : ScreenData(index)

    data class CarouselData(
        val cards: List<CardData>,
        override val index: Int,
    ) : ScreenData(index)

    data class CheckboxData(
        val label: String,
        val checked: Boolean,
        val imageUrl: String?,
        override val index: Int,
    ) : ScreenData(index)

    data class CheckboxListData(
        override val index: Int,
        val checkboxes: List<CheckboxData>,
    ) : ScreenData(index)

    data class RadioButtonData(
        override val index: Int,
        val labels: List<RadioButtonLabelData>,
    ) : ScreenData(index)

    data class ChipData(
        override val index: Int,
        val label: String,
        val imageUrl: String?,
    ) : ScreenData(index)

    data class SwitchData(
        override val index: Int,
        val label: String,
        val imageUrl: String?,
        val checked: Boolean,
    ) : ScreenData(index)
}

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