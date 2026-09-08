package com.bsdevs.forms.impl

import com.bsdevs.babycare.core.domain.BabyCareRepository
import com.bsdevs.common.result.Result
import com.bsdevs.data.repository.FormDeleter
import com.bsdevs.network.dto.UnifiedEventDto
import javax.inject.Inject

class FormDeleterImpl @Inject constructor(
    private val babyCareRepository: BabyCareRepository,
) : FormDeleter {

    override suspend fun delete(userId: String, target: String, entityId: String): Result<Unit> = when (target) {
        "nappyLog" -> performDelete(userId, entityId, "Nappy") { babyCareRepository.getNappyEventById(it, entityId) }
        "feedingLog" -> performDelete(userId, entityId, "Feeding") { babyCareRepository.getFeedingEventById(it, entityId) }
        "temperatureLog" -> performDelete(userId, entityId, "Temperature") { babyCareRepository.getTemperatureEventById(it, entityId) }
        "measurementLog" -> performDelete(userId, entityId, "Measurement") { babyCareRepository.getMeasurementEventById(it, entityId) }
        "vaccinationLog" -> performDelete(userId, entityId, "Vaccination") { babyCareRepository.getVaccinationEventById(it, entityId) }
        else -> Result.Error(UnsupportedOperationException("Delete not supported for target: $target"))
    }

    private suspend fun performDelete(
        userId: String,
        entityId: String,
        label: String,
        fetcher: suspend (String) -> UnifiedEventDto?
    ): Result<Unit> = try {
        fetcher(userId)?.let { event ->
            val date = event.dateTimeString.substringBefore("T").substringBefore(" ")
            babyCareRepository.deleteActivityEvent(userId, date, entityId)
            Result.Success(Unit)
        } ?: Result.Error(Exception("$label record not found"))
    } catch (e: Exception) {
        Result.Error(e)
    }
}
