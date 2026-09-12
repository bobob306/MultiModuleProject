package com.bsdevs.data.repository

import android.util.Log
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.data.local.dao.FormDao
import com.bsdevs.data.local.dao.ScreenDao
import com.bsdevs.data.local.entities.FormSchemaEntity
import com.bsdevs.data.local.entities.ScreenEntity
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.common.FirebaseLogger
import com.bsdevs.network.MetadataDtoMapper
import com.bsdevs.network.dto.AppMetadataDto
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface MetadataRepository {
    suspend fun fetchMetadata(forceRefresh: Boolean = false)
    suspend fun updateMetadata(metadata: AppMetadataDto)
}

@Singleton
class MetadataRepositoryImpl @Inject constructor(
    private val firestoreHolder: FirestoreHolder,
    private val mapper: MetadataDtoMapper,
    private val screenDao: ScreenDao,
    private val formDao: FormDao,
    private val dispatchers: DispatcherProvider
) : MetadataRepository {

    private val firestore get() = firestoreHolder.firestore

    override suspend fun fetchMetadata(forceRefresh: Boolean) {
        withContext(dispatchers.io) {
            try {
                FirebaseLogger.logCall("Read App Metadata (One-time fetch)")
                val snapshot = firestore.collection("metadata").document("app_config").get().await()
                val data = snapshot.data
                if (data != null) {
                    val metadata = mapper.mapToDto(data)

                    // Populate ScreenDao
                    metadata.screens.forEach { (id, components) ->
                        screenDao.insertScreen(ScreenEntity(id, components))
                    }

                    // Populate FormDao
                    metadata.forms.forEach { (id, schema) ->
                        formDao.insertSchema(FormSchemaEntity(id, schema))
                    }
                }
            } catch (e: Exception) {
                Log.e("METADATA_REPO", "Failed to fetch app metadata", e)
            }
        }
    }

    override suspend fun updateMetadata(metadata: AppMetadataDto) {
        withContext(dispatchers.io) {
            try {
                val map = mapper.mapToFirebase(metadata)
                FirebaseLogger.logCall("Update App Metadata")
                firestore.collection("metadata").document("app_config").set(map).await()

                // Also update local cache
                metadata.screens.forEach { (id, components) ->
                    screenDao.insertScreen(ScreenEntity(id, components))
                }
                metadata.forms.forEach { (id, schema) ->
                    formDao.insertSchema(FormSchemaEntity(id, schema))
                }
            } catch (e: Exception) {
                Log.e("METADATA_REPO", "Failed to update app metadata", e)
            }
        }
    }
}
