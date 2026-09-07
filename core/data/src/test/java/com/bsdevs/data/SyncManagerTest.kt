package com.bsdevs.data

import com.bsdevs.common.DispatcherProvider
import com.bsdevs.network.connectivity.ConnectivityObserver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncManagerTest {

    private lateinit var syncManager: SyncManager
    private lateinit var connectivityObserver: ConnectivityObserver
    private val connectivityFlow = MutableStateFlow(ConnectivityObserver.Status.Unavailable)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        connectivityObserver = mockk()
        every { connectivityObserver.observe() } returns connectivityFlow
        val dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }
        syncManager = SyncManager(connectivityObserver, dispatchers)
    }

    @Test
    fun `syncAll is triggered when connectivity becomes Available`() = runTest {
        val syncable = mockk<Syncable>(relaxed = true)
        syncManager.registerSyncable(syncable)

        connectivityFlow.value = ConnectivityObserver.Status.Available
        advanceUntilIdle()

        coVerify { syncable.sync() }
    }

    @Test
    fun `syncAll is NOT triggered when connectivity is Unavailable`() = runTest {
        val syncable = mockk<Syncable>(relaxed = true)
        syncManager.registerSyncable(syncable)

        connectivityFlow.value = ConnectivityObserver.Status.Unavailable
        advanceUntilIdle()

        coVerify(exactly = 0) { syncable.sync() }
    }

    @Test
    fun `syncAll continues if one syncable fails`() = runTest {
        val failingSyncable = mockk<Syncable> {
            coEvery { sync() } throws RuntimeException("Sync failed")
        }
        val succeedingSyncable = mockk<Syncable>(relaxed = true)
        
        syncManager.registerSyncable(failingSyncable)
        syncManager.registerSyncable(succeedingSyncable)

        connectivityFlow.value = ConnectivityObserver.Status.Available
        advanceUntilIdle()

        coVerify { failingSyncable.sync() }
        coVerify { succeedingSyncable.sync() }
    }

    @Test
    fun `unregistered syncable is not triggered`() = runTest {
        val syncable = mockk<Syncable>(relaxed = true)
        syncManager.registerSyncable(syncable)
        syncManager.unregisterSyncable(syncable)

        connectivityFlow.value = ConnectivityObserver.Status.Available
        advanceUntilIdle()

        coVerify(exactly = 0) { syncable.sync() }
    }
}
