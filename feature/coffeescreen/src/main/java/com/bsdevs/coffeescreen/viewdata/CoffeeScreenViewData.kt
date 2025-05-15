package com.bsdevs.coffeescreen.viewdata

import android.R.attr.label
import com.bsdevs.coffeescreen.viewdata.InputViewData.InputRadioVD
import com.bsdevs.coffeescreen.viewdata.InputViewData.InputVD
import java.time.LocalDate

data class InputsViewData(
    val inputList: List<String> = coffeeTastingNotesList.sortedBy { it } ,
    val selectedSet: Set<String> = emptySet(),
    val searchText: String? = null,
    val label: String,
)

data class RadioInputsViewData(
    val label: String = "Decaf",
    val option: List<RadioInputViewData>,
    val isDecaf: Boolean = false,
)

data class RadioInputViewData(
    val label: String,
    val isDecaf: Boolean,
)

data class CoffeeScreenViewData(
    val roastDate: LocalDate? = null,
    val inputs: List<InputViewData> = listOf<InputViewData>(
        InputVD(
            label = "Coffee Type(s)",
            inputList = coffeeBeanTypes,
            selectedSet = emptySet(),
            searchText = null,
            inputType = InputType.BEANS
            ),
        InputVD(
            label = "Coffee Origin(s)",
            inputList = originCountries,
            selectedSet = emptySet(),
            searchText = null,
            inputType = InputType.ORIGIN
        ),
        InputVD(
            label = "Coffee Tasting Notes",
            inputList = coffeeTastingNotesList,
            selectedSet = emptySet(),
            searchText = "",
            inputType = InputType.TASTE
        ),
        InputRadioVD(
            label = "Decaf",
            option = listOf(
                RadioInputViewData(
                    label = "Caffeinated",
                    isDecaf = false,
                ),
                RadioInputViewData(
                    label = "Decaffeinated",
                    isDecaf = true,
                )
            ),
            isDecaf = false,
            ),
    ),
)

sealed class InputViewData {
    data class InputVD(
        val label: String,
        val inputList: List<String>,
        val selectedSet: Set<String>,
        val searchText: String?,
        val inputType: InputType,
    ) : InputViewData()

    data class InputRadioVD(
        val label: String,
        val option: List<RadioInputViewData>,
        val isDecaf: Boolean,
    ) : InputViewData()
}

enum class InputType{
    BEANS, ORIGIN, TASTE,
}

private val coffeeBeanTypes: List<String> = listOf(
    "Arabica", "Robusta", "Liberica", "Excelsa", "Typica",
    "Bourbon", "Catuai", "Caturra", "Pacamara", "Gesha (or Geisha)",
    "SL28", "SL34", "Mundo Novo", "Pacas", "Maragogipe",
    "Kent", "Ethiopian Heirloom", "Sidra", "Conilon", "Java-Ineac",
    "Kona Robusta", "Kona", "Blue Mountain", "Sumatra Mandheling", "Java",
    "Ethiopian Yirgacheffe", "Ethiopian Sidamo", "Colombian Supremo/Excelso",
    "Brazilian Santos", "Vietnamese Robusta", "Tabi"
)

private val coffeeTastingNotesList: List<String> = listOf(
    "Berry",
    "Blueberry",
    "Raspberry",
    "Strawberry",
    "Citrus",
    "Lemon",
    "Orange",
    "Grapefruit",
    "Lime",
    "Stone Fruit",
    "Cherry",
    "Peach",
    "Plum",
    "Apricot",
    "Tropical Fruit",
    "Mango",
    "Pineapple",
    "Passion Fruit",
    "Dried Fruit",
    "Raisin",
    "Fig",
    "Date",
    "Jasmine",
    "Bergamot",
    "Rose",
    "Chamomile",
    "Lavender",
    "Caramel",
    "Chocolate",
    "Milk Chocolate",
    "Dark Chocolate",
    "Cocoa",
    "Vanilla",
    "Honey",
    "Maple Syrup",
    "Brown Sugar",
    "Molasses",
    "Almond",
    "Hazelnut",
    "Peanut",
    "Walnut",
    "Cocoa",
    "Cinnamon",
    "Nutmeg",
    "Clove",
    "Cardamom",
    "Pepper",
    "Earthy",
    "Woody",
    "Tobacco",
    "Herbal",
    "Grassy",
    "Damp Earth",
    "Caramelized",
    "Toasted",
    "Smoky",
    "Rubbery",
    "Tire-like",
    "Sulfur",
    "Bright",
    "Sparkling",
    "Tart",
    "Citric",
    "Malic",
    "Phosphoric",
    "Light",
    "Medium",
    "Full",
    "Syrupy",
    "Creamy",
    "Watery"
)

private val originCountries: List<String> = listOf(
    "Brazil",
    "Vietnam",
    "Colombia",
    "Indonesia",
    "Ethiopia",
    "Honduras",
    "India",
    "Uganda",
    "Mexico",
    "Peru",
    "Guatemala",
    "Nicaragua",
    "China",
    "Ivory Coast",
    "Costa Rica",
    "Kenya",
    "Papua New Guinea",
    "Tanzania",
    "El Salvador",
    "Ecuador",
    "Cameroon",
    "Laos",
    "Madagascar",
    "Thailand",
    "Venezuela",
    "Burundi",
    "Rwanda",
    "Democratic Republic of Congo",
    "Haiti",
    "Philippines"
)