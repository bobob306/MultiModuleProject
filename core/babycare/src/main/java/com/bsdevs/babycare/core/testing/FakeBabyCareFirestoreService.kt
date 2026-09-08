package com.bsdevs.babycare.core.testing

import com.bsdevs.babycare.core.network.BabyCareFirestoreService

class FakeBabyCareFirestoreService : BabyCareFirestoreService {
    // We use deep mutable maps to simulate Firestore and allow updates/deletes in tests
    private val dataStore = mutableMapOf<String, MutableMap<String, MutableMap<String, Any?>>>()
    private val measurements = mutableMapOf<String, MutableMap<String, Map<String, Any?>>>()
    private val vaccinations = mutableMapOf<String, MutableMap<String, Map<String, Any?>>>()

    fun injectMonth(userId: String, monthId: String, data: Map<String, Any?>) {
        val userStore = dataStore.getOrPut(userId) { mutableMapOf() }
        userStore[monthId] = data.toMutableMap()
    }

    override suspend fun getLatestMonthId(userId: String, forceRefresh: Boolean): String? {
        return dataStore[userId]?.keys?.sortedDescending()?.firstOrNull()
    }

    override suspend fun getMonthIdBefore(userId: String, monthId: String): String? {
        return dataStore[userId]?.keys?.filter { it < monthId }?.sortedDescending()?.firstOrNull()
    }

    override suspend fun getAllMonthIds(userId: String): List<String> {
        return dataStore[userId]?.keys?.toList() ?: emptyList()
    }

    override suspend fun fetchMonthDocument(userId: String, monthId: String, forceRefresh: Boolean): Map<String, Any?>? {
        return dataStore[userId]?.get(monthId)
    }

    override suspend fun saveEvent(userId: String, monthId: String, date: String, event: Map<String, Any?>) {
        val monthDoc = dataStore.getOrPut(userId) { mutableMapOf() }.getOrPut(monthId) { mutableMapOf() }
        val days = monthDoc.getOrPut("days") { mutableMapOf<String, List<Map<String, Any?>>>() } as MutableMap<String, List<Map<String, Any?>>>
        val events = days.getOrDefault(date, emptyList())
        days[date] = events + event
    }

    override suspend fun updateEvent(userId: String, monthId: String, date: String, eventId: String, updatedEvent: Map<String, Any?>) {
        val monthDoc = dataStore[userId]?.get(monthId) ?: return
        val days = monthDoc["days"] as? MutableMap<String, List<Map<String, Any?>>> ?: return
        val events = days[date] ?: return
        days[date] = events.map { if (it["id"] == eventId) updatedEvent else it }
    }

    override suspend fun deleteEvent(userId: String, monthId: String, date: String, eventId: String) {
        val monthDoc = dataStore[userId]?.get(monthId) ?: return
        val days = monthDoc["days"] as? MutableMap<String, List<Map<String, Any?>>> ?: return
        val events = days[date] ?: return
        days[date] = events.filterNot { it["id"] == eventId }
    }

    override suspend fun fetchAllMeasurements(userId: String): List<Map<String, Any?>> {
        return measurements[userId]?.values?.toList() ?: emptyList()
    }

    override suspend fun saveMeasurement(userId: String, eventId: String, measurement: Map<String, Any?>) {
        measurements.getOrPut(userId) { mutableMapOf() }[eventId] = measurement
    }

    override suspend fun updateMeasurement(userId: String, eventId: String, updatedMeasurement: Map<String, Any?>) {
        saveMeasurement(userId, eventId, updatedMeasurement)
    }

    override suspend fun deleteMeasurement(userId: String, eventId: String) {
        measurements[userId]?.remove(eventId)
    }

    override suspend fun fetchAllVaccinations(userId: String): List<Map<String, Any?>> {
        return vaccinations[userId]?.values?.toList() ?: emptyList()
    }

    override suspend fun saveVaccination(userId: String, eventId: String, vaccination: Map<String, Any?>) {
        vaccinations.getOrPut(userId) { mutableMapOf() }[eventId] = vaccination
    }

    override suspend fun updateVaccination(userId: String, eventId: String, updatedVaccination: Map<String, Any?>) {
        saveVaccination(userId, eventId, updatedVaccination)
    }

    override suspend fun deleteVaccination(userId: String, eventId: String) {
        vaccinations[userId]?.remove(eventId)
    }
}
