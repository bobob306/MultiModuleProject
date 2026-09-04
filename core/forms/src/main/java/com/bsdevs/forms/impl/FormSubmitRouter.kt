package com.bsdevs.forms.impl

import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.coffeescreen.data.CoffeeRepository
import com.bsdevs.coffeescreen.network.CoffeeDto
import com.bsdevs.common.result.Result
import com.bsdevs.network.repository.FormSubmitter
import java.util.UUID
import javax.inject.Inject

class FormSubmitRouter @Inject constructor(
    private val coffeeRepository: CoffeeRepository,
    private val babyCareRepository: BabyCareRepository,
) : FormSubmitter {
    override suspend fun submit(userId: String, target: String, entityId: String?, values: Map<String, Any>): Result<Unit> = when (target) {
        "coffeeLog" -> submitCoffee(userId, entityId, values)
        "nappyLog" -> submitNappy(userId, entityId, values)
        "feedingLog" -> submitFeeding(userId, entityId, values)
        "temperatureLog" -> submitTemperature(userId, entityId, values)
        "measurementLog" -> submitMeasurement(userId, entityId, values)
        "vaccinationLog" -> submitVaccination(userId, entityId, values)
        else -> Result.Error(IllegalArgumentException("Unknown submit target: $target"))
    }

    private suspend fun submitCoffee(userId: String, entityId: String?, values: Map<String, Any>): Result<Unit> = try {
        val roaster = values["roaster"] as? String ?: ""
        val origins = (values["origin_countries"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val method = (values["preparation_method"] as? List<*>)?.filterIsInstance<String>()?.firstOrNull() ?: ""
        val roastDate = values["roast_date"] as? String ?: ""
        val label = "$roaster ${origins.joinToString(", ")} $method $roastDate".trim()

        coffeeRepository.uploadCoffee(
            userId = userId,
            coffee = CoffeeDto(
                userId = userId,
                roastDate = roastDate,
                roaster = roaster,
                label = label,
                isDecaf = values["is_decaf"] == "Decaffeinated",
                beanTypes = (values["bean_types"] as? List<*>)?.filterIsInstance<String>(),
                originCountries = origins,
                tastingNotes = (values["tasting_notes"] as? List<*>)?.filterIsInstance<String>(),
                beanPreparationMethod = listOfNotNull(method).ifEmpty { null },
                id = entityId ?: UUID.randomUUID().toString(),
            )
        )
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    private suspend fun submitNappy(userId: String, entityId: String?, values: Map<String, Any>): Result<Unit> = try {
        val date = values["date"] as? String
            ?: return Result.Error(IllegalArgumentException("nappyLog requires 'date' field"))
        val event = UnifiedEventDto(
            id = entityId ?: UUID.randomUUID().toString(),
            type = "NAPPY",
            time = values["time"] as? String ?: "",
            dateTimeString = "$date ${values["time"] ?: "00:00"}",
            nappyType = values["nappy_type"] as? String,
            comment = values["comment"] as? String,
        )
        if (entityId != null) {
            babyCareRepository.updateActivityEvent(userId, date, entityId, event)
        } else {
            babyCareRepository.saveActivityEvent(userId, date, event)
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    private suspend fun submitTemperature(userId: String, entityId: String?, values: Map<String, Any>): Result<Unit> = try {
        val date = values["date"] as? String
            ?: return Result.Error(IllegalArgumentException("temperatureLog requires 'date' field"))
        val tNum = values["temperature_value"] as? Number
            ?: return Result.Error(IllegalArgumentException("temperatureLog requires 'temperature_value' field"))
        val event = UnifiedEventDto(
            id = entityId ?: UUID.randomUUID().toString(),
            type = "TEMPERATURE",
            time = values["time"] as? String ?: "",
            dateTimeString = "$date ${values["time"] ?: "00:00"}",
            temperature = tNum.toDouble() / 10.0,
            comment = values["comment"] as? String,
        )
        if (entityId != null) {
            babyCareRepository.updateActivityEvent(userId, date, entityId, event)
        } else {
            babyCareRepository.saveActivityEvent(userId, date, event)
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    private suspend fun submitMeasurement(userId: String, entityId: String?, values: Map<String, Any>): Result<Unit> = try {
        val date = values["date"] as? String
            ?: return Result.Error(IllegalArgumentException("measurementLog requires 'date' field"))
        
        val recordHeight = values["record_height"] == true
        val recordWeight = values["record_weight"] == true
        val recordHead = values["record_head_circumference"] == true
        
        if (!recordHeight && !recordWeight && !recordHead) {
            return Result.Error(IllegalArgumentException("Please record at least height, weight or head circumference"))
        }

        val hNum = values["height_value"] as? Number
        val wNum = values["weight_value"] as? Number
        val hcNum = values["head_circumference_value"] as? Number

        val event = UnifiedEventDto(
            id = entityId ?: UUID.randomUUID().toString(),
            type = "MEASUREMENT",
            time = values["time"] as? String ?: "",
            dateTimeString = "$date ${values["time"] ?: "00:00"}",
            height = if (recordHeight) (hNum?.toDouble()?.div(10.0) ?: 50.0) else null,
            weight = if (recordWeight) (wNum?.toDouble()?.div(100.0) ?: 3.5) else null,
            headCircumference = if (recordHead) (hcNum?.toDouble()?.div(10.0) ?: 40.0) else null,
            isMedical = values["is_medical"] == true,
            comment = values["comment"] as? String,
        )
        if (entityId != null) {
            babyCareRepository.updateActivityEvent(userId, date, entityId, event)
        } else {
            babyCareRepository.saveActivityEvent(userId, date, event)
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    private suspend fun submitFeeding(userId: String, entityId: String?, values: Map<String, Any>): Result<Unit> = try {
        val date = values["date"] as? String
            ?: return Result.Error(IllegalArgumentException("feedingLog requires 'date' field"))
        
        val bottleStr = values["bottle_amount_ml"] as? String
        
        val event = UnifiedEventDto(
            id = entityId ?: UUID.randomUUID().toString(),
            type = "FEEDING",
            time = values["start_time"] as? String ?: "",
            dateTimeString = "$date ${values["start_time"] ?: "00:00"}",
            mainFeedingSide = values["feeding_side"] as? String,
            bottleAmountMl = bottleStr?.toIntOrNull(),
            comment = values["comment"] as? String,
        )
        if (entityId != null) {
            babyCareRepository.updateActivityEvent(userId, date, entityId, event)
        } else {
            babyCareRepository.saveActivityEvent(userId, date, event)
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    private suspend fun submitVaccination(userId: String, entityId: String?, values: Map<String, Any>): Result<Unit> = try {
        val date = values["date"] as? String
            ?: return Result.Error(IllegalArgumentException("vaccinationLog requires 'date' field"))
        val names = (values["vaccination_names"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val sIdRaw = values["series_id"] as? String
        val seriesId = if (!sIdRaw.isNullOrBlank()) sIdRaw
            else names.firstOrNull()?.replace(Regex("[^a-zA-Z0-9]"), "_")?.lowercase()
            ?: UUID.randomUUID().toString()

        val event = UnifiedEventDto(
            id = entityId ?: UUID.randomUUID().toString(),
            type = "VACCINATION",
            time = values["time"] as? String ?: "",
            dateTimeString = "$date ${values["time"] ?: "00:00"}",
            vaccinationNames = names,
            location = values["location"] as? String,
            seriesId = seriesId,
            comment = values["comment"] as? String,
        )
        if (entityId != null) {
            babyCareRepository.updateActivityEvent(userId, date, entityId, event)
        } else {
            babyCareRepository.saveActivityEvent(userId, date, event)
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
