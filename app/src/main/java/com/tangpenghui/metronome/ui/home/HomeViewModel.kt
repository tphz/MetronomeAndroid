package com.tangpenghui.metronome.ui.home

import androidx.lifecycle.ViewModel
import com.tangpenghui.metronome.controller.MetronomeController
import com.tangpenghui.metronome.controller.MetronomeState
import com.tangpenghui.metronome.controller.TimerMode
import com.tangpenghui.metronome.service.MetronomeBinder
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(private val binder: MetronomeBinder) : ViewModel() {
    private val controller: MetronomeController get() = binder.controller()
    val state: StateFlow<MetronomeState> get() = controller.state

    fun start() = controller.start()
    fun pause() = controller.pause()
    fun stop() = controller.stop()
    fun setBpm(bpm: Int) = controller.setBpm(bpm)
    fun setMode(mode: TimerMode, customMinutes: Int? = null) = controller.setMode(mode, customMinutes)
    fun setVolume(v: Float) = controller.setVolume(v)
}
