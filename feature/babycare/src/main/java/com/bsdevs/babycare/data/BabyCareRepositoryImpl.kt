package com.bsdevs.babycare.data.repository

import android.util.Log
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.domain.RepositoryFetchResult
import com.bsdevs.babycare.network.DailyLogDto
import com.bsdevs.babycare.network.UnifiedEventDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import java.time.YearMonth

@Singleton
class BabyCareRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : BabyCareRepository {

    private val _cachedDays = MutableStateFlow<List<DailyLogDto>>(emptyList())
    override val cachedDays: StateFlow<List<DailyLogDto>> = _cachedDays.asStateFlow()

    // 📅 Tracks the current year-month anchor for pagination
    private var currentAnchorMonth: YearMonth = YearMonth.now()

    override suspend fun loadInitialData(userId: String, pageSize: Int): RepositoryFetchResult {
        // Reset anchor to the current month for the initial load
        currentAnchorMonth = YearMonth.now()

        val monthString = formatYearMonth(currentAnchorMonth) // e.g., "2026-08"
        Log.w("FIRESTORE_METRICS", "📡 Fetching initial month document: $monthString")

        // Fetch the entire month document and parse its daily map
        val monthlyDays = fetchMonthDocument(userId, monthString)
        println(monthlyDays)

        // Update cache, sorting the days descending so the newest logs appear first
        _cachedDays.value = monthlyDays.sortedByDescending { it.date }

        // Point the anchor to the previous month for the next pagination call
        currentAnchorMonth = currentAnchorMonth.minusMonths(1)

        return RepositoryFetchResult(
            nextAnchorMonth = currentAnchorMonth,
            hasMoreData = true // You can check if the previous month exists to set this accurately
        )
    }

    private suspend fun fetchMonthDocument(userId: String, monthString: String): List<DailyLogDto> {
        return try {
            // 1. Target the single monthly document directly using its ID (e.g., "2026-08")
            val documentSnapshot = firestore.collection("babyLogs")
                .document(userId)
                .collection("months")
                .document(monthString)
                .get()
                .await()

            // If the document doesn't exist yet, return an empty list safely
            if (!documentSnapshot.exists()) {
                Log.d("FIRESTORE_METRICS", "ℹ️ No document found for month: $monthString")
                return emptyList()
            }

            Log.d("FIRESTORE_METRICS", "🎉 Billed Cost = 1 document read for the entire month: $monthString!")

            // 2. Cast the root "days" field to a Map
            val daysMap = documentSnapshot.get("days") as? Map<String, List<Map<String, Any?>>> ?: emptyMap()

            // 3. Parse the map entries into your DailyLogDto objects
            daysMap.map { (dateString, eventsArray) ->
                DailyLogDto(
                    date = dateString,
                    userId = userId,
                    events = eventsArray.map { eventMap -> parseUnifiedEvent(eventMap) }
                )
            }
        } catch (e: Exception) {
            Log.e("FIRESTORE_ERROR", "❌ Failed fetching monthly data for $monthString", e)
            emptyList()
        }
    }

    private fun parseUnifiedEvent(eventMap: Map<String, Any?>): UnifiedEventDto {
        return UnifiedEventDto(
            id = eventMap["id"] as? String ?: "",
            type = eventMap["type"] as? String ?: "",
            time = eventMap["time"] as? String ?: "",
            dateTimeString = eventMap["dateTimeString"] as? String ?: "",

            // Nappy-specific fields
            nappyType = eventMap["nappyType"] as? String,

            // Feeding-specific fields
            mainFeedingSide = eventMap["mainFeedingSide"] as? String,
            leftDuration = eventMap["leftDuration"] as? Long ?: 0L,
            rightDuration = eventMap["rightDuration"] as? Long ?: 0L,
            totalDuration = eventMap["totalDuration"] as? Long ?: 0L,

            // Safe conversion from Firestore Long to your DTO's Int?
            bottleAmountMl = (eventMap["bottleAmountMl"] as? Long)?.toInt()
        )
    }


    private fun formatYearMonth(yearMonth: YearMonth): String {
        return String.format("%04d-%02d", yearMonth.year, yearMonth.monthValue)
    }

    override suspend fun refreshData(userId: String, pageSize: Int): RepositoryFetchResult {
        return loadInitialData(userId, pageSize)
    }

    override suspend fun loadMoreData(userId: String, pageSize: Int): RepositoryFetchResult {
        val monthString = formatYearMonth(currentAnchorMonth)
        Log.w("FIRESTORE_METRICS", "📡 Paginating next month document: $monthString")

        // Fetch the new month's data
        val newMonthlyDays = fetchMonthDocument(userId, monthString)

        // Append new logs to the cache and maintain a descending chronological order (newest first)
        val updatedList = _cachedDays.value + newMonthlyDays
        _cachedDays.value = updatedList.sortedByDescending { it.date }

        // Advance the tracking anchor backward by one month
        currentAnchorMonth = currentAnchorMonth.minusMonths(1)

        return RepositoryFetchResult(
            nextAnchorMonth = currentAnchorMonth,
            hasMoreData = true // Ideal optimization: verify if older months exist in Firestore
        )
    }

