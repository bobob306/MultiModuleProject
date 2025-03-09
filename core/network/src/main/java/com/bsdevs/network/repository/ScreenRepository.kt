package com.bsdevs.network.repository

import com.bsdevs.common.result.Result
import com.bsdevs.network.ScreenDtoMapper
import com.bsdevs.network.dto.ScreenDto
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

interface ScreenRepository {
    suspend fun getScreen(screen: String): Task<DocumentSnapshot>

    suspend fun getScreenFlow(screen: String): Flow<Result<List<ScreenDto>>>
}

class ScreenRepositoryImpl @Inject constructor(
    private val scr: CollectionReference,
    private val mapper: ScreenDtoMapper,
) : ScreenRepository {
    private var cache: Task<DocumentSnapshot>? = null
    private var cacheFlow: List<ScreenDto>? = null

    override suspend fun getScreen(screen: String): Task<DocumentSnapshot> {
        return if (cache != null) {
            cache as Task<DocumentSnapshot>
        } else {
            cache = scr.document(screen).get()
            return cache as Task<DocumentSnapshot>
        }
    }

    override suspend fun getScreenFlow(screen: String): Flow<Result<List<ScreenDto>>> {
        if (cacheFlow != null) {
            return flowOf(Result.Success(cacheFlow!!))
        } else {
            try {
                val document = scr.document(screen).get().await().data
                println("document = $document")
                cacheFlow = mapper.mapToDto(document as HashMap)
                return flowOf(Result.Success(cacheFlow!!))
            } catch (e: Exception) {
                println(e.message)
                return flowOf(Result.Error(e))
            }
        }
    }
}