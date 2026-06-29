package com.tangpenghui.metronome.engine

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TimerEngineTest {
    @Test fun `countdown mode decrements timeLeft`() = runTest {
        var now = 0L
        val engine = TimerEngine(totalDurationSec = 30, timeSource = { now })
        engine.start(this) {}
        now = 5_000_000_000L
        engine.tick()
        assertEquals(25, engine.timeLeftSec)
        assertEquals(5, engine.timeElapsedSec)
        engine.stop()
    }

    @Test fun `free mode does not decrement timeLeft`() = runTest {
        var now = 0L
        val engine = TimerEngine(totalDurationSec = 0, timeSource = { now })
        engine.start(this) {}
        now = 10_000_000_000L
        engine.tick()
        assertEquals(0, engine.timeLeftSec)
        assertEquals(10, engine.timeElapsedSec)
        engine.stop()
    }

    @Test fun `countdown reaches zero invokes onFinished`() = runTest {
        var now = 0L
        var finished = false
        val engine = TimerEngine(totalDurationSec = 2, timeSource = { now })
        engine.onFinished = { finished = true }
        engine.start(this) {}
        now = 2_500_000_000L
        engine.tick()
        assertTrue(finished)
        engine.stop()
    }

    @Test fun `reset clears elapsed and left`() {
        val engine = TimerEngine(totalDurationSec = 30)
        engine.reset()
        assertEquals(0, engine.timeElapsedSec)
        assertEquals(30, engine.timeLeftSec)
    }
}
