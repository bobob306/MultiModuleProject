package com.bsdevs.common

import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

object FirebaseLogger {
    private val callCount = AtomicInteger(0)

    fun logCall(operation: String) {
        val count = callCount.incrementAndGet()
        Log.d("FIREBASE_CALL", "[$count] $operation")
    }

    fun getCount(): Int = callCount.get()
}
