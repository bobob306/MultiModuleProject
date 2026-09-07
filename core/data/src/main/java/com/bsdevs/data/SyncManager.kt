package com.bsdevs.data

import android.util.Log
import com.bsdevs.network.connectivity.ConnectivityObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface Syncable {
    suspend fun sync()
}

@Singleton
class SyncManager @Inject constructor(
    private val connectivityObserver: ConnectivityObserver
) {
    private val scope = CoroutineScope(SupervisorJob())
    private val syncables = mutableSetOf<Syncable>()

    init {
        scope.launch {
            connectivityObserver.observe().collectLatest { status ->
                Log.d("SYNC_MANAGER", "Connectivity status: $status")
                if (status == ConnectivityObserver.Status.Available) {
                    syncAll()
                }
            }
        }
    }

    private fun syncAll() {
        scope.launch {
            Log.d("SYNC_MANAGER", "Starting sync for ${syncables.size} syncables")
            syncables.forEach { 
                try {
                    it.sync()
                } catch (e: Exception) {
                    Log.e("SYNC_MANAGER", "Sync failed for $it", e)
                }
            }
        }
    }

    fun registerSyncable(syncable: Syncable) {
        syncables.add(syncable)
    }

    fun unregisterSyncable(syncable: Syncable) {
        syncables.remove(syncable)
    }
}