    override suspend fun saveActivityEvent(userId: String, date: String, event: UnifiedEventDto) {
        val db = firestore.collection("babyLogs").document(userId).collection("days").document(date)

        try {
            // 🚀 1. Try to atomically push the event into the array assuming the day doc exists
            db.update("events", FieldValue.arrayUnion(event)).await()
            Log.d("FIRESTORE_WRITE", "Successfully appended activity event to existing day document: $date")
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            // 🛑 2. If the document doesn't exist, handle the error code gracefully
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND) {
                Log.w("FIRESTORE_WRITE", "Day document $date not found. Creating a fresh one...")

                val newDayDoc = DailyLogDto(
                    date = date,
                    userId = userId,
                    events = listOf(event)
                )
                // Use set with merge options to safely initialize the document layout
                db.set(newDayDoc, SetOptions.merge()).await()
            } else {
                throw e // Re-throw any other critical network or rules permissions faults
            }
        }
    }

    override suspend fun getFeedingEventById(userId: String, activityId: String): UnifiedEventDto? {
        // 🔍 1. High Performance Look-up: Search your local in-memory cache first! Cost = 0 reads! 🎉
        val cachedEvent = _cachedDays.value
            .flatMap { it.events }
            .firstOrNull { it.id == activityId }

        if (cachedEvent != null) return cachedEvent

        // 📡 2. Cloud Server Fallback: If not in cache, query the database using the collectionGroup tool
        Log.w("FIRESTORE_METRICS", "Activity not found in cache. Querying network server...")

        val snapshot = firestore.collectionGroup("days")
            .get()
            .await()

        // Map through the day blocks to isolate the target object
        val remoteDayDocs = snapshot.toObjects(DailyLogDto::class.java)
        return remoteDayDocs
            .flatMap { it.events }
            .firstOrNull { it.id == activityId }
    }

    override suspend fun getNappyEventById(userId: String, activityId: String): UnifiedEventDto? {
        // 🔍 1. High Performance Look-up: Search local in-memory cache first! Cost = 0 reads! 🎉
        val cachedEvent = _cachedDays.value
            .flatMap { it.events }
            .firstOrNull { it.id == activityId }

        if (cachedEvent != null) return cachedEvent

        // 📡 2. Cloud Server Fallback: Query network server only if cache doesn't contain it
        Log.w("FIRESTORE_METRICS", "Nappy activity not found in cache. Querying network server...")
        val snapshot = firestore.collectionGroup("days").get().await()
        val remoteDayDocs = snapshot.toObjects(DailyLogDto::class.java)

        return remoteDayDocs
            .flatMap { it.events }
            .firstOrNull { it.id == activityId }
    }

    override suspend fun updateActivityEvent(
        userId: String,
        date: String,
        eventId: String,
        updatedEvent: UnifiedEventDto
    ) {
        val docRef = firestore.collection("babyLogs").document(userId).collection("days").document(date)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val dailyLog = snapshot.toObject(DailyLogDto::class.java) ?: return@runTransaction

            // 🔄 Map through the list, replace the old event matching the ID with the updated copy
            val updatedEventsList = dailyLog.events.map { existingEvent ->
                if (existingEvent.id == eventId) updatedEvent else existingEvent
            }

            // Write the modified array payload back to the document root safely
            transaction.update(docRef, "events", updatedEventsList)
        }.await()
        Log.d("FIRESTORE_EDIT", "Successfully updated event: $eventId inside day block doc: $date")
    }

    override suspend fun deleteActivityEvent(userId: String, date: String, eventId: String) {
        val docRef = firestore.collection("babyLogs").document(userId).collection("days").document(date)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val dailyLog = snapshot.toObject(DailyLogDto::class.java) ?: return@runTransaction

            // ✂️ Filter out the item matching your targeted delete identifier string
            val updatedEventsList = dailyLog.events.filterNot { it.id == eventId }

            transaction.update(docRef, "events", updatedEventsList)
        }.await()
        Log.d("FIRESTORE_DELETE", "Successfully deleted event: $eventId from day block doc: $date")
    }


    // --- Private Optimized Sub-Collection Fetcher ---
    private suspend fun fetchDayDocumentsBlock(
        userId: String,
        startDate: String,
        endDate: String
    ): List<DailyLogDto> {
        val snapshot = firestore.collection("babyLogs").document(userId).collection("days")
            .whereLessThanOrEqualTo("__name__", startDate) // Range-filters against the Document ID key strings directly
            .whereGreaterThanOrEqualTo("__name__", endDate)
            .orderBy("__name__", Query.Direction.DESCENDING)
            .get()
            .await()

        Log.d("FIRESTORE_METRICS", "🎉 Billed Cost = ${snapshot.size()} reads total for this 5-day history block! Max saved profile!")
        return snapshot.toObjects(DailyLogDto::class.java)
    }
}
