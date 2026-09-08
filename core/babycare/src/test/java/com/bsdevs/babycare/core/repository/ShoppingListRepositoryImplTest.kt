package com.bsdevs.babycare.core.repository

import app.cash.turbine.test
import com.bsdevs.babycare.core.data.ShoppingListRepositoryImpl
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.SyncManager
import com.bsdevs.data.local.dao.ShoppingDao
import com.bsdevs.data.local.entities.ShoppingItemEntity
import com.bsdevs.data.repository.UserRepository
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.dto.ShoppingListDto
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var shoppingDao: ShoppingDao
    private lateinit var syncManager: SyncManager
    private lateinit var repository: ShoppingListRepositoryImpl
    private lateinit var dispatchers: DispatcherProvider

    @Before
    fun setUp() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        coEvery { any<Task<*>>().await() } returns mockk(relaxed = true)
        mockkStatic(FieldValue::class)
        firestore = mockk(relaxed = true)
        firestoreHolder = mockk(relaxed = true)
        every { firestoreHolder.firestore } returns firestore
        userRepository = mockk(relaxed = true)
        shoppingDao = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        
        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }
        repository = ShoppingListRepositoryImpl(
            firestoreHolder, userRepository, dispatchers, shoppingDao, syncManager
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `startListening collects from local DB`() = runTest {
        val babyId = "baby1"
        val item = ShoppingListDto(id = "item1", name = "Milk")
        val entity = ShoppingItemEntity("item1", babyId, item)
        coEvery { shoppingDao.getShoppingItems(babyId) } returns flowOf(listOf(entity))

        repository.startListening(babyId)

        repository.shoppingList.test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Milk", list.first().name)
        }
    }

    @Test
    fun `addShoppingItem saves to DB and firestore`() = runTest {
        val babyId = "baby1"
        val item = ShoppingListDto(id = "item1", name = "Diapers")
        
        repository.addShoppingItem(babyId, item)
        
        coVerify { shoppingDao.insertItems(match { it.first().item.name == "Diapers" }) }
        verify { firestore.collection("shoppingLists") }
    }

    @Test
    fun `sync pushes pending items`() = runTest {
        val babyId = "baby1"
        every { userRepository.userProfile.value?.babyId } returns babyId
        val item = ShoppingListDto(id = "p1", name = "Sync Me")
        val entity = ShoppingItemEntity("p1", babyId, item, isPendingSync = true)
        coEvery { shoppingDao.getPendingSync() } returns listOf(entity)

        repository.sync()

        verify { firestore.collection("shoppingLists").document(babyId) }
    }
}
