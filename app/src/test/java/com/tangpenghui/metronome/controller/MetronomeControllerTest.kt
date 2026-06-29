package com.tangpenghui.metronome.controller

import com.tangpenghui.metronome.audio.AudioEngine
import com.tangpenghui.metronome.engine.TimerEngine
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class MetronomeControllerTest {
    private fun newAudio(): AudioEngine = mock()

    @Test fun `initial state is stopped 30min 150bpm`() {
        val audio = newAudio()
        whenever(audio.bpm).thenReturn(150)
        val timer = TimerEngine(totalDurationSec = 30 * 60)
        val ctrl = MetronomeController(audio, timer)
        val s = ctrl.state.value
        assertEquals(RunState.STOPPED, s.runState)
        assertEquals(TimerMode.MODE_30MIN, s.mode)
        assertEquals(150, s.bpm)
    }

    @Test fun `start transitions to running`() {
        val ctrl = MetronomeController(newAudio(), TimerEngine())
        ctrl.start()
        assertEquals(RunState.RUNNING, ctrl.state.value.runState)
    }

    @Test fun `pause transitions to paused`() {
        val ctrl = MetronomeController(newAudio(), TimerEngine())
        ctrl.start()
        ctrl.pause()
        assertEquals(RunState.PAUSED, ctrl.state.value.runState)
    }

    @Test fun `stop from running returns to stopped`() {
        val ctrl = MetronomeController(newAudio(), TimerEngine())
        ctrl.start()
        ctrl.stop()
        assertEquals(RunState.STOPPED, ctrl.state.value.runState)
    }

    @Test fun `setMode is rejected while running`() {
        val ctrl = MetronomeController(newAudio(), TimerEngine())
        ctrl.start()
        val accepted = ctrl.setMode(TimerMode.MODE_45MIN)
        assertFalse(accepted)
        assertEquals(TimerMode.MODE_30MIN, ctrl.state.value.mode)
    }

    @Test fun `setMode while stopped is accepted`() {
        val ctrl = MetronomeController(newAudio(), TimerEngine())
        val accepted = ctrl.setMode(TimerMode.MODE_45MIN)
        assertTrue(accepted)
        assertEquals(TimerMode.MODE_45MIN, ctrl.state.value.mode)
        assertEquals(45 * 60, ctrl.state.value.totalDurationSec)
    }

    @Test fun `setBpm propagates to audio engine`() {
        val audio = newAudio()
        whenever(audio.bpm).thenReturn(180)
        val ctrl = MetronomeController(audio, TimerEngine())
        ctrl.setBpm(180)
        assertEquals(180, ctrl.state.value.bpm)
    }
}
