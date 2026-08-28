package com.bsdevs.network.repository

import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.dto.BabyDto
import com.bsdevs.network.dto.UserDto
import com.bsdevs.network.FirestoreHolder
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firestoreHolder: FirestoreHolder
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var dispatchers: DispatcherProvider

    @Before
    fun setUp() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        firestore = mockk(relaxed = true)
        firestoreHolder = mockk(relaxed = true)
        every { firestoreHolder.firestore } returns firestore
        
        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }
        userRepository = UserRepositoryImpl(firestoreHolder, dispatchers)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `saveUser writes to firestore and updates flow`() = runTest {
        val user = UserDto(id = "user1", firstName = "John")
        val collection = mockk<CollectionReference>(relaxed = true)
        val document = mockk<DocumentReference>(relaxed = true)
        
        every { firestore.collection("users") } returns collection
        every { collection.document("user1") } returns document
        coEvery { document.set(user).await() } returns mockk()
        
        userRepository.saveUser(user)
        
        assertEquals(user, userRepository.userProfile.value)
        verify { collection.document("user1") }
    }

    @Test
    fun `getUser cached returns profile correctly without get call`() = runTest {
        val userId = "user1"
        val user = UserDto(id = userId, firstName = "Cached")
        
        // Populate cache via saveUser
        val collection = mockk<CollectionReference>(relaxed = true)
        val document = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("users") } returns collection
        every { collection.document(userId) } returns document
        coEvery { document.set(user).await() } returns mockk()
        
        userRepository.saveUser(user)
        
        // Clear recording of the saveUser call to collection.document(userId)
        clearMocks(collection, document)
        every { firestore.collection("users") } returns collection
        every { collection.document(userId) } returns document

        val result = userRepository.getUser(userId, forceRefresh = false)
        
        assertEquals(user, result)
        // Verify no NEW read was performed
        verify(exactly = 0) { document.get(any<com.google.firebase.firestore.Source>()) }
    }

    @Test
    fun `getUser fetches from network on cache miss`() = runTest {
        val userId = "user1"
        val user = UserDto(id = userId, firstName = "Network")
        val collection = mockk<CollectionReference>(relaxed = true)
        val document = mockk<DocumentReference>(relaxed = true)
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        
        every { firestore.collection("users") } returns collection
        every { collection.document(userId) } returns document
        coEvery { document.get(any<com.google.firebase.firestore.Source>()).await() } returns snapshot
        every { snapshot.toObject(UserDto::class.java) } returns user
        
        val result = userRepository.getUser(userId, forceRefresh = false)
        
        assertEquals(user, result)
        assertEquals(user, userRepository.userProfile.value)
    }

    @Test
    fun `saveBaby writes to firestore`() = runTest {
        val baby = BabyDto(id = "baby1", firstName = "Junior")
        val collection = mockk<CollectionReference>(relaxed = true)
        val document = mockk<DocumentReference>(relaxed = true)
        
        every { firestore.collection("babies") } returns collection
        every { collection.document("baby1") } returns document
        coEvery { document.set(baby).await() } returns mockk()
        
        userRepository.saveBaby(baby)
        
        verify { collection.document("baby1") }
    }

    @Test
    fun `babyExists returns true when document exists`() = runTest {
        val babyId = "baby1"
        val collection = mockk<CollectionReference>(relaxed = true)
        val document = mockk<DocumentReference>(relaxed = true)
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        
        every { firestore.collection("babies") } returns collection
        every { collection.document(babyId) } returns document
        coEvery { document.get().await() } returns snapshot
        every { snapshot.exists() } returns true
        
        val exists = userRepository.babyExists(babyId)
        
        assertEquals(true, exists)
    }

    @Test
    fun `getUser returns null on failure`() = runTest {
        val userId = "user1"
        val collection = mockk<CollectionReference>(relaxed = true)
        val document = mockk<DocumentReference>(relaxed = true)
        
        every { firestore.collection("users") } returns collection
        every { collection.document(userId) } returns document
        coEvery { document.get(any<com.google.firebase.firestore.Source>()).await() } throws Exception("Network error")
        
        val result = userRepository.getUser(userId, forceRefresh = false)
        
        assertNull(result)
    }

    @Test
    fun `deleteUserData deletes user and associated data`() = runTest {
        val userId = "user1"
        val user = UserDto(id = userId, babyId = "baby1", babyIds = listOf("baby1"))
        
        // Mock getUser to return the user profile
        val userCollection = mockk<CollectionReference>(relaxed = true)
        val userDoc = mockk<DocumentReference>(relaxed = true)
        val userSnapshot = mockk<DocumentSnapshot>(relaxed = true)
        
        every { firestore.collection("users") } returns userCollection
        every { userCollection.document(userId) } returns userDoc
        coEvery { userDoc.get(any<com.google.firebase.firestore.Source>()).await() } returns userSnapshot
        every { userSnapshot.toObject(UserDto::class.java) } returns user
        
        // Mock baby lookup and deletion
        val babyCollection = mockk<CollectionReference>(relaxed = true)
        val babyDoc = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("babies") } returns babyCollection
        every { babyCollection.document("baby1") } returns babyDoc
        
        // Mock query for other parents (none found)
        val queryResult = mockk<com.google.firebase.firestore.QuerySnapshot>(relaxed = true)
        coEvery { userCollection.whereEqualTo("babyId", "baby1").get().await() } returns queryResult
        coEvery { userCollection.whereArrayContains("babyIds", "baby1").get().await() } returns queryResult
        every { queryResult.documents } returns emptyList()
        
        // Mock baby logs deletion
        val babyLogsCollection = mockk<CollectionReference>(relaxed = true)
        val babyLogsDoc = mockk<DocumentReference>(relaxed = true)
        val monthsCollection = mockk<CollectionReference>(relaxed = true)
        val monthsQueryResult = mockk<com.google.firebase.firestore.QuerySnapshot>(relaxed = true)
        
        every { firestore.collection("babyLogs") } returns babyLogsCollection
        every { babyLogsCollection.document("baby1") } returns babyLogsDoc
        every { babyLogsDoc.collection("months") } returns monthsCollection
        coEvery { monthsCollection.get().await() } returns monthsQueryResult
        every { monthsQueryResult.documents } returns emptyList()
        coEvery { babyLogsDoc.delete().await() } returns mockk()
        
        // Mock coffee logs deletion
        val coffeeCollection = mockk<CollectionReference>(relaxed = true)
        val coffeeQueryResult = mockk<com.google.firebase.firestore.QuerySnapshot>(relaxed = true)
        every { firestore.collection("coffeeUploads") } returns coffeeCollection
        coEvery { coffeeCollection.whereEqualTo("userId", userId).get().await() } returns coffeeQueryResult
        every { coffeeQueryResult.documents } returns emptyList()

        coEvery { userDoc.delete().await() } returns mockk()
        coEvery { babyDoc.delete().await() } returns mockk()

        userRepository.deleteUserData(userId)
        
        coVerify { userDoc.delete().await() }
        coVerify { babyDoc.delete().await() }
        coVerify { babyLogsDoc.delete().await() }
        assertNull(userRepository.userProfile.value)
    }

    @Test
    fun `deleteUserData does NOT delete baby if other parents exist`() = runTest {
        val userId = "user1"
        val user = UserDto(id = userId, babyId = "shared_baby")
        
        val userCollection = mockk<CollectionReference>(relaxed = true)
        val userDoc = mockk<DocumentReference>(relaxed = true)
        val userSnapshot = mockk<DocumentSnapshot>(relaxed = true)
        
        every { firestore.collection("users") } returns userCollection
        every { userCollection.document(userId) } returns userDoc
        coEvery { userDoc.get(any<com.google.firebase.firestore.Source>()).await() } returns userSnapshot
        every { userSnapshot.toObject(UserDto::class.java) } returns user
        
        val babyCollection = mockk<CollectionReference>(relaxed = true)
        val babyDoc = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("babies") } returns babyCollection
        every { babyCollection.document("shared_baby") } returns babyDoc
        
        // Mock query for other parents (Found another parent!)
        val otherParentDoc = mockk<DocumentSnapshot>(relaxed = true)
        every { otherParentDoc.id } returns "user2"
        val queryResult = mockk<com.google.firebase.firestore.QuerySnapshot>(relaxed = true)
        every { queryResult.documents } returns listOf(otherParentDoc)
        
        coEvery { userCollection.whereEqualTo("babyId", "shared_baby").get().await() } returns queryResult
        coEvery { userCollection.whereArrayContains("babyIds", "shared_baby").get().await() } returns queryResult
        
        // Mock coffee logs deletion (Required even if baby is not deleted)
        val coffeeCollection = mockk<CollectionReference>(relaxed = true)
        val coffeeQueryResult = mockk<com.google.firebase.firestore.QuerySnapshot>(relaxed = true)
        every { firestore.collection("coffeeUploads") } returns coffeeCollection
        coEvery { coffeeCollection.whereEqualTo("userId", userId).get().await() } returns coffeeQueryResult
        every { coffeeQueryResult.documents } returns emptyList()

        coEvery { userDoc.delete().await() } returns mockk()

        userRepository.deleteUserData(userId)
        
        coVerify { userDoc.delete().await() }
        coVerify(exactly = 0) { babyDoc.delete().await() }
    }

    @Test
    fun `deleteUserData deletes nested coffee shots`() = runTest {
        val userId = "user1"
        val user = UserDto(id = userId)
        
        val userCollection = mockk<CollectionReference>(relaxed = true)
        val userDoc = mockk<DocumentReference>(relaxed = true)
        val userSnapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { firestore.collection("users") } returns userCollection
        every { userCollection.document(userId) } returns userDoc
        coEvery { userDoc.get(any<com.google.firebase.firestore.Source>()).await() } returns userSnapshot
        every { userSnapshot.toObject(UserDto::class.java) } returns user
        
        val coffeeCollection = mockk<CollectionReference>(relaxed = true)
        val coffeeDoc = mockk<DocumentReference>(relaxed = true)
        val coffeeSnapshot = mockk<DocumentSnapshot>(relaxed = true)
        val coffeeQueryResult = mockk<com.google.firebase.firestore.QuerySnapshot>(relaxed = true)
        
        every { firestore.collection("coffeeUploads") } returns coffeeCollection
        coEvery { coffeeCollection.whereEqualTo("userId", userId).get().await() } returns coffeeQueryResult
        every { coffeeQueryResult.documents } returns listOf(coffeeSnapshot)
        every { coffeeSnapshot.reference } returns coffeeDoc
        
        val shotsCollection = mockk<CollectionReference>(relaxed = true)
        val shotDoc = mockk<DocumentReference>(relaxed = true)
        val shotSnapshot = mockk<DocumentSnapshot>(relaxed = true)
        val shotsQueryResult = mockk<com.google.firebase.firestore.QuerySnapshot>(relaxed = true)
        
        every { coffeeDoc.collection("shots") } returns shotsCollection
        coEvery { shotsCollection.get().await() } returns shotsQueryResult
        every { shotsQueryResult.documents } returns listOf(shotSnapshot)
        every { shotSnapshot.reference } returns shotDoc
        
        coEvery { shotDoc.delete().await() } returns mockk()
        coEvery { coffeeDoc.delete().await() } returns mockk()
        coEvery { userDoc.delete().await() } returns mockk()

        userRepository.deleteUserData(userId)
        
        coVerify { shotDoc.delete().await() }
        coVerify { coffeeDoc.delete().await() }
        coVerify { userDoc.delete().await() }
    }
}
