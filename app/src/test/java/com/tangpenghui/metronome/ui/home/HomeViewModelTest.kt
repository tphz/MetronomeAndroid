package com.tangpenghui.metronome.ui.home

import com.tangpenghui.metronome.audio.AudioEngine
import com.tangpenghui.metronome.controller.MetronomeController
import com.tangpenghui.metronome.controller.RunState
import com.tangpenghui.metronome.controller.TimerMode
import com.tangpenghui.metronome.engine.TimerEngine
import com.tangpenghui.metronome.service.MetronomeBinder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class HomeViewModelTest {
    private val audio = mock<AudioEngine>()
    private val timer = TimerEngine()
    private val ctrl = MetronomeController(audio, timer)

    private fun fakeBinder(): MetronomeBinder {
        val binder = mock<MetronomeBinder>()
        whenever(binder.controller()).thenReturn(ctrl)
        return binder
    }

    @Test fun `start delegates to controller`() = runTest {
        val vm = HomeViewModel(fakeBinder())
        vm.start()
        assertEquals(RunState.RUNNING, vm.state.first().runState)
    }

    @Test fun `setBpm delegates to controller`() = runTest {
        whenever(audio.bpm).thenReturn(180)
        val vm = HomeViewModel(fakeBinder())
        vm.setBpm(180)
        assertEquals(180, vm.state.first().bpm)
    }

    @Test fun `setMode delegates to controller`() = runTest {
        val vm = HomeViewModel(fakeBinder())
        vm.setMode(TimerMode.MODE_45MIN)
        assertEquals(TimerMode.MODE_45MIN, vm.state.first().mode)
    }
}
