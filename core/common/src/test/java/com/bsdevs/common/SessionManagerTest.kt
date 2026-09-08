package com.bsdevs.common

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    @Test
    fun `clearSession emits Unit on onSessionCleared flow`() = runTest {
        val sessionManager = SessionManager()
        
        sessionManager.onSessionCleared.test {
            sessionManager.clearSession()
            assertEquals(Unit, awaitItem())
        }
    }

    @Test
    fun `clearSession can be called multiple times`() = runTest {
        val sessionManager = SessionManager()
        
        sessionManager.onSessionCleared.test {
            sessionManager.clearSession()
            assertEquals(Unit, awaitItem())
            
            sessionManager.clearSession()
            assertEquals(Unit, awaitItem())
        }
    }
}
