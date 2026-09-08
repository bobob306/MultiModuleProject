package com.bsdevs.common.result

import app.cash.turbine.test
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultTest {

    @Test
    fun `asResult emits Loading then Success`() = runTest {
        flowOf("data").asResult().test {
            assertEquals(Result.Loading, awaitItem())
            val success = awaitItem() as Result.Success<String>
            assertEquals("data", success.data)
            awaitComplete()
        }
    }

    @Test
    fun `asResult emits Loading then Error on failure`() = runTest {
        val exception = RuntimeException("Test failure")
        flow<String> { throw exception }.asResult().test {
            assertEquals(Result.Loading, awaitItem())
            val error = awaitItem() as Result.Error
            assertEquals(exception, error.exception)
            awaitComplete()
        }
    }

    @Test
    fun `asResult emits multiple successes`() = runTest {
        flowOf("a", "b").asResult().test {
            assertEquals(Result.Loading, awaitItem())
            assertEquals("a", (awaitItem() as Result.Success).data)
            assertEquals("b", (awaitItem() as Result.Success).data)
            awaitComplete()
        }
    }
}
