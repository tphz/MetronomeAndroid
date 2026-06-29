package com.tangpenghui.metronome.engine

import android.util.Log
import kotlinx.coroutines.*

data class TimerSnapshot(
    val timeLeftSec: Int,
    val timeElapsedSec: Int,
    val totalDurationSec: Int
)

class TimerEngine(
    var totalDurationSec: Int = 0,
    private val tickIntervalMs: Long = 100,
    private val timeSource: SystemTimeSource = RealSystemTimeSource
) {
    @Volatile var timeElapsedSec: Int = 0
        private set
    @Volatile var timeLeftSec: Int = if (totalDurationSec == 0) 0 else totalDurationSec
        private set
    @Volatile var isRunning: Boolean = false
        private set

    var onFinished: (() -> Unit)? = null

    private var lastTickNanos: Long = 0L
    private var elapsedNanos: Long = 0L
    private var job: Job? = null
    private var finishedEmitted = false

    fun start(scope: CoroutineScope, onTick: (TimerSnapshot) -> Unit) {
        if (isRunning) return
        Log.d(TAG, "start: totalDurationSec=$totalDurationSec elapsedNanos=$elapsedNanos")
        isRunning = true
        lastTickNanos = timeSource.nanoTime()
        finishedEmitted = false
        job = scope.launch {
            while (isActive && isRunning) {
                delay(tickIntervalMs)
                tick(onTick)
            }
        }
    }

    fun tick(onTick: (TimerSnapshot) -> Unit = {}) {
        if (!isRunning) {
            Log.w(TAG, "tick: not running, skip")
            return
        }
        val now = timeSource.nanoTime()
        val dtNanos = now - lastTickNanos
        lastTickNanos = now
        elapsedNanos += dtNanos

        val newElapsed = (elapsedNanos / 1_000_000_000L).toInt()
        timeElapsedSec = newElapsed

        if (totalDurationSec > 0) {
            val newLeft = (totalDurationSec - newElapsed).coerceAtLeast(0)
            timeLeftSec = newLeft
            if (newLeft == 0 && !finishedEmitted) {
                finishedEmitted = true
                isRunning = false
                onFinished?.invoke()
            }
        } else {
            timeLeftSec = 0
        }
        Log.d(TAG, "tick: elapsed=$newElapsed left=$timeLeftSec")
        onTick(TimerSnapshot(timeLeftSec, timeElapsedSec, totalDurationSec))
    }

    fun stop() { isRunning = false; job?.cancel(); job = null }

    fun reset() {
        stop()
        timeElapsedSec = 0
        timeLeftSec = if (totalDurationSec == 0) 0 else totalDurationSec
        elapsedNanos = 0L
        finishedEmitted = false
    }

    fun setTotalDuration(sec: Int) {
        totalDurationSec = sec
        if (!isRunning) {
            timeLeftSec = if (sec == 0) 0 else sec
            timeElapsedSec = 0
            elapsedNanos = 0L
        } else {
            timeLeftSec = (sec - timeElapsedSec).coerceAtLeast(0)
        }
    }

    companion object { private const val TAG = "TimerEngine" }
}
