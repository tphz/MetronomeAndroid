package com.tangpenghui.metronome.controller

import com.tangpenghui.metronome.audio.AudioEngine
import com.tangpenghui.metronome.engine.TimerEngine
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MetronomeControllerTest {
    @Test fun `initial state is stopped 30min 150bpm`() {
        val audio = AudioEngine()
        val timer = TimerEngine(totalDurationSec = 30 * 60)
        val ctrl = MetronomeController(audio, timer)
        val s = ctrl.state.value
        assertEquals(RunState.STOPPED, s.runState)
        assertEquals(TimerMode.MODE_30MIN, s.mode)
        assertEquals(150, s.bpm)
    }

    @Test fun `start transitions to running`() {
        val ctrl = MetronomeController(AudioEngine(), TimerEngine())
        ctrl.start()
        assertEquals(RunState.RUNNING, ctrl.state.value.runState)
    }

    @Test fun `pause transitions to paused`() {
        val ctrl = MetronomeController(AudioEngine(), TimerEngine())
        ctrl.start()
        ctrl.pause()
        assertEquals(RunState.PAUSED, ctrl.state.value.runState)
    }

    @Test fun `stop from running returns to stopped`() {
        val ctrl = MetronomeController(AudioEngine(), TimerEngine())
        ctrl.start()
        ctrl.stop()
        assertEquals(RunState.STOPPED, ctrl.state.value.runState)
    }

    @Test fun `setMode is rejected while running`() {
        val ctrl = MetronomeController(AudioEngine(), TimerEngine())
        ctrl.start()
        val accepted = ctrl.setMode(TimerMode.MODE_45MIN)
        assertFalse(accepted)
        assertEquals(TimerMode.MODE_30MIN, ctrl.state.value.mode)
    }

    @Test fun `setMode while stopped is accepted`() {
        val ctrl = MetronomeController(AudioEngine(), TimerEngine())
        val accepted = ctrl.setMode(TimerMode.MODE_45MIN)
        assertTrue(accepted)
        assertEquals(TimerMode.MODE_45MIN, ctrl.state.value.mode)
        assertEquals(45 * 60, ctrl.state.value.totalDurationSec)
    }

    @Test fun `setBpm propagates to audio engine`() {
        val ctrl = MetronomeController(AudioEngine(), TimerEngine())
        ctrl.setBpm(180)
        assertEquals(180, ctrl.state.value.bpm)
    }
}
