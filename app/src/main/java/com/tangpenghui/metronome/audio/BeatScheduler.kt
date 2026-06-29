package com.tangpenghui.metronome.audio

import kotlin.math.roundToInt

class BeatScheduler(
    private val sampleRate: Int = 44100,
    bpm: Int = 150
) {
    var bpm: Int = bpm
        set(value) { field = value.coerceIn(60, 240) }

    val samplesPerBeat: Int
        get() = (sampleRate * 60.0 / bpm).roundToInt()

    fun beatType(beatIndex: Long): BeatType =
        if (beatIndex % 2L == 1L) BeatType.HEAVY else BeatType.LIGHT
}
