package com.bsdevs.data.repository

import app.cash.turbine.test
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.local.dao.ScreenDao
import com.bsdevs.data.local.entities.ScreenEntity
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.ScreenDtoMapper
import com.bsdevs.network.dto.ScreenDto
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
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
class ScreenRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firestoreHolder: FirestoreHolder
    private lateinit var userRepository: UserRepository
    private lateinit var mapper: ScreenDtoMapper
    private lateinit var screenDao: ScreenDao
    private lateinit var repository: ScreenRepositoryImpl
    private lateinit var dispatchers: DispatcherProvider

    @Before
    fun setUp() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        firestore = mockk(relaxed = true)
        firestoreHolder = mockk(relaxed = true)
        every { firestoreHolder.firestore } returns firestore
        userRepository = mockk(relaxed = true)
        mapper = mockk()
        screenDao = mockk(relaxed = true)
        
        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }
        repository = ScreenRepositoryImpl(
            firestoreHolder, userRepository, mapper, dispatchers, screenDao
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getScreenFlow returns cached data first`() = runTest {
        val screenId = "home"
        val components = listOf(ScreenDto.TitleDto(0, "Welcome"))
        coEvery { screenDao.getScreen(screenId) } returns ScreenEntity(screenId, components)

        repository.getScreenFlow(screenId).test {
            val result = awaitItem() as Result.Success
            assertEquals(components, result.data)
            cancelAndIgnoreRemainingEvents()
        }
        
        coVerify { screenDao.getScreen(screenId) }
    }

    @Test
    fun `getScreenFlow updates cache from network`() = runTest {
        val screenId = "home"
        val components = listOf(ScreenDto.TitleDto(0, "Network Data"))
        coEvery { screenDao.getScreen(screenId) } returns null
        
        val collection = mockk<CollectionReference>(relaxed = true)
        val document = mockk<DocumentReference>(relaxed = true)
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        val task = mockk<Task<DocumentSnapshot>>(relaxed = true)
        
        every { firestore.collection("screens") } returns collection
        every { collection.document(screenId) } returns document
        every { document.get(any<Source>()) } returns task
        coEvery { task.await() } returns snapshot
        
        every { snapshot.data } returns hashMapOf<String, Any>()
        every { mapper.mapToDto(any()) } returns components

        repository.getScreenFlow(screenId).test {
            val result = awaitItem() as Result.Success
            assertEquals(components, result.data)
            awaitComplete()
        }

        coVerify { screenDao.insertScreen(match { it.screenId == screenId && it.components == components }) }
    }

    @Test
    fun `updateScreen updates both firestore and room`() = runTest {
        val screenId = "home"
        val components = listOf(ScreenDto.TitleDto(0, "Updated"))
        val map = mapOf("components" to emptyList<Any>())
        
        val collection = mockk<CollectionReference>(relaxed = true)
        val document = mockk<DocumentReference>(relaxed = true)
        val task = mockk<Task<Void>>(relaxed = true)
        
        every { firestore.collection("screens") } returns collection
        every { collection.document(screenId) } returns document
        every { mapper.mapToFirebase(components) } returns map
        every { document.set(any<Map<String, Any>>()) } returns task
        coEvery { task.await() } returns mockk()

        repository.updateScreen(screenId, components)

        verify { document.set(map) }
        coVerify { screenDao.insertScreen(match { it.screenId == screenId && it.components == components }) }
    }
}
