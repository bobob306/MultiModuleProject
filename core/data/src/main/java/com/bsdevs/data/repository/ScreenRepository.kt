package com.bsdevs.data.repository

import android.util.Log
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.local.dao.ScreenDao
import com.bsdevs.data.local.entities.ScreenEntity
import com.bsdevs.network.FirestoreHolder
import com.bsdevs.network.ScreenDtoMapper
import com.bsdevs.network.dto.ScreenDto
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    private val screenDao: ScreenDao
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
        flow {
            val cached = screenDao.getScreen(screen)
            cached?.let {
                emit(Result.Success(it.components))
                cacheFlowMap[screen] = it.components
            }

            if (forceRefresh || cached == null) {
                try {
                    Log.d("FIREBASE_CALL", "Read Screen: $screen (Force: $forceRefresh)")
                    val source = if (forceRefresh) com.google.firebase.firestore.Source.SERVER else com.google.firebase.firestore.Source.DEFAULT
                    val snapshot = scr.document(screen).get(source).await()
                    val document = snapshot.data
                    
                    if (document != null) {
                        val dto = mapper.mapToDto(document as HashMap)
                        screenDao.insertScreen(ScreenEntity(screen, dto))
                        cacheFlowMap[screen] = dto
                        emit(Result.Success(dto))
                    } else if (cached == null) {
                        emit(Result.Error(Exception("Screen document not found")))
                    }
                } catch (e: Exception) {
                    Log.e("SCREEN_REPO", "Failed to fetch screen $screen", e)
                    if (cached == null) emit(Result.Error(e))
                }
            }
        }
    }

    override suspend fun updateScreen(screen: String, dtos: List<ScreenDto>) = withContext(dispatchers.io) {
        val map = mapper.mapToFirebase(dtos)
        Log.d("FIREBASE_CALL", "Update Screen: $screen")
        scr.document(screen).set(map).await()
        screenDao.insertScreen(ScreenEntity(screen, dtos))
        cacheFlowMap[screen] = dtos
    }

    override suspend fun deleteScreen(screen: String) = withContext(dispatchers.io) {
        Log.d("FIREBASE_CALL", "Delete Screen: $screen")
        scr.document(screen).delete().await()
        screenDao.deleteScreen(screen)
        cacheFlowMap.remove(screen)
        Unit
    }

    override fun clearCache() {
        cacheFlowMap.clear()
    }
}
