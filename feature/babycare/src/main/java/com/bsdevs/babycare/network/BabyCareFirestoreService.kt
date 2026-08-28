package com.bsdevs.babycare.network

interface BabyCareFirestoreService {
    suspend fun getLatestMonthId(userId: String, forceRefresh: Boolean = false): String?
    suspend fun getMonthIdBefore(userId: String, monthId: String): String?
    suspend fun getAllMonthIds(userId: String): List<String>
    suspend fun fetchMonthDocument(userId: String, monthId: String, forceRefresh: Boolean = false): Map<String, Any?>?
    suspend fun saveEvent(userId: String, monthId: String, date: String, event: Map<String, Any?>)
    suspend fun updateEvent(userId: String, monthId: String, date: String, eventId: String, updatedEvent: Map<String, Any?>)
    suspend fun deleteEvent(userId: String, monthId: String, date: String, eventId: String)
}
