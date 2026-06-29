package com.tangpenghui.metronome.controller

data class MetronomeState(
    val runState: RunState = RunState.STOPPED,
    val mode: TimerMode = TimerMode.MODE_30MIN,
    val customMinutes: Int = 20,
    val bpm: Int = 150,
    val volume: Float = 0.3f,
    val timeLeftSec: Int = 30 * 60,
    val timeElapsedSec: Int = 0,
    val totalDurationSec: Int = 30 * 60
)
