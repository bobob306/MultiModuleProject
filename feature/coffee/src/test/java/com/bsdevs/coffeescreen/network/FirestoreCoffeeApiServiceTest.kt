package com.bsdevs.coffeescreen.network

import com.bsdevs.coffeescreen.screens.detailscreen.ShotDto
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreCoffeeApiServiceTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var service: FirestoreCoffeeApiService

    @Before
    fun setUp() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        firestore = mockk(relaxed = true)
        service = FirestoreCoffeeApiService(firestore)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `uploadCoffee writes to coffeeUploads collection with label as docId`() = runTest {
        val userId = "user1"
        val coffee = CoffeeDto(id = "c1", label = "Espresso Blend")
        val collection = mockk<CollectionReference>(relaxed = true)
        val document = mockk<DocumentReference>(relaxed = true)
        
        every { firestore.collection("coffeeUploads") } returns collection
        every { collection.document("Espresso Blend") } returns document
        coEvery { document.set(any<Map<String, Any?>>()).await() } returns mockk()

        service.uploadCoffee(userId, coffee)
        
        verify { collection.document("Espresso Blend") }
        verify { document.set(match<Map<String, Any?>> { 
            it["userId"] == userId && it["label"] == "Espresso Blend" 
        }) }
    }

    @Test
    fun `getCoffeeById queries with userId and coffeeId`() = runTest {
        val userId = "user1"
        val coffeeId = "c1"
        val collection = mockk<CollectionReference>(relaxed = true)
        val query = mockk<Query>(relaxed = true)
        val querySnapshot = mockk<QuerySnapshot>(relaxed = true)
        
        every { firestore.collection("coffeeUploads") } returns collection
        every { collection.whereEqualTo("userId", userId) } returns query
        every { query.whereEqualTo("id", coffeeId) } returns query
        coEvery { query.get().await() } returns querySnapshot
        every { querySnapshot.toObjects(CoffeeDto::class.java) } returns listOf(CoffeeDto(id = coffeeId))

        val result = service.getCoffeeById(userId, coffeeId)
        
        assertEquals(coffeeId, result?.id)
    }

    @Test
    fun `getShotsForCoffee fetches from shots subcollection`() = runTest {
        val coffeeLabel = "Espresso Blend"
        val collection = mockk<CollectionReference>(relaxed = true)
        val document = mockk<DocumentReference>(relaxed = true)
        val shotsCollection = mockk<CollectionReference>(relaxed = true)
        val querySnapshot = mockk<QuerySnapshot>(relaxed = true)
        
        every { firestore.collection("coffeeUploads") } returns collection
        every { collection.document(coffeeLabel) } returns document
        every { document.collection("shots") } returns shotsCollection
        coEvery { shotsCollection.get().await() } returns querySnapshot
        every { querySnapshot.toObjects(ShotDto::class.java) } returns listOf(ShotDto(id = "s1"))

        val result = service.getShotsForCoffee(coffeeLabel)
        
        assertEquals(1, result.size)
        assertEquals("s1", result.first().id)
    }

    @Test
    fun `uploadShot writes to shots subcollection`() = runTest {
        val coffeeLabel = "Espresso Blend"
        val shot = ShotDto(id = "s1")
        val collection = mockk<CollectionReference>(relaxed = true)
        val document = mockk<DocumentReference>(relaxed = true)
        val shotsCollection = mockk<CollectionReference>(relaxed = true)
        val shotDoc = mockk<DocumentReference>(relaxed = true)
        
        every { firestore.collection("coffeeUploads") } returns collection
        every { collection.document(coffeeLabel) } returns document
        every { document.collection("shots") } returns shotsCollection
        every { shotsCollection.document("s1") } returns shotDoc
        coEvery { shotDoc.set(shot).await() } returns mockk()

        service.uploadShot(coffeeLabel, shot)
        
        verify { shotsCollection.document("s1") }
    }
}
