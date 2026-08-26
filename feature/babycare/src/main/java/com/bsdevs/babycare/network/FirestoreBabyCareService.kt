package com.bsdevs.babycare.network

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreBabyCareService @Inject constructor(
    private val firestore: FirebaseFirestore
) : BabyCareFirestoreService {

    private fun getMonthsCollection(userId: String) = 
        firestore.collection("babyLogs").document(userId).collection("months")

    override suspend fun getLatestMonthId(userId: String): String? {
        return getMonthsCollection(userId)
            .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .documents.firstOrNull()?.id
    }

    override suspend fun getMonthIdBefore(userId: String, monthId: String): String? {
        return getMonthsCollection(userId)
            .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
            .whereLessThan(FieldPath.documentId(), monthId)
            .limit(1)
            .get()
            .await()
            .documents.firstOrNull()?.id
    }

    override suspend fun getAllMonthIds(userId: String): List<String> {
        return getMonthsCollection(userId)
            .get()
            .await()
            .documents.map { it.id }
    }

    override suspend fun fetchMonthDocument(userId: String, monthId: String): Map<String, Any?>? {
        val snapshot = getMonthsCollection(userId).document(monthId).get().await()
        return if (snapshot.exists()) snapshot.data else null
    }

    override suspend fun saveEvent(userId: String, monthId: String, date: String, event: Map<String, Any?>) {
        val docRef = getMonthsCollection(userId).document(monthId)
        try {
            docRef.update("days.$date", FieldValue.arrayUnion(event)).await()
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND) {
                docRef.set(mapOf("days" to mapOf(date to listOf(event))), SetOptions.merge()).await()
            } else throw e
        }
    }

    override suspend fun updateEvent(userId: String, monthId: String, date: String, eventId: String, updatedEvent: Map<String, Any?>) {
        val docRef = getMonthsCollection(userId).document(monthId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val daysMap = snapshot.get("days") as? Map<String, List<Map<String, Any?>>> ?: return@runTransaction
            val events = daysMap[date] ?: return@runTransaction
            val updatedList = events.map { if (it["id"] == eventId) updatedEvent else it }
            transaction.update(docRef, "days.$date", updatedList)
        }.await()
    }

    override suspend fun deleteEvent(userId: String, monthId: String, date: String, eventId: String) {
        val docRef = getMonthsCollection(userId).document(monthId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val daysMap = snapshot.get("days") as? Map<String, List<Map<String, Any?>>> ?: return@runTransaction
            val events = daysMap[date] ?: return@runTransaction
            val updatedList = events.filterNot { it["id"] == eventId }
            transaction.update(docRef, "days.$date", updatedList)
        }.await()
    }
}
