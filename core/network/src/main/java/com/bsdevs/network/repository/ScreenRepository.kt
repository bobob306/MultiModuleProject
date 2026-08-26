package com.bsdevs.network.repository

import com.bsdevs.common.DispatcherProvider
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
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

interface ScreenRepository {
    suspend fun getScreen(screen: String): Task<DocumentSnapshot>

    suspend fun getScreenFlow(screen: String): Flow<Result<List<ScreenDto>>>
}

class ScreenRepositoryImpl @Inject constructor(
    private val scr: CollectionReference,
    private val mapper: ScreenDtoMapper,
    private val dispatchers: DispatcherProvider,
) : ScreenRepository {
    private val cacheFlowMap = ConcurrentHashMap<String, List<ScreenDto>>()

    override suspend fun getScreen(screen: String): Task<DocumentSnapshot> {
        return scr.document(screen).get()
    }

    override suspend fun getScreenFlow(screen: String): Flow<Result<List<ScreenDto>>> = withContext(dispatchers.io) {
        val cached = cacheFlowMap[screen]
        if (cached != null) {
            flowOf(Result.Success(cached))
        } else {
            try {
                val document = scr.document(screen).get().await().data
                val dto = mapper.mapToDto(document as HashMap)
                cacheFlowMap[screen] = dto
                flowOf(Result.Success(dto))
            } catch (e: Exception) {
                flowOf(Result.Error(e))
            }
        }
    }
}
