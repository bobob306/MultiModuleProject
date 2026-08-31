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
        val tempInt = (values["temperature_value"] as? Number)?.toInt()
            ?: return Result.Error(IllegalArgumentException("temperatureLog requires 'temperature_value' field"))
        val event = UnifiedEventDto(
            id = entityId ?: UUID.randomUUID().toString(),
            type = "TEMPERATURE",
            time = values["time"] as? String ?: "",
            dateTimeString = "$date ${values["time"] ?: "00:00"}",
            temperature = tempInt.toDouble() / 10.0,
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
        val heightInt = (values["height_value"] as? Number)?.toInt()
        val weightInt = (values["weight_value"] as? Number)?.toInt()
        if (heightInt == null && weightInt == null) {
            return Result.Error(IllegalArgumentException("Please record at least height or weight"))
        }
        val event = UnifiedEventDto(
            id = entityId ?: UUID.randomUUID().toString(),
            type = "MEASUREMENT",
            time = values["time"] as? String ?: "",
            dateTimeString = "$date ${values["time"] ?: "00:00"}",
            height = heightInt?.toDouble()?.div(10.0),
            weight = weightInt?.toDouble()?.div(100.0),
            isMedical = values["is_medical"] as? Boolean ?: false,
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
        val event = UnifiedEventDto(
            id = entityId ?: UUID.randomUUID().toString(),
            type = "FEEDING",
            time = values["start_time"] as? String ?: "",
            dateTimeString = "$date ${values["start_time"] ?: "00:00"}",
            mainFeedingSide = values["feeding_side"] as? String,
            bottleAmountMl = (values["bottle_amount_ml"] as? String)?.toIntOrNull(),
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
