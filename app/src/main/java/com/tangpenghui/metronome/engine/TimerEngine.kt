package com.tangpenghui.metronome.engine

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
    private var job: Job? = null
    private var finishedEmitted = false

    fun start(scope: CoroutineScope, onTick: (TimerSnapshot) -> Unit) {
        if (isRunning) return
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
        if (!isRunning) return
        val now = timeSource.nanoTime()
        val dtSec = (now - lastTickNanos).toDouble() / 1_000_000_000.0
        lastTickNanos = now

        val newElapsed = (timeElapsedSec + dtSec).toInt()
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
        onTick(TimerSnapshot(timeLeftSec, timeElapsedSec, totalDurationSec))
    }

    fun stop() { isRunning = false; job?.cancel(); job = null }

    fun reset() {
        stop()
        timeElapsedSec = 0
        timeLeftSec = if (totalDurationSec == 0) 0 else totalDurationSec
        finishedEmitted = false
    }

    fun setTotalDuration(sec: Int) {
        totalDurationSec = sec
        if (!isRunning) {
            timeLeftSec = if (sec == 0) 0 else sec
            timeElapsedSec = 0
        } else {
            timeLeftSec = (sec - timeElapsedSec).coerceAtLeast(0)
        }
    }
}
