package com.bsdevs.babycare.network

import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.dto.UserDto
import com.bsdevs.network.repository.UserRepository
import com.bsdevs.network.FirestoreHolder
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreBabyCareServiceTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firestoreHolder: FirestoreHolder
    private lateinit var userRepository: UserRepository
    private lateinit var service: FirestoreBabyCareService
    private lateinit var dispatchers: DispatcherProvider

    private val userId = "user1"
    private val babyId = "baby1"

    @Before
    fun setUp() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        firestore = mockk(relaxed = true)
        firestoreHolder = mockk(relaxed = true)
        every { firestoreHolder.firestore } returns firestore
        
        userRepository = mockk(relaxed = true)
        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }
        service = FirestoreBabyCareService(firestoreHolder, userRepository, dispatchers)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getLatestMonthId returns null if user is not authorized for baby data`() = runTest {
        // User with no babies
        every { userRepository.userProfile } returns MutableStateFlow(UserDto(id = userId))
        
        val result = service.getLatestMonthId(userId, forceRefresh = false)
        
        assertNull(result)
    }

    @Test
    fun `getLatestMonthId returns monthId for authorized baby`() = runTest {
        val user = UserDto(id = userId, babyId = babyId)
        every { userRepository.userProfile } returns MutableStateFlow(user)
        
        val collection = mockk<CollectionReference>(relaxed = true)
        val query = mockk<Query>(relaxed = true)
        val querySnapshot = mockk<QuerySnapshot>(relaxed = true)
        val document = mockk<QueryDocumentSnapshot>(relaxed = true)

        every { firestore.collection("babyLogs").document(babyId).collection("months") } returns collection
        every { collection.orderBy(any<FieldPath>(), any()) } returns query
        every { query.limit(1) } returns query
        coEvery { query.get(any<Source>()).await() } returns querySnapshot
        every { querySnapshot.documents } returns listOf(document)
        every { document.id } returns "2026-08"

        val result = service.getLatestMonthId(userId, forceRefresh = false)
        
        assertEquals("2026-08", result)
    }

    @Test
    fun `saveEvent uses arrayUnion when document exists`() = runTest {
        val user = UserDto(id = userId, babyId = babyId)
        every { userRepository.userProfile } returns MutableStateFlow(user)
        
        val docRef = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("babyLogs").document(babyId).collection("months").document("2026-08") } returns docRef
        
        val event = mapOf("id" to "e1")
        coEvery { docRef.update("days.2026-08-27", any()).await() } returns mockk()

        service.saveEvent(userId, "2026-08", "2026-08-27", event)
        
        coVerify { docRef.update("days.2026-08-27", any()) }
    }

    @Test
    fun `saveEvent uses set with merge when document does not exist`() = runTest {
        val user = UserDto(id = userId, babyId = babyId)
        every { userRepository.userProfile } returns MutableStateFlow(user)
        
        val docRef = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("babyLogs").document(babyId).collection("months").document("2026-08") } returns docRef
        
        val event = mapOf("id" to "e1")
        val exception = mockk<FirebaseFirestoreException>(relaxed = true)
        every { exception.code } returns FirebaseFirestoreException.Code.NOT_FOUND
        
        coEvery { docRef.update("days.2026-08-27", any()).await() } throws exception
        coEvery { docRef.set(any<Map<String, Any>>(), any<SetOptions>()).await() } returns mockk()

        service.saveEvent(userId, "2026-08", "2026-08-27", event)
        
        coVerify { docRef.set(any<Map<String, Any>>(), any<SetOptions>()) }
    }

    @Test
    fun `deleteEvent performs transaction with correct logic`() = runTest {
        val user = UserDto(id = userId, babyId = babyId)
        every { userRepository.userProfile } returns MutableStateFlow(user)
        
        val docRef = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("babyLogs").document(babyId).collection("months").document("2026-08") } returns docRef
        
        val event1 = mapOf("id" to "e1")
        val event2 = mapOf("id" to "e2")
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { snapshot["days"] } returns mapOf("2026-08-27" to listOf(event1, event2))
        
        val transaction = mockk<Transaction>(relaxed = true)
        every { transaction.get(docRef) } returns snapshot
        
        // Capture the transaction block
        val transactionSlot = slot<Transaction.Function<Unit>>()
        coEvery { firestore.runTransaction(capture(transactionSlot)).await() } returns mockk()
        
        service.deleteEvent(userId, "2026-08", "2026-08-27", "e1")
        
        // Execute the captured transaction logic
        transactionSlot.captured.apply(transaction)
        
        verify { transaction.update(docRef, "days.2026-08-27", listOf(event2)) }
    }

    @Test
    fun `saveVaccination uses update with dot notation when document exists`() = runTest {
        val user = UserDto(id = userId, babyId = babyId)
        every { userRepository.userProfile } returns MutableStateFlow(user)
        
        val docRef = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("babyLogs").document(babyId).collection("vaccinations").document("all_data") } returns docRef
        
        val vaccine = mapOf("id" to "v1", "type" to "VACCINATION")
        coEvery { docRef.update("items.v1", vaccine).await() } returns mockk()

        service.saveVaccination(userId, "v1", vaccine)
        
        coVerify { docRef.update("items.v1", vaccine) }
    }

    @Test
    fun `fetchAllVaccinations returns list from document map`() = runTest {
        val user = UserDto(id = userId, babyId = babyId)
        every { userRepository.userProfile } returns MutableStateFlow(user)
        
        val docRef = mockk<DocumentReference>(relaxed = true)
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { firestore.collection("babyLogs").document(babyId).collection("vaccinations").document("all_data") } returns docRef
        coEvery { docRef.get().await() } returns snapshot
        every { snapshot.exists() } returns true
        
        val v1 = mapOf("id" to "v1", "dateTimeString" to "2026-08-27 10:00")
        val v2 = mapOf("id" to "v2", "dateTimeString" to "2026-08-27 11:00")
        every { snapshot.data } returns mapOf("items" to mapOf("v1" to v1, "v2" to v2))

        val result = service.fetchAllVaccinations(userId)
        
        assertEquals(2, result.size)
        assertEquals("v2", result[0]["id"]) // Sorted by dateTimeString desc
    }
}
