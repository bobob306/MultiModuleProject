package com.bsdevs.forms.impl

import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.coffeescreen.data.CoffeeRepository
import com.bsdevs.network.repository.FormPrefiller
import javax.inject.Inject

class FormPrefillerImpl @Inject constructor(
    private val coffeeRepository: CoffeeRepository,
    private val babyCareRepository: BabyCareRepository,
) : FormPrefiller {

    override suspend fun loadExistingValues(
        userId: String,
        target: String,
        entityId: String,
    ): Map<String, Any>? = when (target) {
        "coffeeLog" -> loadCoffeeValues(userId, entityId)
        "nappyLog" -> loadNappyValues(userId, entityId)
        "feedingLog" -> loadFeedingValues(userId, entityId)
        "temperatureLog" -> loadTemperatureValues(userId, entityId)
        "measurementLog" -> loadMeasurementValues(userId, entityId)
        "vaccinationLog" -> loadVaccinationValues(userId, entityId)
        else -> null
    }

    private suspend fun loadCoffeeValues(userId: String, entityId: String): Map<String, Any>? {
        val coffee = coffeeRepository.allCoffee.value.firstOrNull { it.id == entityId }
            ?: coffeeRepository.getCoffeeById(userId, entityId)
            ?: return null
        return buildMap {
            coffee.beanTypes?.let { put("bean_types", it) }
            coffee.originCountries?.let { put("origin_countries", it) }
            coffee.tastingNotes?.let { put("tasting_notes", it) }
            coffee.beanPreparationMethod?.let { if (it.isNotEmpty()) put("preparation_method", it) }
            coffee.roaster?.let { put("roaster", it) }
            coffee.roastDate?.let { put("roast_date", it) }
            put("is_decaf", if (coffee.isDecaf == true) "Decaffeinated" else "Caffeinated")
        }
    }

    private suspend fun loadNappyValues(userId: String, entityId: String): Map<String, Any>? {
        val event = babyCareRepository.getNappyEventById(userId, entityId) ?: return null
        return buildMap {
            put("time", event.time)
            event.nappyType?.let { put("nappy_type", it) }
            event.comment?.let { put("comment", it) }
            put("date", event.dateTimeString.substringBefore(" "))
        }
    }

    private suspend fun loadTemperatureValues(userId: String, entityId: String): Map<String, Any>? {
        val event = babyCareRepository.getTemperatureEventById(userId, entityId)
            ?.takeIf { it.type == "TEMPERATURE" } ?: return null
        return buildMap {
            put("date", event.dateTimeString.substringBefore(" "))
            put("time", event.time)
            event.temperature?.let { put("temperature_value", (it * 10).toInt()) }
            event.comment?.let { put("comment", it) }
        }
    }

    private suspend fun loadMeasurementValues(userId: String, entityId: String): Map<String, Any>? {
        val event = babyCareRepository.getMeasurementEventById(userId, entityId)
            ?.takeIf { it.type == "MEASUREMENT" } ?: return null
        return buildMap {
            put("date", event.dateTimeString.substringBefore(" "))
            put("time", event.time)
            put("is_medical", event.isMedical ?: false)
            event.height?.let {
                put("record_height", true)
                put("height_value", (it * 10).toInt())
            }
            event.weight?.let {
                put("record_weight", true)
                put("weight_value", (it * 100).toInt())
            }
            event.headCircumference?.let {
                put("record_head_circumference", true)
                put("head_circumference_value", (it * 10).toInt())
            }
            event.comment?.let { put("comment", it) }
        }
    }

    private suspend fun loadFeedingValues(userId: String, entityId: String): Map<String, Any>? {
        val event = babyCareRepository.getFeedingEventById(userId, entityId) ?: return null
        return buildMap {
            put("start_time", event.time)
            event.mainFeedingSide?.let { put("feeding_side", it) }
            event.bottleAmountMl?.let { put("bottle_amount_ml", it.toString()) }
            event.comment?.let { put("comment", it) }
            put("date", event.dateTimeString.substringBefore(" "))
        }
    }

    private suspend fun loadVaccinationValues(userId: String, entityId: String): Map<String, Any>? {
        val event = babyCareRepository.getVaccinationEventById(userId, entityId)
            ?.takeIf { it.type == "VACCINATION" } ?: return null
        return buildMap {
            put("date", event.dateTimeString.substringBefore(" "))
            put("time", event.time)
            event.vaccinationNames?.let { put("vaccination_names", it) }
            event.location?.let { put("location", it) }
            event.seriesId?.let { put("series_id", it) }
            event.comment?.let { put("comment", it) }
        }
    }
}
