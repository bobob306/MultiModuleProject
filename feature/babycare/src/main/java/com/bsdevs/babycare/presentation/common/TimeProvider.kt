package com.bsdevs.babycare.presentation.common

import android.os.SystemClock
import javax.inject.Inject

interface TimeProvider {
    fun currentTimeMillis(): Long
    fun elapsedRealtime(): Long
}

class DefaultTimeProvider @Inject constructor() : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}
