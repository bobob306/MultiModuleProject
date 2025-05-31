package com.bsdevs.coffeescreen.screens.homescreen.viewdata

import com.bsdevs.coffeescreen.network.CoffeeDto

data class CoffeeHomeScreenViewData(
    val viewData: List<CoffeeHomeScreenViewDatas>
)

sealed class CoffeeHomeScreenViewDatas {
    data class Button(
        val label: String,
        val destination: ButtonDestination,
        val enabled: Boolean = true,
        val icon: ButtonIcon? = null,
        val type: ButtonType? = ButtonType.PRIMARY,
        val size: ButtonSize? = ButtonSize.MEDIUM,
    ) : CoffeeHomeScreenViewDatas()

    data class Image(
        val url: String,
        val description: String,
    ) : CoffeeHomeScreenViewDatas()

    data class HeaderSection(
        val title: String,
        val description: String,
    ) : CoffeeHomeScreenViewDatas()

    data class CoffeeList(
        val coffeeList: List<CoffeeDto>?
    ) : CoffeeHomeScreenViewDatas()
}

enum class ButtonDestination { INPUT, EDIT, HOME }

enum class ButtonIcon { INPUT, EDIT, HOME }

enum class ButtonType { PRIMARY, SECONDARY, TERTIARY }

enum class ButtonSize { SMALL, MEDIUM, LARGE }