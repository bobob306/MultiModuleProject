package com.bsdevs.coffeescreen.viewdata

import java.time.LocalDate

data class InputViewData(
    val inputList: List<String> = coffeeTastingNotesList,
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
    val coffeeTypes: List<String> = coffeeBeanTypes,
    val selectedCoffeeTypes: Set<String> = emptySet(),
    val roastDate: LocalDate? = null,
    val originCountryOptions: List<String> = originCountries,
    val selectedOriginCountries: Set<String> = emptySet(),
    val decaf: Boolean = false,
    val beanTypeInput: InputViewData = InputViewData(
        label = "Coffee Type(s)",
        inputList = coffeeBeanTypes,
        selectedSet = emptySet(),
        searchText = "",
    ),
    val originInput: InputViewData = InputViewData(
        label = "Coffee Origin(s)",
        inputList = originCountries,
        selectedSet = emptySet(),
        searchText = null,
    ),
    val coffeeTastingNotesInput: InputViewData = InputViewData(
        label = "Coffee Tasting Notes",
        inputList = coffeeTastingNotesList,
        selectedSet = emptySet(),
        searchText = "",
    ),
    val decafInput: RadioInputsViewData = RadioInputsViewData(
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
    )
)

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