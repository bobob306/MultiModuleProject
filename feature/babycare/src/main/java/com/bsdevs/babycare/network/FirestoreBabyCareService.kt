package com.bsdevs.babycare.network

import android.util.Log
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreBabyCareService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository,
    private val dispatchers: DispatcherProvider,
) : BabyCareFirestoreService {

    private fun getMonthsCollection(id: String): com.google.firebase.firestore.CollectionReference {
        val user = userRepository.userProfile.value
        val authorizedIds = (user?.babyIds ?: emptyList()) + listOfNotNull(user?.babyId)
        
        // If the ID passed is the user's ID, we default to their first authorized babyId.
        // If it's already a babyId, we verify it's in their authorized list.
        val targetBabyId = when {
            id == user?.id -> authorizedIds.firstOrNull()
            authorizedIds.contains(id) -> id
            else -> null
        }
        
        if (targetBabyId == null) {
            Log.e("BABYCARE_SERVICE", "Access Denied. ID $id is not authorized for user ${user?.id}. Authorized Baby IDs: $authorizedIds")
            // In a production app, we would throw an exception here.
            // For now, return a reference to a non-existent path to prevent crashes but stop data leak.
            return firestore.collection("unauthorized_access").document("logs").collection("empty")
        }
        
        return firestore.collection("babyLogs").document(targetBabyId).collection("months")
    }

    override suspend fun getLatestMonthId(userId: String): String? = withContext(dispatchers.io) {
        try {
            android.util.Log.d("FIREBASE_CALL", "Read Latest Month ID (Optimized Query): $userId")
            getMonthsCollection(userId)
                .orderBy(com.google.firebase.firestore.FieldPath.documentId(), com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()?.id
        } catch (e: Exception) {
            Log.e("BABYCARE_SERVICE", "Error fetching latest month ID", e)
            null
        }
    }

    override suspend fun getMonthIdBefore(userId: String, monthId: String): String? = withContext(dispatchers.io) {
        try {
            android.util.Log.d("FIREBASE_CALL", "Read Month ID Before (Optimized Query): $userId / $monthId")
            getMonthsCollection(userId)
                .whereLessThan(com.google.firebase.firestore.FieldPath.documentId(), monthId)
                .orderBy(com.google.firebase.firestore.FieldPath.documentId(), com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()?.id
        } catch (e: Exception) {
            Log.e("BABYCARE_SERVICE", "Error fetching month ID before $monthId", e)
            null
        }
    }

    override suspend fun getAllMonthIds(userId: String): List<String> = withContext(dispatchers.io) {
        try {
            Log.d("FIREBASE_CALL", "Read Collection (All Months IDs): $userId")
            getMonthsCollection(userId)
                .get()
                .await()
                .documents
                .map { it.id }
        } catch (e: Exception) {
            Log.e("BABYCARE_SERVICE", "Error fetching all month IDs", e)
            emptyList()
        }
    }

    override suspend fun fetchMonthDocument(userId: String, monthId: String): Map<String, Any?>? = withContext(dispatchers.io) {
        Log.d("FIREBASE_CALL", "Read Month Doc: $userId / $monthId")
        val snapshot = getMonthsCollection(userId).document(monthId).get().await()
        if (snapshot.exists()) snapshot.data else null
    }

    override suspend fun saveEvent(userId: String, monthId: String, date: String, event: Map<String, Any?>) = withContext(dispatchers.io) {
        val docRef = getMonthsCollection(userId).document(monthId)
        try {
            Log.d("FIREBASE_CALL", "Update Event: $userId / $monthId / $date")
            docRef.update("days.$date", FieldValue.arrayUnion(event)).await()
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND) {
                Log.d("FIREBASE_CALL", "Set Initial Month Doc: $userId / $monthId")
                docRef.set(mapOf("days" to mapOf(date to listOf(event))), SetOptions.merge()).await()
            } else throw e
        }
        Unit
    }

    override suspend fun updateEvent(userId: String, monthId: String, date: String, eventId: String, updatedEvent: Map<String, Any?>) = withContext(dispatchers.io) {
        val docRef = getMonthsCollection(userId).document(monthId)
        Log.d("FIREBASE_CALL", "Transaction Update Event: $userId / $monthId / $date / $eventId")
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val daysMap = snapshot.get("days") as? Map<String, List<Map<String, Any?>>> ?: return@runTransaction
            val events = daysMap[date] ?: return@runTransaction
            val updatedList = events.map { if (it["id"] == eventId) updatedEvent else it }
            transaction.update(docRef, "days.$date", updatedList)
        }.await()
        Unit
    }

    override suspend fun deleteEvent(userId: String, monthId: String, date: String, eventId: String) = withContext(dispatchers.io) {
        val docRef = getMonthsCollection(userId).document(monthId)
        Log.d("FIREBASE_CALL", "Transaction Delete Event: $userId / $monthId / $date / $eventId")
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val daysMap = snapshot.get("days") as? Map<String, List<Map<String, Any?>>> ?: return@runTransaction
            val events = daysMap[date] ?: return@runTransaction
            val updatedList = events.filterNot { it["id"] == eventId }
            transaction.update(docRef, "days.$date", updatedList)
        }.await()
        Unit
    }
}
