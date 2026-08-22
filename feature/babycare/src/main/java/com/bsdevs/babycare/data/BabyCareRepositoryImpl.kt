package com.bsdevs.babycare.data.repository

import android.util.Log
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.domain.RepositoryFetchResult
import com.bsdevs.babycare.network.DailyLogDto
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.SizeData
import com.bsdevs.data.SpacerTypeData
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
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions

@Singleton
class BabyCareRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : BabyCareRepository {

    // 🌟 Expose a single StateFlow stream of your pre-grouped daily objects!
    private val _cachedDays = MutableStateFlow<List<DailyLogDto>>(emptyList())
    override val cachedDays: StateFlow<List<DailyLogDto>> = _cachedDays.asStateFlow()

    // 📅 Tracks the starting day anchor coordinate for your pagination windows
    private var currentAnchorDate: LocalDate = LocalDate.now()

    override suspend fun loadInitialData(userId: String, pageSize: Int): RepositoryFetchResult {
        currentAnchorDate = LocalDate.now()

        // Boundaries for the first 5 calendar days (e.g., Today down to 4 days ago)
        val startDateStr = currentAnchorDate.toString()
        val endDateStr = currentAnchorDate.minusDays(4).toString()

        Log.w("FIRESTORE_METRICS", "📡 Fetching initial 5 days: $startDateStr down to $endDateStr")
        val dailyDocs = fetchDayDocumentsBlock(userId, startDateStr, endDateStr)

        _cachedDays.value = dailyDocs
        currentAnchorDate = currentAnchorDate.minusDays(5)

        return RepositoryFetchResult(
            nextAnchorDate = currentAnchorDate,
            hasMoreData = true
        )
    }

    override suspend fun refreshData(userId: String, pageSize: Int): RepositoryFetchResult {
        return loadInitialData(userId, pageSize)
    }

    override suspend fun loadMoreData(userId: String, pageSize: Int): RepositoryFetchResult {
        val startDateStr = currentAnchorDate.toString()
        val endDateStr = currentAnchorDate.minusDays(4).toString()

        Log.w("FIRESTORE_METRICS", "📡 Paginiating next 5 days: $startDateStr down to $endDateStr")
        val newDailyDocs = fetchDayDocumentsBlock(userId, startDateStr, endDateStr)

        // Append the new calendar days cleanly into your local data memory pools
        _cachedDays.value = _cachedDays.value + newDailyDocs
        currentAnchorDate = currentAnchorDate.minusDays(5)

        return RepositoryFetchResult(
            nextAnchorDate = currentAnchorDate,
            hasMoreData = true
        )
    }

    override suspend fun saveActivityEvent(userId: String, date: String, event: UnifiedEventDto) {
        val db = firestore.collection("babyLogs").document(userId).collection("days").document(date)

        try {
            // 🚀 1. Try to atomically push the event into the array assuming the day doc exists
            db.update("events", FieldValue.arrayUnion(event)).await()
            Log.d("FIRESTORE_WRITE", "Successfully appended activity event to existing day document: $date")
        } catch (e: FirebaseFirestoreException) {
            // 🛑 2. If the document doesn't exist, handle the error code gracefully
            if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
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

    override suspend fun fetchScreenLayout(screenName: String): List<NetworkScreenData> {
        return try {
            // Query your remote static schema collection document payload
            val snapshot = firestore.collection("screens").document(screenName).get().await()
            val rawList = snapshot.get("ScreenContent") as? List<Map<String, Any>> ?: emptyList()

            // Loop through maps and cast them type-safely using your existing data schemas
            rawList.mapIndexed { fallbackIndex, map ->
                val type = map["type"] as? String ?: "UNKNOWN"
                val index = (map["index"] as? Long)?.toInt() ?: fallbackIndex

                when (type) {
                    "TITLE" -> NetworkScreenData.SmallTitleDataNetwork(
                        index = index,
                        content = map["content"] as? String ?: ""
                    )
                    "SUBTITLE" -> NetworkScreenData.SubtitleDataNetwork(
                        index = index,
                        content = map["content"] as? String ?: ""
                    )
                    "SPACER" -> {
                        val sizeObj = map["sizeobject"] as? Map<String, Any>
                        val heightVal = (sizeObj?.get("size") as? String)?.toIntOrNull() ?: 16
                        NetworkScreenData.SpacerDataNetwork(
                            index = index,
                            size = SizeData(
                                type = SpacerTypeData.HEIGHT,
                                height = heightVal,
                                weight = null,
                            )
                        )
                    }
                    "IMAGE" -> NetworkScreenData.ImageDataNetwork(
                        index = index,
                        url = map["url"] as? String ?: "",
                        contentDescription = map["contentDescription"] as? String ?: "",
                        height = (map["height"] as? Long)?.toInt() ?: 120,
                        width = (map["width"] as? Long)?.toInt() ?: 120
                    )
                    else -> NetworkScreenData.Unknown(index = index)
                }
            }.sortedBy { it.index } // Force structural synchronization sorting rules
        } catch (e: Exception) {
            Log.e("FIRESTORE_LAYOUT", "Failed to deserialize layout payload", e)
            emptyList()
        }
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
