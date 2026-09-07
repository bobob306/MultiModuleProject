package com.bsdevs.data.repository

import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.local.dao.UserBabyDao
import com.bsdevs.data.local.entities.UserEntity
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.dto.UserDto
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firestoreHolder: FirestoreHolder
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var dispatchers: DispatcherProvider
    private lateinit var userBabyDao: UserBabyDao

    @Before
    fun setUp() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        coEvery { any<Task<*>>().await() } returns mockk(relaxed = true)

        firestore = mockk(relaxed = true)
        firestoreHolder = mockk(relaxed = true)
        every { firestoreHolder.firestore } returns firestore
        userBabyDao = mockk(relaxed = true)

        dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }
        userRepository = UserRepositoryImpl(firestoreHolder, dispatchers, userBabyDao)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `saveUser updates both firestore and DB`() = runTest {
        val user = UserDto(id = "u1", firstName = "John")

        userRepository.saveUser(user)

        verify { firestore.collection("users").document("u1") }
        coVerify { userBabyDao.insertUser(match { it.id == "u1" }) }
    }

    @Test
    fun `getUser returns from DB when available`() = runTest {
        val user = UserDto(id = "u1", firstName = "Cached")
        coEvery { userBabyDao.getUser("u1") } returns UserEntity("u1", user)

        val result = userRepository.getUser("u1")

        assertEquals(user, result)
        // Verify firestore was NOT called
        verify(exactly = 0) { firestore.collection("users") }
    }
}
