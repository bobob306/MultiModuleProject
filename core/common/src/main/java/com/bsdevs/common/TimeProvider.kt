package com.bsdevs.common

import android.os.SystemClock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

interface TimeProvider {
    fun currentLocalDate(): LocalDate
    fun currentLocalTime(): LocalTime
    fun currentTimeMillis(): Long
    fun elapsedRealtime(): Long
}

class DefaultTimeProvider @Inject constructor() : TimeProvider {
    override fun currentLocalDate(): LocalDate = LocalDate.now()
    override fun currentLocalTime(): LocalTime = LocalTime.now()
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}
