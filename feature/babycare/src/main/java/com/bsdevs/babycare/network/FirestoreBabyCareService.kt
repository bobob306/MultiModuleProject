package com.bsdevs.babycare.network

import android.util.Log
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreBabyCareService @Inject constructor(
    private val firestoreHolder: FirestoreHolder,
    private val userRepository: UserRepository,
    private val dispatchers: DispatcherProvider,
) : BabyCareFirestoreService {

    private val firestore get() = firestoreHolder.firestore

    private suspend fun getAuthorizedBabyId(id: String): String? {
        val user = userRepository.userProfile.value ?: userRepository.getUser(id)
        val authorizedIds = (user?.babyIds ?: emptyList()) + listOfNotNull(user?.babyId)

        val targetId = when {
            authorizedIds.contains(id) -> id
            id == user?.id -> authorizedIds.firstOrNull()
            else -> null
        }

        if (targetId == null) {
            Log.e("BABYCARE_SERVICE", "Access Denied. ID $id is not authorized for user ${user?.id}. Authorized Baby IDs: $authorizedIds")
        }
        return targetId
    }

    private fun getMonthsCollection(babyId: String) =
        firestore.collection("babyLogs").document(babyId).collection("months")

    private fun getMeasurementsDocument(babyId: String) =
        firestore.collection("babyLogs").document(babyId).collection("measurements").document("all_data")

    private fun getVaccinationsDocument(babyId: String) =
        firestore.collection("babyLogs").document(babyId).collection("vaccinations").document("all_data")

    override suspend fun getLatestMonthId(userId: String, forceRefresh: Boolean): String? = withContext(dispatchers.io) {
        try {
            val babyId = getAuthorizedBabyId(userId) ?: return@withContext null
            Log.d("FIREBASE_CALL", "Read Latest Month ID (Optimized Query) for Baby: $babyId (Force: $forceRefresh)")
            val source = if (forceRefresh) com.google.firebase.firestore.Source.SERVER else com.google.firebase.firestore.Source.DEFAULT
            getMonthsCollection(babyId)
                .orderBy(com.google.firebase.firestore.FieldPath.documentId(), com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get(source)
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
            val babyId = getAuthorizedBabyId(userId) ?: return@withContext null
            Log.d("FIREBASE_CALL", "Read Month ID Before (Optimized Query) for Baby: $babyId / $monthId")
            getMonthsCollection(babyId)
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
            val babyId = getAuthorizedBabyId(userId) ?: return@withContext emptyList()
            Log.d("FIREBASE_CALL", "Read Collection (All Months IDs) for Baby: $babyId")
            getMonthsCollection(babyId)
                .get()
                .await()
                .documents
                .map { it.id }
        } catch (e: Exception) {
            Log.e("BABYCARE_SERVICE", "Error fetching all month IDs", e)
            emptyList()
        }
    }

    override suspend fun fetchMonthDocument(userId: String, monthId: String, forceRefresh: Boolean): Map<String, Any?>? = withContext(dispatchers.io) {
        val babyId = getAuthorizedBabyId(userId) ?: return@withContext null
        Log.d("FIREBASE_CALL", "Read Month Doc for Baby: $babyId / $monthId (Force: $forceRefresh)")
        val source = if (forceRefresh) com.google.firebase.firestore.Source.SERVER else com.google.firebase.firestore.Source.DEFAULT
        val snapshot = getMonthsCollection(babyId).document(monthId).get(source).await()
        val data = if (snapshot.exists()) snapshot.data else null
        data?.let {
            val sizeKb = it.toString().toByteArray().size / 1024.0
            Log.d("FIREBASE_CALL", "Month Doc Size: %.2f KB".format(sizeKb))
        }
        data
    }

    override suspend fun saveEvent(userId: String, monthId: String, date: String, event: Map<String, Any?>) = withContext(dispatchers.io) {
        val babyId = getAuthorizedBabyId(userId) ?: return@withContext
        val docRef = getMonthsCollection(babyId).document(monthId)
        try {
            Log.d("FIREBASE_CALL", "Update Event for Baby: $babyId / $monthId / $date")
            docRef.update("days.$date", FieldValue.arrayUnion(event)).await()
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND) {
                Log.d("FIREBASE_CALL", "Set Initial Month Doc for Baby: $babyId / $monthId")
                docRef.set(mapOf("days" to mapOf(date to listOf(event))), SetOptions.merge()).await()
            } else throw e
        }
        Unit
    }

    override suspend fun updateEvent(userId: String, monthId: String, date: String, eventId: String, updatedEvent: Map<String, Any?>) = withContext(dispatchers.io) {
        val babyId = getAuthorizedBabyId(userId) ?: return@withContext
        val docRef = getMonthsCollection(babyId).document(monthId)
        Log.d("FIREBASE_CALL", "Transaction Update Event for Baby: $babyId / $monthId / $date / $eventId")
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
        val babyId = getAuthorizedBabyId(userId) ?: return@withContext
        val docRef = getMonthsCollection(babyId).document(monthId)
        Log.d("FIREBASE_CALL", "Transaction Delete Event for Baby: $babyId / $monthId / $date / $eventId")
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val daysMap = snapshot.get("days") as? Map<String, List<Map<String, Any?>>> ?: return@runTransaction
            val events = daysMap[date] ?: return@runTransaction
            val updatedList = events.filterNot { it["id"] == eventId }
            transaction.update(docRef, "days.$date", updatedList)
        }.await()
        Unit
    }

    override suspend fun fetchAllMeasurements(userId: String): List<Map<String, Any?>> = withContext(dispatchers.io) {
        try {
            val babyId = getAuthorizedBabyId(userId) ?: return@withContext emptyList()
            
            Log.d("FIREBASE_CALL", "Read All Measurements (Single Doc) for Baby: $babyId")
            val snapshot = getMeasurementsDocument(babyId).get().await()
            val data = if (snapshot.exists()) snapshot.data else null
            data?.let {
                val sizeKb = it.toString().toByteArray().size / 1024.0
                Log.d("FIREBASE_CALL", "Measurements Doc Size: %.2f KB".format(sizeKb))
            }
            val items = data?.get("items") as? Map<String, Map<String, Any?>> ?: emptyMap()
            items.values.toList().sortedByDescending { it["dateTimeString"] as? String ?: "" }
        } catch (e: Exception) {
            Log.e("BABYCARE_SERVICE", "Error fetching measurements", e)
            emptyList()
        }
    }

    override suspend fun saveMeasurement(userId: String, eventId: String, measurement: Map<String, Any?>) = withContext(dispatchers.io) {
        val babyId = getAuthorizedBabyId(userId) ?: return@withContext
        Log.d("FIREBASE_CALL", "Save Measurement into Single Doc for Baby: $babyId / $eventId")
        val docRef = getMeasurementsDocument(babyId)
        try {
            // Use dot notation to update a specific item in the map without overwriting other items
            docRef.update("items.$eventId", measurement).await()
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND) {
                // Document doesn't exist yet, create it with the first item
                docRef.set(mapOf("items" to mapOf(eventId to measurement))).await()
            } else {
                // If doc exists but field doesn't, merge it in
                docRef.set(mapOf("items" to mapOf(eventId to measurement)), SetOptions.merge()).await()
            }
        }
        Unit
    }

    override suspend fun updateMeasurement(userId: String, eventId: String, updatedMeasurement: Map<String, Any?>) = withContext(dispatchers.io) {
        saveMeasurement(userId, eventId, updatedMeasurement)
    }

    override suspend fun deleteMeasurement(userId: String, eventId: String) = withContext(dispatchers.io) {
        val babyId = getAuthorizedBabyId(userId) ?: return@withContext
        Log.d("FIREBASE_CALL", "Delete Measurement from Single Doc for Baby: $babyId / $eventId")
        getMeasurementsDocument(babyId).update("items.$eventId", FieldValue.delete()).await()
        Unit
    }

    override suspend fun fetchAllVaccinations(userId: String): List<Map<String, Any?>> = withContext(dispatchers.io) {
        try {
            val babyId = getAuthorizedBabyId(userId) ?: return@withContext emptyList()
            Log.d("FIREBASE_CALL", "Read All Vaccinations (Single Doc) for Baby: $babyId")
            val snapshot = getVaccinationsDocument(babyId).get().await()
            val data = if (snapshot.exists()) snapshot.data else null
            data?.let {
                val sizeKb = it.toString().toByteArray().size / 1024.0
                Log.d("FIREBASE_CALL", "Vaccinations Doc Size: %.2f KB".format(sizeKb))
            }
            val items = data?.get("items") as? Map<String, Map<String, Any?>> ?: emptyMap()
            Log.d("FIREBASE_CALL", "Fetched ${items.size} vaccinations")
            items.values.toList().sortedByDescending { it["dateTimeString"] as? String ?: "" }
        } catch (e: Exception) {
            Log.e("BABYCARE_SERVICE", "Error fetching vaccinations", e)
            emptyList()
        }
    }

    override suspend fun saveVaccination(userId: String, eventId: String, vaccination: Map<String, Any?>) = withContext(dispatchers.io) {
        val babyId = getAuthorizedBabyId(userId) ?: return@withContext
        Log.d("FIREBASE_CALL", "Save Vaccination into Single Doc for Baby: $babyId / $eventId")
        val docRef = getVaccinationsDocument(babyId)
        try {
            docRef.update("items.$eventId", vaccination).await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                docRef.set(mapOf("items" to mapOf(eventId to vaccination))).await()
            } else {
                docRef.set(mapOf("items" to mapOf(eventId to vaccination)), SetOptions.merge()).await()
            }
        }
        Unit
    }

    override suspend fun updateVaccination(userId: String, eventId: String, updatedVaccination: Map<String, Any?>) = withContext(dispatchers.io) {
        saveVaccination(userId, eventId, updatedVaccination)
    }

    override suspend fun deleteVaccination(userId: String, eventId: String) = withContext(dispatchers.io) {
        val babyId = getAuthorizedBabyId(userId) ?: return@withContext
        Log.d("FIREBASE_CALL", "Delete Vaccination from Single Doc for Baby: $babyId / $eventId")
        getVaccinationsDocument(babyId).update("items.$eventId", FieldValue.delete()).await()
        Unit
    }
}
