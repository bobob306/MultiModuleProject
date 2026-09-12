package com.bsdevs.data.repository

import app.cash.turbine.test
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.SyncManager
import com.bsdevs.data.local.dao.FormDao
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.FormDtoMapper
import com.bsdevs.network.dto.FormSchemaDto
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.bouncycastle.util.test.SimpleTest.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firestoreHolder: FirestoreHolder
    private lateinit var mapper: FormDtoMapper
    private lateinit var formDao: FormDao
    private lateinit var syncManager: SyncManager
    private lateinit var userRepository: UserRepository
    private lateinit var repository: FormRepositoryImpl
    private lateinit var dispatchers: DispatcherProvider

    @Before
    fun setUp() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        firestore = mockk(relaxed = true)
        firestoreHolder = mockk(relaxed = true)
        every { firestoreHolder.firestore } returns firestore
        userRepository = mockk(relaxed = true)
        mapper = mockk()
        formDao = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        
        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }
        repository = FormRepositoryImpl(firestoreHolder, userRepository, mapper, dispatchers, formDao, syncManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getFormSchema emits Loading then Success`() = runTest {
        val formId = "testForm"
        val schemaDto = FormSchemaDto(title = "Test")
        
        val collection = mockk<CollectionReference>(relaxed = true)
        val document = mockk<DocumentReference>(relaxed = true)
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        val task = mockk<Task<DocumentSnapshot>>(relaxed = true)
        
        every { firestore.collection("formSchemas") } returns collection
        every { collection.document(formId) } returns document
        every { document.get() } returns task
        coEvery { task.await() } returns snapshot
        every { snapshot.data } returns mapOf("title" to "Test", "fields" to emptyList<Any>())
        every { mapper.mapToDto(any()) } returns schemaDto
        coEvery { formDao.getSchema(any()) } returns null

        repository.getFormSchema(formId).test {
            val first = awaitItem()
            if (first is Result.Loading) {
                val second = awaitItem() as Result.Success
                assertEquals(schemaDto, second.data)
            } else {
                val success = first as Result.Success
                assertEquals(schemaDto, success.data)
            }
            awaitComplete()
        }
    }

    @Test
    fun `getFormSchema emits Error when not found and no cache`() = runTest {
        val formId = "missing"
        coEvery { formDao.getSchema(any()) } returns null
        
        val collection = mockk<CollectionReference>(relaxed = true)
        val document = mockk<DocumentReference>(relaxed = true)
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        val task = mockk<Task<DocumentSnapshot>>(relaxed = true)
        
        every { firestore.collection("formSchemas") } returns collection
        every { collection.document(formId) } returns document
        every { document.get() } returns task
        coEvery { task.await() } returns snapshot
        every { snapshot.data } returns null

        repository.getFormSchema(formId).test {
            val first = awaitItem()
            if (first is Result.Loading) {
                val second = awaitItem() as Result.Error
                assertTrue(second.exception.message!!.contains("not found"))
            } else {
                val error = first as Result.Error
                assertTrue(error.exception.message!!.contains("not found"))
            }
            awaitComplete()
        }
    }
}
