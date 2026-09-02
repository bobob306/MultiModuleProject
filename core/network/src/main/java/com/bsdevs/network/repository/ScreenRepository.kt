package com.bsdevs.network.repository

import android.util.Log
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.ScreenDtoMapper
import com.bsdevs.network.dto.ScreenDto
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

interface ScreenRepository {
    suspend fun getScreen(screen: String): Task<DocumentSnapshot>

    suspend fun getScreenFlow(screen: String, forceRefresh: Boolean = false): Flow<Result<List<ScreenDto>>>

    suspend fun updateScreen(screen: String, dtos: List<ScreenDto>)

    suspend fun deleteScreen(screen: String)

    fun clearCache()
}

class ScreenRepositoryImpl @Inject constructor(
    private val firestoreHolder: FirestoreHolder,
    private val userRepository: UserRepository,
    private val mapper: ScreenDtoMapper,
    private val dispatchers: DispatcherProvider,
) : ScreenRepository, Clearable {
    private val cacheFlowMap = ConcurrentHashMap<String, List<ScreenDto>>()

    init {
        userRepository.registerClearable(this)
    }

    private val scr get() = firestoreHolder.firestore.collection("screens")

    override suspend fun getScreen(screen: String): Task<DocumentSnapshot> {
        return scr.document(screen).get()
    }

    override suspend fun getScreenFlow(screen: String, forceRefresh: Boolean): Flow<Result<List<ScreenDto>>> = withContext(dispatchers.io) {
        val cached = if (forceRefresh) null else cacheFlowMap[screen]
        if (cached != null) {
            flowOf(Result.Success(cached))
        } else {
            try {
                Log.d("FIREBASE_CALL", "Read Screen: $screen (Force: $forceRefresh)")
                val source = if (forceRefresh) com.google.firebase.firestore.Source.SERVER else com.google.firebase.firestore.Source.DEFAULT
                val document = scr.document(screen).get(source).await().data
                val dto = mapper.mapToDto(document as HashMap)
                cacheFlowMap[screen] = dto
                flowOf(Result.Success(dto))
            } catch (e: Exception) {
                flowOf(Result.Error(e))
            }
        }
    }

    override suspend fun updateScreen(screen: String, dtos: List<ScreenDto>) = withContext(dispatchers.io) {
        val map = mapper.mapToFirebase(dtos)
        Log.d("FIREBASE_CALL", "Update Screen: $screen")
        scr.document(screen).set(map).await()
        cacheFlowMap[screen] = dtos
    }

    override suspend fun deleteScreen(screen: String) = withContext(dispatchers.io) {
        Log.d("FIREBASE_CALL", "Delete Screen: $screen")
        scr.document(screen).delete().await()
        cacheFlowMap.remove(screen)
        Unit
    }

    override fun clearCache() {
        cacheFlowMap.clear()
    }
}
