package com.bsdevs.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupermarketDto(
    @SerialName("Supermarkets")
    val supermarkets: List<String>
)

enum class Supermarket(val displayName: String) {
    ALDI(displayName = "Aldi"),
    ASDA(displayName = "Asda"),
    COOP(displayName = "Co-op"),
    ICELAND(displayName = "Iceland"),
    LIDL(displayName = "Lidl"),
    MARKS_AND_SPENCER(displayName = "M&S"),
    MORRISONS(displayName = "Morrisons"),
    SAINSBURYS(displayName = "Sainsbury's"),
    TESCO(displayName = "Tesco"),
    WAITROSE(displayName = "Waitrose")
}

enum class FoodCategory {
    FRUIT,
    VEGETABLES,
    MEAT,
    FISH,
    DAIRY,
    BAKERY,
    CANNED_GOODS,
    VEGAN,
    SNACKS,
    BEVERAGES,
    ALCOHOL,
    OTHER
}

@Serializable
data class ShoppingItemDto(
    @SerialName("id") val id: Int,
    @SerialName("price") val price: Double,
    @SerialName("name") val name: String,
    @SerialName("supermarket") val supermarket: Supermarket,
    @SerialName("quantity") val quantity: Int? = null,
    @SerialName("weight") val weight: Double? = null,
    @SerialName("frozen") val frozen: Boolean? = false,
    @SerialName("date") val date: String,
    @SerialName("category") val category: String,
    @SerialName("brand") val brand: String,
)
