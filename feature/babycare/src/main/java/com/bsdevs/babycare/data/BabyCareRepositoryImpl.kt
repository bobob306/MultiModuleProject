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
            comment = eventMap["comment"] as? String,

            // Nappy-specific fields
            nappyType = eventMap["nappyType"] as? String,

            // Feeding-specific fields
            mainFeedingSide = eventMap["mainFeedingSide"] as? String,
            leftDuration = eventMap["leftDuration"] as? Long ?: 0L,
            rightDuration = eventMap["rightDuration"] as? Long ?: 0L,
            totalDuration = eventMap["totalDuration"] as? Long ?: 0L,

            // Safe conversion from Firestore Long to your DTO's Int?
            bottleAmountMl = (eventMap["bottleAmountMl"] as? Long)?.toInt(),

            // Temperature-specific field
            temperature = eventMap["temperature"] as? Double
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

    override suspend fun deleteActivityEvent(userId: String, date: String, eventId: String) {
        val monthDocId = extractMonthString(date)
        val docRef = firestore.collection("babyLogs").document(userId).collection("months").document(monthDocId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            if (!snapshot.exists()) return@runTransaction

            val rawDaysMap = snapshot.get("days") as? Map<String, List<Map<String, Any?>>> ?: return@runTransaction
            val dayEvents = rawDaysMap[date]?.map { parseUnifiedEvent(it) } ?: emptyList()

            // ✂️ Filter out the item matching your targeted delete identifier string
            val updatedEventsList = dayEvents.filterNot { it.id == eventId }

            // 🌟 CONVERT TO MAPS: Prevents Firestore serialization crash
            val firestoreCompatibleList = updatedEventsList.map { event ->
                toFirestoreMap(event)
            }

            transaction.update(docRef, "days.$date", firestoreCompatibleList)
        }.await()
        Log.d("FIRESTORE_DELETE", "Successfully deleted event: $eventId from month doc: $monthDocId")

        // 🌟 FORCE UI REFRESH: Remove the entry from the local cache stream
        updateLocalCacheWithDeletedEvent(date, eventId)
    }

    private fun toFirestoreMap(event: UnifiedEventDto): Map<String, Any?> {
        return mapOf(
            "id" to event.id,
            "type" to event.type,
            "time" to event.time,
            "dateTimeString" to event.dateTimeString,
            "comment" to event.comment,
            "nappyType" to event.nappyType,
            "mainFeedingSide" to event.mainFeedingSide,
            "leftDuration" to event.leftDuration,
            "rightDuration" to event.rightDuration,
            "totalDuration" to event.totalDuration,
            "bottleAmountMl" to event.bottleAmountMl,
            "temperature" to event.temperature
        )
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

    private fun extractMonthString(dateString: String): String {
        return try {
            val parts = dateString.split("-")
            "${parts[0]}-${parts[1]}"
        } catch (e: Exception) {
            ""
        }
    }

    override suspend fun saveActivityEvent(userId: String, date: String, event: UnifiedEventDto) {
        val monthDocId = extractMonthString(date)
        val db = firestore.collection("babyLogs").document(userId).collection("months").document(monthDocId)

        try {
            db.update("days.$date", FieldValue.arrayUnion(toFirestoreMap(event))).await()
            Log.d("FIRESTORE_WRITE", "Successfully saved event to network.")

            // 🌟 FORCE UI REFRESH: Insert the new event into the local cache stream
            updateLocalCacheWithNewEvent(date, userId, event)

        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND) {
                val initialPayload = mapOf("days" to mapOf(date to listOf(toFirestoreMap(event))))
                db.set(initialPayload, SetOptions.merge()).await()

                // 🌟 FORCE UI REFRESH: Insert the new event into the local cache stream
                updateLocalCacheWithNewEvent(date, userId, event)
            } else {
                throw e
            }
        }
    }

    override suspend fun getFeedingEventById(userId: String, activityId: String): UnifiedEventDto? {
        // 🔍 1. In-Memory Cache Lookup (Cost = 0 reads)
        val cachedEvent = _cachedDays.value
            .flatMap { it.events }
            .firstOrNull { it.id == activityId }

        if (cachedEvent != null) return cachedEvent

        // 📡 2. Targeted Cloud Server Fallback
        Log.w("FIRESTORE_METRICS", "Activity not found in cache. Querying user months...")
        return try {
            val snapshot = firestore.collection("babyLogs")
                .document(userId)
                .collection("months")
                .get()
                .await()

            // Loop through user's monthly documents to find the matching event ID
            snapshot.documents.flatMap { doc ->
                val daysMap = doc.get("days") as? Map<String, List<Map<String, Any?>>> ?: emptyMap()
                daysMap.values.flatten().map { parseUnifiedEvent(it) }
            }.firstOrNull { it.id == activityId }
        } catch (e: Exception) {
            Log.e("FIRESTORE_ERROR", "Failed to retrieve event by ID via network", e)
            null
        }
    }

    override suspend fun getNappyEventById(userId: String, activityId: String): UnifiedEventDto? {
        // Both types map to unified fields, so we can route cleanly into the same lookup engine
        return getFeedingEventById(userId, activityId)
    }

    override suspend fun updateActivityEvent(
        userId: String,
        date: String,
        eventId: String,
        updatedEvent: UnifiedEventDto
    ) {
        val monthDocId = extractMonthString(date)
        val docRef = firestore.collection("babyLogs").document(userId).collection("months").document(monthDocId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            if (!snapshot.exists()) return@runTransaction

            val rawDaysMap = snapshot.get("days") as? Map<String, List<Map<String, Any?>>> ?: return@runTransaction
            val dayEvents = rawDaysMap[date]?.map { parseUnifiedEvent(it) } ?: emptyList()

            val updatedEventsList = dayEvents.map { existingEvent ->
                if (existingEvent.id == eventId) updatedEvent else existingEvent
            }

            // 🌟 CONVERT TO MAPS: Prevents Firestore serialization crash and updates the comment key
            val firestoreCompatibleList = updatedEventsList.map { event ->
                toFirestoreMap(event)
            }

            transaction.update(docRef, "days.$date", firestoreCompatibleList)
        }.await()
        Log.d("FIRESTORE_EDIT", "Successfully updated event on network.")

        // 🌟 FORCE UI REFRESH: Modify the entry inside the local cache stream
        updateLocalCacheWithModifiedEvent(date, eventId, updatedEvent)
    }

    private fun updateLocalCacheWithNewEvent(date: String, userId: String, event: UnifiedEventDto) {
        val currentList = _cachedDays.value.toMutableList()
        val existingDayIndex = currentList.indexOfFirst { it.date == date }

        if (existingDayIndex != -1) {
            // Day already exists in cache, append the event to its array
            val targetDay = currentList[existingDayIndex]
            currentList[existingDayIndex] = targetDay.copy(
                events = targetDay.events + event
            )
        } else {
            // Day doesn't exist in memory yet, initialize a brand new DailyLogDto block
            currentList.add(
                DailyLogDto(date = date, userId = userId, events = listOf(event))
            )
        }

        // Re-sort descending so the UI remains chronological and push the state
        _cachedDays.value = currentList.sortedByDescending { it.date }
    }

    private fun updateLocalCacheWithModifiedEvent(date: String, eventId: String, updatedEvent: UnifiedEventDto) {
        val currentList = _cachedDays.value.toMutableList()
        val existingDayIndex = currentList.indexOfFirst { it.date == date }

        if (existingDayIndex != -1) {
            val targetDay = currentList[existingDayIndex]
            val updatedEvents = targetDay.events.map { existingEvent ->
                if (existingEvent.id == eventId) updatedEvent else existingEvent
            }
            currentList[existingDayIndex] = targetDay.copy(events = updatedEvents)

            // Push the update to emit to all active screen viewers instantly
            _cachedDays.value = currentList
        }
    }

    private fun updateLocalCacheWithDeletedEvent(date: String, eventId: String) {
        val currentList = _cachedDays.value.toMutableList()
        val existingDayIndex = currentList.indexOfFirst { it.date == date }

        if (existingDayIndex != -1) {
            val targetDay = currentList[existingDayIndex]
            val updatedEvents = targetDay.events.filterNot { it.id == eventId }
            currentList[existingDayIndex] = targetDay.copy(events = updatedEvents)

            // Push the update to emit to all active screen viewers instantly
            _cachedDays.value = currentList
        }
    }
}
