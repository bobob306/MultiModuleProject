package com.bsdevs.multimoduleproject.forms

import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.common.result.Result
import com.bsdevs.network.repository.FormDeleter
import javax.inject.Inject

class FormDeleterImpl @Inject constructor(
    private val babyCareRepository: BabyCareRepository,
) : FormDeleter {

    override suspend fun delete(userId: String, target: String, entityId: String): Result<Unit> = when (target) {
        "nappyLog" -> deleteNappy(userId, entityId)
        "feedingLog" -> deleteFeeding(userId, entityId)
        "temperatureLog" -> deleteTemperature(userId, entityId)
        "measurementLog" -> deleteMeasurement(userId, entityId)
        else -> Result.Error(UnsupportedOperationException("Delete not supported for target: $target"))
    }

    private suspend fun deleteNappy(userId: String, entityId: String): Result<Unit> = try {
        val event = babyCareRepository.getNappyEventById(userId, entityId)
            ?: return Result.Error(Exception("Nappy record not found"))
        val date = event.dateTimeString.substringBefore("T")
        babyCareRepository.deleteActivityEvent(userId, date, entityId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    private suspend fun deleteTemperature(userId: String, entityId: String): Result<Unit> = try {
        val event = babyCareRepository.getFeedingEventById(userId, entityId)
            ?: return Result.Error(Exception("Temperature record not found"))
        val date = event.dateTimeString.substringBefore(" ")
        babyCareRepository.deleteActivityEvent(userId, date, entityId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    private suspend fun deleteMeasurement(userId: String, entityId: String): Result<Unit> = try {
        val event = babyCareRepository.getMeasurementEventById(userId, entityId)
            ?: return Result.Error(Exception("Measurement record not found"))
        val date = event.dateTimeString.substringBefore(" ")
        babyCareRepository.deleteActivityEvent(userId, date, entityId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    private suspend fun deleteFeeding(userId: String, entityId: String): Result<Unit> = try {
        val event = babyCareRepository.getFeedingEventById(userId, entityId)
            ?: return Result.Error(Exception("Feeding record not found"))
        val date = event.dateTimeString.substringBefore("T")
        babyCareRepository.deleteActivityEvent(userId, date, entityId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
