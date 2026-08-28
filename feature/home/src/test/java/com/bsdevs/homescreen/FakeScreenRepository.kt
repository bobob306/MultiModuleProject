package com.bsdevs.homescreen

import com.bsdevs.common.result.Result
import com.bsdevs.network.dto.ScreenDto
import com.bsdevs.network.repository.ScreenRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeScreenRepository : ScreenRepository {
    private val screenFlows = mutableMapOf<String, MutableStateFlow<Result<List<ScreenDto>>>>()

    fun emitScreenData(screen: String, data: List<ScreenDto>) {
        screenFlows.getOrPut(screen) { MutableStateFlow(Result.Loading) }.value = Result.Success(data)
    }

    fun emitError(screen: String, exception: Exception) {
        screenFlows.getOrPut(screen) { MutableStateFlow(Result.Loading) }.value = Result.Error(exception)
    }

    fun emitLoading(screen: String) {
        screenFlows.getOrPut(screen) { MutableStateFlow(Result.Loading) }.value = Result.Loading
    }

    override suspend fun getScreen(screen: String): Task<DocumentSnapshot> {
        throw UnsupportedOperationException("Not used in ViewModel")
    }

    override suspend fun getScreenFlow(screen: String): Flow<Result<List<ScreenDto>>> {
        return screenFlows.getOrPut(screen) { MutableStateFlow(Result.Loading) }
    }
}
