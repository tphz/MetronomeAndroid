package com.tangpenghui.metronome.controller

import com.tangpenghui.metronome.audio.AudioEngine
import com.tangpenghui.metronome.engine.TimerEngine
import com.tangpenghui.metronome.engine.TimerSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MetronomeController(
    private val audio: AudioEngine,
    private val timer: TimerEngine,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val onSessionEnd: (durationSec: Int, mode: TimerMode, completed: Boolean) -> Unit = { _, _, _ -> }
) {
    private val _state = MutableStateFlow(MetronomeState())
    val state: StateFlow<MetronomeState> = _state.asStateFlow()

    init {
        timer.setTotalDuration(_state.value.totalDurationSec)
        timer.onFinished = ::handleCountdownFinished
    }

    fun start() {
        if (_state.value.runState == RunState.RUNNING) return
        _state.value = _state.value.copy(runState = RunState.RUNNING)
        audio.start()
        timer.start(scope) { snap -> onTimerTick(snap) }
    }

    fun pause() {
        if (_state.value.runState != RunState.RUNNING) return
        _state.value = _state.value.copy(runState = RunState.PAUSED)
        audio.pause()
        timer.stop()
    }

    fun stop() {
        val current = _state.value
        if (current.runState == RunState.STOPPED) return
        val elapsed = current.timeElapsedSec
        timer.stop()
        audio.stop()
        _state.value = current.copy(
            runState = RunState.STOPPED,
            timeElapsedSec = 0,
            timeLeftSec = current.totalDurationSec
        )
        timer.reset()
        onSessionEnd(elapsed, current.mode, false)
    }

    fun setMode(mode: TimerMode, customMinutes: Int? = null): Boolean {
        if (_state.value.runState == RunState.RUNNING) return false
        val cm = customMinutes ?: _state.value.customMinutes
        val newTotal = mode.toMinutes(cm) * 60
        _state.value = _state.value.copy(
            mode = mode, customMinutes = cm,
            totalDurationSec = newTotal, timeLeftSec = newTotal
        )
        timer.setTotalDuration(newTotal)
        return true
    }

    fun setBpm(bpm: Int) {
        audio.bpm = bpm
        _state.value = _state.value.copy(bpm = audio.bpm)
    }

    fun setVolume(v: Float) {
        audio.setVolume(v)
        _state.value = _state.value.copy(volume = audio.volume)
    }

    private fun onTimerTick(snap: TimerSnapshot) {
        _state.value = _state.value.copy(
            timeLeftSec = snap.timeLeftSec,
            timeElapsedSec = snap.timeElapsedSec,
            totalDurationSec = snap.totalDurationSec
        )
    }

    private fun handleCountdownFinished() {
        val current = _state.value
        val elapsed = current.timeElapsedSec
        audio.stop()
        audio.playEndChime()
        _state.value = current.copy(
            runState = RunState.STOPPED,
            timeElapsedSec = 0,
            timeLeftSec = current.totalDurationSec
        )
        timer.reset()
        onSessionEnd(elapsed, current.mode, true)
    }

    fun shutdown() {
        timer.stop()
        audio.shutdown()
        scope.cancel()
    }
}
