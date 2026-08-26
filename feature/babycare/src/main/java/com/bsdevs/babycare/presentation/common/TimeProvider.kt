package com.bsdevs.babycare.presentation.common

import android.os.SystemClock
import javax.inject.Inject

interface TimeProvider {
    fun elapsedRealtime(): Long
}

class DefaultTimeProvider @Inject constructor() : TimeProvider {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}
