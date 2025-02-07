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
}
data class SizeData(
    val type: SpacerTypeData,
    val height: Int,
)
enum class SpacerTypeData {
    HEIGHT, WEIGHT,
}