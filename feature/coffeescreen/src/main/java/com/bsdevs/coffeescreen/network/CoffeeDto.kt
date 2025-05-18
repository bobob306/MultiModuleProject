package com.bsdevs.coffeescreen.network

data class CoffeeDto(
    val roastDate: String,
    val beanTypes: List<String>,
    val originCountries: List<String>,
    val tastingNotes: List<String>,
    val beanPreparationMethod: List<String>,
    val roaster: String,
    val isDecaf: Boolean,
    val label: String,
)
