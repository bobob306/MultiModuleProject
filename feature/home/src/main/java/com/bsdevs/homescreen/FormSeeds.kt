package com.bsdevs.homescreen

internal object FormSeeds {

    val coffeeLog: Map<String, Any> = mapOf(
        "title" to "Log a coffee",
        "submitTarget" to "coffeeLog",
        "submitDestination" to "coffee_home",
        "deletable" to false,
        "fields" to listOf(
            mapOf(
                "fieldKey" to "bean_types", "type" to "DROPDOWN", "label" to "Coffee Type(s)",
                "required" to true, "index" to 0, "multiSelect" to true,
                "options" to listOf(
                    "Arabica", "Robusta", "Liberica", "Excelsa", "Typica", "Bourbon",
                    "Catuai", "Caturra", "Pacamara", "Gesha (or Geisha)", "SL28", "SL34",
                    "Mundo Novo", "Pacas", "Maragogipe", "Kent", "Ethiopian Heirloom",
                    "Sidra", "Conilon", "Java-Ineac", "Kona Robusta", "Kona",
                    "Blue Mountain", "Sumatra Mandheling", "Java", "Ethiopian Yirgacheffe",
                    "Ethiopian Sidamo", "Colombian Supremo/Excelso", "Brazilian Santos",
                    "Vietnamese Robusta", "Tabi", "Centroamericano",
                ),
            ),
            mapOf(
                "fieldKey" to "origin_countries", "type" to "DROPDOWN", "label" to "Coffee Origin(s)",
                "required" to true, "index" to 1, "multiSelect" to true,
                "options" to listOf(
                    "Brazil", "Vietnam", "Colombia", "Indonesia", "Ethiopia", "Honduras",
                    "India", "Uganda", "Mexico", "Peru", "Guatemala", "Nicaragua",
                    "China", "Ivory Coast", "Costa Rica", "Kenya", "Papua New Guinea",
                    "Tanzania", "El Salvador", "Ecuador", "Cameroon", "Laos",
                    "Madagascar", "Thailand", "Venezuela", "Burundi", "Rwanda",
                    "Democratic Republic of Congo", "Haiti", "Philippines",
                ),
            ),
            mapOf(
                "fieldKey" to "tasting_notes", "type" to "DROPDOWN", "label" to "Tasting Notes",
                "required" to true, "index" to 2, "multiSelect" to true,
                "options" to listOf(
                    "Berry", "Blueberry", "Raspberry", "Strawberry", "Citrus", "Lemon",
                    "Orange", "Grapefruit", "Lime", "Stone Fruit", "Cherry", "Peach",
                    "Plum", "Apricot", "Tropical Fruit", "Mango", "Pineapple",
                    "Passion Fruit", "Dried Fruit", "Raisin", "Fig", "Date",
                    "Jasmine", "Bergamot", "Rose", "Chamomile", "Lavender",
                    "Caramel", "Chocolate", "Milk Chocolate", "Dark Chocolate", "Cocoa",
                    "Vanilla", "Honey", "Maple Syrup", "Brown Sugar", "Molasses",
                    "Almond", "Hazelnut", "Peanut", "Walnut", "Cinnamon", "Nutmeg",
                    "Clove", "Cardamom", "Pepper", "Earthy", "Woody", "Tobacco",
                    "Herbal", "Grassy", "Damp Earth", "Caramelized", "Toasted",
                    "Smoky", "Rubbery", "Tire-like", "Sulfur", "Bright", "Sparkling",
                    "Tart", "Citric", "Malic", "Phosphoric", "Light", "Medium",
                    "Full", "Syrupy", "Creamy", "Watery", "Sugar",
                ),
            ),
            mapOf(
                "fieldKey" to "preparation_method", "type" to "DROPDOWN", "label" to "Preparation Method",
                "required" to true, "index" to 3, "multiSelect" to false,
                "options" to listOf("Washed", "Natural", "Honey", "Wet", "Anaerobic", "Pulped"),
            ),
            mapOf(
                "fieldKey" to "roaster", "type" to "DROPDOWN", "label" to "Roaster",
                "required" to true, "index" to 4, "multiSelect" to false,
                "options" to listOf("CoffeeLink", "Wogan", "Pact", "Viento", "Missing Bean", "Skylark"),
            ),
            mapOf(
                "fieldKey" to "is_decaf", "type" to "RADIO", "label" to "Caffeine",
                "required" to true, "index" to 5,
                "options" to listOf("Caffeinated", "Decaffeinated"),
            ),
            mapOf(
                "fieldKey" to "roast_date", "type" to "DATE_INPUT", "label" to "Roast Date",
                "required" to true, "index" to 6,
            ),
        ),
    )

