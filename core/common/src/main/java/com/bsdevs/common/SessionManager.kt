package com.bsdevs.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private val _onSessionCleared = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val onSessionCleared = _onSessionCleared.asSharedFlow()

    fun clearSession() {
        _onSessionCleared.tryEmit(Unit)
    }
}
