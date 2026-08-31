package com.bsdevs.babycare.presentation.feeding

import com.bsdevs.babycare.presentation.common.TimeProvider
import com.bsdevs.common.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId

data class FeedingTimerState(
    val activityId: String? = null,
    val leftDuration: Long = 0,
    val rightDuration: Long = 0,
    val isLeftRunning: Boolean = false,
    val isRightRunning: Boolean = false,
    val startTime: String? = null,
    val date: String? = null
)

@Singleton
class FeedingTimerManager @Inject constructor(
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)
    private val _timerState = MutableStateFlow(FeedingTimerState())
    val timerState: StateFlow<FeedingTimerState> = _timerState.asStateFlow()

    private val timerJobs = mutableMapOf<FeedingSide, Job?>()
    private val baseDurations = mutableMapOf<FeedingSide, Long>().withDefault { 0L }

    fun toggleTimer(side: FeedingSide, activityId: String? = null) {
        val isRunning = when (side) {
            FeedingSide.LEFT -> _timerState.value.isLeftRunning
            FeedingSide.RIGHT -> _timerState.value.isRightRunning
        }

        if (isRunning) {
            pauseTimer(side)
        } else {
            startTimer(side, activityId)
        }
    }

    fun setSessionMetadata(startTime: String, date: String) {
        _timerState.update { it.copy(startTime = startTime, date = date) }
    }

    fun startTimer(side: FeedingSide, activityId: String? = null) {
        if (timerJobs[side]?.isActive == true) return

        val sessionStartTime = timeProvider.elapsedRealtime()
        val previousAccumulatedDuration = baseDurations.getValue(side)

        _timerState.update { state ->
            var newState = when (side) {
                FeedingSide.LEFT -> state.copy(isLeftRunning = true)
                FeedingSide.RIGHT -> state.copy(isRightRunning = true)
            }
            // Update activityId if provided and not already set
            if (activityId != null && newState.activityId == null) {
                newState = newState.copy(activityId = activityId)
            }

            // 🌟 PERSISTENT START TIME: If not already set, lock in the current time/date
            if (newState.startTime == null) {
                val now = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timeProvider.currentTimeMillis()),
                    ZoneId.systemDefault()
                )
                newState = newState.copy(
                    startTime = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                    date = now.toLocalDate().toString()
                )
            }
            newState
        }

        timerJobs[side] = scope.launch {
            while (true) {
                val actualSecondsElapsed = (timeProvider.elapsedRealtime() - sessionStartTime) / 1000
                val totalDuration = previousAccumulatedDuration + actualSecondsElapsed

                _timerState.update { state ->
                    when (side) {
                        FeedingSide.LEFT -> state.copy(leftDuration = totalDuration)
                        FeedingSide.RIGHT -> state.copy(rightDuration = totalDuration)
                    }
                }
                delay(1000L.milliseconds)
            }
        }
    }

    fun pauseTimer(side: FeedingSide) {
        timerJobs[side]?.cancel()
        timerJobs[side] = null

        val finalDuration = when (side) {
            FeedingSide.LEFT -> _timerState.value.leftDuration
            FeedingSide.RIGHT -> _timerState.value.rightDuration
        }
        baseDurations[side] = finalDuration

        _timerState.update { state ->
            when (side) {
                FeedingSide.LEFT -> state.copy(isLeftRunning = false)
                FeedingSide.RIGHT -> state.copy(isRightRunning = false)
            }
        }
    }

    fun setDuration(side: FeedingSide, duration: Long, activityId: String? = null) {
        baseDurations[side] = duration
        _timerState.update { state ->
            val newState = when (side) {
                FeedingSide.LEFT -> state.copy(leftDuration = duration)
                FeedingSide.RIGHT -> state.copy(rightDuration = duration)
            }
            if (activityId != null) newState.copy(activityId = activityId) else newState
        }
    }

    fun reset() {
        timerJobs.values.forEach { it?.cancel() }
        timerJobs.clear()
        baseDurations.clear()
        _timerState.value = FeedingTimerState()
    }

    fun isAnyTimerRunning(): Boolean {
        return _timerState.value.isLeftRunning || _timerState.value.isRightRunning
    }
}