    val temperatureLog: Map<String, Any> = mapOf(
        "title" to "Log temperature",
        "submitTarget" to "temperatureLog",
        "submitDestination" to "baby_home",
        "deletable" to true,
        "fields" to listOf(
            mapOf("fieldKey" to "date", "type" to "DATE_INPUT", "label" to "Date", "required" to true, "index" to 0),
            mapOf("fieldKey" to "time", "type" to "TIME_INPUT", "label" to "Time", "required" to true, "index" to 1),
            mapOf(
                "fieldKey" to "temperature_value", "type" to "WHEEL_INPUT", "label" to "Temperature (°C)",
                "required" to true, "index" to 2,
                "startNumber" to 350, "endNumber" to 420, "decimalPlaces" to 1, "defaultValue" to 370,
            ),
            mapOf("fieldKey" to "comment", "type" to "TEXT_INPUT", "label" to "Comment", "required" to false, "index" to 3),
        ),
    )

    val measurementLog: Map<String, Any> = mapOf(
        "title" to "Log measurement",
        "submitTarget" to "measurementLog",
        "submitDestination" to "baby_home",
        "deletable" to true,
        "fields" to listOf(
            mapOf("fieldKey" to "date", "type" to "DATE_INPUT", "label" to "Date", "required" to true, "index" to 0),
            mapOf("fieldKey" to "time", "type" to "TIME_INPUT", "label" to "Time", "required" to true, "index" to 1),
            mapOf("fieldKey" to "is_medical", "type" to "SWITCH", "label" to "Medical Recording?", "required" to false, "index" to 2, "defaultValue" to false),
            mapOf("fieldKey" to "record_height", "type" to "SWITCH", "label" to "Record Height", "required" to false, "index" to 3, "defaultValue" to false),
            mapOf(
                "fieldKey" to "height_value", "type" to "WHEEL_INPUT", "label" to "Height (cm)",
                "required" to false, "index" to 4,
                "startNumber" to 300, "endNumber" to 1200, "decimalPlaces" to 1, "defaultValue" to 500,
                "showWhen" to mapOf("fieldKey" to "record_height", "equals" to true),
            ),
            mapOf("fieldKey" to "record_weight", "type" to "SWITCH", "label" to "Record Weight", "required" to false, "index" to 5, "defaultValue" to false),
            mapOf(
                "fieldKey" to "weight_value", "type" to "WHEEL_INPUT", "label" to "Weight (kg)",
                "required" to false, "index" to 6,
                "startNumber" to 200, "endNumber" to 2000, "decimalPlaces" to 2, "defaultValue" to 350,
                "showWhen" to mapOf("fieldKey" to "record_weight", "equals" to true),
            ),
            mapOf("fieldKey" to "comment", "type" to "TEXT_INPUT", "label" to "Comment", "required" to false, "index" to 7),
        ),
    )

    val nappyLog: Map<String, Any> = mapOf(
        "title" to "Log a nappy change",
        "submitTarget" to "nappyLog",
        "submitDestination" to "baby_home",
        "deletable" to true,
        "fields" to listOf(
            mapOf(
                "fieldKey" to "date", "type" to "DATE_INPUT", "label" to "Date",
                "required" to true, "index" to 0,
            ),
            mapOf(
                "fieldKey" to "time", "type" to "TIME_INPUT", "label" to "Time",
                "required" to true, "index" to 1,
            ),
            mapOf(
                "fieldKey" to "nappy_type", "type" to "RADIO", "label" to "Type",
                "required" to true, "index" to 2,
                "options" to listOf("Wet", "Dirty", "Both"),
            ),
            mapOf(
                "fieldKey" to "comment", "type" to "TEXT_INPUT", "label" to "Comment",
                "required" to false, "index" to 3,
            ),
        ),
    )
}
