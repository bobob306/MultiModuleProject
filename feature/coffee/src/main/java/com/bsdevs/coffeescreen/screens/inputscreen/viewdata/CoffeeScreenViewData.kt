package com.bsdevs.coffeescreen.screens.inputscreen.viewdata

import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.InputViewData.InputRadioVD
import com.bsdevs.coffeescreen.screens.inputscreen.viewdata.InputViewData.InputVD
import java.time.LocalDate

data class RadioInputViewData(
    val label: String,
    val isDecaf: Boolean,
)

data class CoffeeScreenViewData(
    val roastDate: LocalDate? = null,
    val isButtonEnabled: Boolean = false,
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
        InputVD(
            label = "Coffee Preparation Method",
            inputList = beanPreparationMethod,
            selectedSet = emptySet(),
            searchText = null,
            inputType = InputType.METHOD,
            singleInput = true,
        ),
        InputVD(
            label = "Roaster",
            inputList = coffeeRoasters,
            selectedSet = emptySet(),
            searchText = "",
            inputType = InputType.ROASTER,
            singleInput = true,
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
        val singleInput: Boolean = false
    ) : InputViewData()

    data class InputRadioVD(
        val label: String,
        val option: List<RadioInputViewData>,
        val isDecaf: Boolean,
    ) : InputViewData()
}

enum class InputType {
    BEANS, ORIGIN, TASTE, METHOD, ROASTER
}

internal val beanPreparationMethod =
    listOf("Washed", "Natural", "Honey", "Wet", "Anaerobic", "Pulped")

internal val coffeeBeanTypes: List<String> = listOf(
    "Arabica", "Robusta", "Liberica", "Excelsa", "Typica",
    "Bourbon", "Catuai", "Caturra", "Pacamara", "Gesha (or Geisha)",
    "SL28", "SL34", "Mundo Novo", "Pacas", "Maragogipe",
    "Kent", "Ethiopian Heirloom", "Sidra", "Conilon", "Java-Ineac",
    "Kona Robusta", "Kona", "Blue Mountain", "Sumatra Mandheling", "Java",
    "Ethiopian Yirgacheffe", "Ethiopian Sidamo", "Colombian Supremo/Excelso",
    "Brazilian Santos", "Vietnamese Robusta", "Tabi"
)

internal val coffeeRoasters: List<String> = listOf(
    "CoffeeLink", "Wogan", "Pact"
)

internal val coffeeTastingNotesList: List<String> = listOf(
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

internal val originCountries: List<String> = listOf(
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

val sampleRoasters = listOf(
    "Pact Coffee",
    "CoffeeLink",
    "Wogan Coffee"
)

fun generateSampleCoffeeDto(count: Int): List<CoffeeDto> {
    val list = mutableListOf<CoffeeDto>()
    val random = java.util.Random()

    // Ensure sampleRoasters is defined as:
    // val sampleRoasters = listOf("Pact Coffee", "CoffeeLink", "Wogan Coffee")
    // This should be defined at the top level of your file or passed into the function.

    for (i in 1..count) {
        val year = 2024
        val month = random.nextInt(12) + 1
        val day = random.nextInt(28) + 1
        val roastDate = String.format("%d-%02d-%02d", year, month, day)

        val numBeanTypes = if (i % 4 == 0) 2 else 1
        val beans = coffeeBeanTypes.shuffled().take(numBeanTypes)

        val numOrigins = if (i % 4 == 0) 2 else 1
        val origins = listOf(originCountries.random())

        val numTastingNotes = if (i % 4 == 0) 1 else 2
        val notes = listOf(coffeeTastingNotesList.random(), coffeeTastingNotesList.random(), coffeeTastingNotesList.random())

        val singleMethod = listOf(beanPreparationMethod.random())

        val singleRoaster = sampleRoasters.random()

        val isDecaf = random.nextBoolean() && (i % 4 == 0)

        list.add(
            CoffeeDto(
                roastDate = roastDate,
                beanTypes = beans,
                originCountries = origins,
                tastingNotes = notes,
                beanPreparationMethod = singleMethod,
                roaster = singleRoaster, // This will now correctly pick from the restricted list
                isDecaf = isDecaf,
                label = singleRoaster + " " + origins.joinToString(", ") + " " +
                        singleMethod.first() + " " + roastDate,
                userId = "user123",
                id = "coffee$i"
            )
        )
    }
    return list
}

fun generateSampleCoffeeScreenViewData(): CoffeeScreenViewData {
    val random = java.util.Random()

    val year = 2024
    val month = random.nextInt(12) + 1
    val day = random.nextInt(28) + 1
    val roastDate = LocalDate.of(year, month, day)

    val isButtonEnabled = random.nextBoolean()

    val beansSelected = coffeeBeanTypes.shuffled().take(random.nextInt(3) + 1).toSet()
    val originSelected = originCountries.shuffled().take(random.nextInt(2) + 1).toSet()
    val tasteSelected = coffeeTastingNotesList.shuffled().take(random.nextInt(4) + 1).toSet()
    val methodSelected = setOf(beanPreparationMethod.random())
    val roasterSelected = setOf(coffeeRoasters.random())
    val isDecafSelected = random.nextBoolean()

    return CoffeeScreenViewData(
        roastDate = roastDate,
        isButtonEnabled = isButtonEnabled,
        inputs = listOf(
            InputVD(
                label = "Coffee Type(s)",
                inputList = coffeeBeanTypes,
                selectedSet = beansSelected,
                searchText = null,
                inputType = InputType.BEANS
            ),
            InputVD(
                label = "Coffee Origin(s)",
                inputList = originCountries,
                selectedSet = originSelected,
                searchText = null,
                inputType = InputType.ORIGIN
            ),
            InputVD(
                label = "Coffee Tasting Notes",
                inputList = coffeeTastingNotesList,
                selectedSet = tasteSelected,
                searchText = "",
                inputType = InputType.TASTE
            ),
            InputVD(
                label = "Coffee Preparation Method",
                inputList = beanPreparationMethod,
                selectedSet = methodSelected,
                searchText = null,
                inputType = InputType.METHOD,
                singleInput = true
            ),
            InputVD(
                label = "Roaster",
                inputList = coffeeRoasters,
                selectedSet = roasterSelected,
                searchText = "",
                inputType = InputType.ROASTER,
                singleInput = true
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
                isDecaf = isDecafSelected,
            ),
        )
    )
}