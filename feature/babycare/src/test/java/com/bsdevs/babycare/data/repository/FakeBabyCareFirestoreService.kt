package com.bsdevs.babycare.data.repository

import com.bsdevs.babycare.network.BabyCareFirestoreService

class FakeBabyCareFirestoreService : BabyCareFirestoreService {
    
    // Map<UserId, Map<MonthId, Map<String, Any>>>
    private val database = mutableMapOf<String, MutableMap<String, Map<String, Any>>>()

    fun injectMonth(userId: String, monthId: String, data: Map<String, Any>) {
        database.getOrPut(userId) { mutableMapOf() }[monthId] = data
    }

    override suspend fun getLatestMonthId(userId: String): String? {
        return database[userId]?.keys?.sortedDescending()?.firstOrNull()
    }

    override suspend fun getMonthIdBefore(userId: String, monthId: String): String? {
        return database[userId]?.keys?.filter { it < monthId }?.sortedDescending()?.firstOrNull()
    }

    override suspend fun fetchMonthDocument(userId: String, monthId: String): Map<String, Any>? {
        return database[userId]?.get(monthId)
    }

    override suspend fun saveEvent(userId: String, monthId: String, date: String, event: Map<String, Any?>) {
        val userDb = database.getOrPut(userId) { mutableMapOf() }
        val monthData = userDb.getOrDefault(monthId, mapOf("days" to mutableMapOf<String, List<Map<String, Any?>>>())).toMutableMap()
        val days = (monthData["days"] as? Map<String, List<Map<String, Any?>>> ?: emptyMap()).toMutableMap()
        val events = (days[date] ?: emptyList()) + event
        days[date] = events
        monthData["days"] = days
        userDb[monthId] = monthData as Map<String, Any>
    }

    override suspend fun updateEvent(userId: String, monthId: String, date: String, eventId: String, updatedEvent: Map<String, Any?>) {
        val userDb = database[userId] ?: return
        val monthData = userDb[monthId]?.toMutableMap() ?: return
        val days = (monthData["days"] as? Map<String, List<Map<String, Any?>>> ?: return).toMutableMap()
        val events = days[date] ?: return
        days[date] = events.map { if (it["id"] == eventId) updatedEvent else it }
        monthData["days"] = days
        userDb[monthId] = monthData as Map<String, Any>
    }

    override suspend fun deleteEvent(userId: String, monthId: String, date: String, eventId: String) {
        val userDb = database[userId] ?: return
        val monthData = userDb[monthId]?.toMutableMap() ?: return
        val days = (monthData["days"] as? Map<String, List<Map<String, Any?>>> ?: return).toMutableMap()
        val events = days[date] ?: return
        days[date] = events.filterNot { it["id"] == eventId }
        monthData["days"] = days
        userDb[monthId] = monthData as Map<String, Any>
    }
}
