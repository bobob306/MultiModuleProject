package com.bsdevs.babycare.presentation.common

import android.os.SystemClock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

interface TimeProvider {
    fun currentTimeMillis(): Long
    fun elapsedRealtime(): Long
    fun currentLocalDate(): LocalDate
    fun currentLocalTime(): LocalTime
}

class DefaultTimeProvider @Inject constructor() : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
    override fun currentLocalDate(): LocalDate = LocalDate.now()
    override fun currentLocalTime(): LocalTime = LocalTime.now()
}
