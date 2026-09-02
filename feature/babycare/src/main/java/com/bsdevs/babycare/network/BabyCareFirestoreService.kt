package com.bsdevs.babycare.network

interface BabyCareFirestoreService {
    suspend fun getLatestMonthId(userId: String, forceRefresh: Boolean = false): String?
    suspend fun getMonthIdBefore(userId: String, monthId: String): String?
    suspend fun getAllMonthIds(userId: String): List<String>
    suspend fun fetchMonthDocument(userId: String, monthId: String, forceRefresh: Boolean = false): Map<String, Any?>?
    suspend fun saveEvent(userId: String, monthId: String, date: String, event: Map<String, Any?>)
    suspend fun updateEvent(userId: String, monthId: String, date: String, eventId: String, updatedEvent: Map<String, Any?>)
    suspend fun deleteEvent(userId: String, monthId: String, date: String, eventId: String)

    // Measurement-specific methods (Stored in a separate collection for growth tracking)
    suspend fun fetchAllMeasurements(userId: String): List<Map<String, Any?>>
    suspend fun saveMeasurement(userId: String, eventId: String, measurement: Map<String, Any?>)
    suspend fun updateMeasurement(userId: String, eventId: String, updatedMeasurement: Map<String, Any?>)
    suspend fun deleteMeasurement(userId: String, eventId: String)

    // Vaccination-specific methods
    suspend fun fetchAllVaccinations(userId: String): List<Map<String, Any?>>
    suspend fun saveVaccination(userId: String, eventId: String, vaccination: Map<String, Any?>)
    suspend fun updateVaccination(userId: String, eventId: String, updatedVaccination: Map<String, Any?>)
    suspend fun deleteVaccination(userId: String, eventId: String)
}
