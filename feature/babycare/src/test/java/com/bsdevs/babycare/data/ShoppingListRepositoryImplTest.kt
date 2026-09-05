package com.bsdevs.babycare.data

import app.cash.turbine.test
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.dto.ShoppingListDto
import com.bsdevs.network.repository.UserRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firestoreHolder: FirestoreHolder
    private lateinit var userRepository: UserRepository
    private lateinit var repository: ShoppingListRepositoryImpl
    private lateinit var dispatchers: DispatcherProvider

    @Before
    fun setUp() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        mockkStatic(FieldValue::class)
        firestore = mockk(relaxed = true)
        firestoreHolder = mockk(relaxed = true)
        every { firestoreHolder.firestore } returns firestore
        userRepository = mockk(relaxed = true)
        
        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }
        repository = ShoppingListRepositoryImpl(firestoreHolder, userRepository, dispatchers)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `addShoppingItem writes to map in document with merge`() = runTest {
        val babyId = "baby1"
        val item = ShoppingListDto(name = "Diapers")
        
        val listCollection = mockk<CollectionReference>(relaxed = true)
        val babyDoc = mockk<DocumentReference>(relaxed = true)
        
        every { firestore.collection("shoppingLists") } returns listCollection
        every { listCollection.document(babyId) } returns babyDoc
        coEvery { babyDoc.set(any<Map<String, Any>>(), SetOptions.merge()).await() } returns mockk()
        
        repository.addShoppingItem(babyId, item)
        
        verify { babyDoc.set(match<Map<String, Any>> { it.containsKey("items") }, SetOptions.merge()) }
    }

    @Test
    fun `deleteShoppingItem calls update with delete field value`() = runTest {
        val babyId = "baby1"
        val itemId = "item1"
        val deleteValue = mockk<FieldValue>()
        every { FieldValue.delete() } returns deleteValue
        
        val listCollection = mockk<CollectionReference>(relaxed = true)
        val babyDoc = mockk<DocumentReference>(relaxed = true)
        
        every { firestore.collection("shoppingLists") } returns listCollection
        every { listCollection.document(babyId) } returns babyDoc
        coEvery { babyDoc.update("items.item1", deleteValue).await() } returns mockk()
        
        repository.deleteShoppingItem(babyId, itemId)
        
        coVerify { babyDoc.update("items.item1", deleteValue) }
    }

    @Test
    fun `updateShoppingItem calls update on specific map key`() = runTest {
        val babyId = "baby1"
        val item = ShoppingListDto(id = "item1", name = "Updated Name")
        
        val listCollection = mockk<CollectionReference>(relaxed = true)
        val babyDoc = mockk<DocumentReference>(relaxed = true)
        
        every { firestore.collection("shoppingLists") } returns listCollection
        every { listCollection.document(babyId) } returns babyDoc
        coEvery { babyDoc.update("items.item1", item).await() } returns mockk()
        
        repository.updateShoppingItem(babyId, item)
        
        coVerify { babyDoc.update("items.item1", item) }
    }

    @Test
    fun `clearCache resets state`() = runTest {
        repository.clearCache()
        repository.shoppingList.test {
            assertEquals(emptyList<ShoppingListDto>(), awaitItem())
        }
    }
}
