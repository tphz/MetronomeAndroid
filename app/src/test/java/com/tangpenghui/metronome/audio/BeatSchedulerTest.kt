package com.tangpenghui.metronome.audio

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BeatSchedulerTest {
    @Test fun `default bpm is 150`() {
        assertEquals(150, BeatScheduler(sampleRate = 44100).bpm)
    }

    @ParameterizedTest
    @ValueSource(ints = [60, 90, 120, 150, 180, 240])
    fun `samples per beat matches formula`(bpm: Int) {
        val s = BeatScheduler(sampleRate = 44100, bpm = bpm)
        val expected = (44100.0 * 60.0 / bpm).toInt()
        assertEquals(expected, s.samplesPerBeat)
    }

    @Test fun `bpm setter clamps to 60 minimum`() {
        val s = BeatScheduler()
        s.bpm = 30
        assertEquals(60, s.bpm)
    }

    @Test fun `bpm setter clamps to 240 maximum`() {
        val s = BeatScheduler()
        s.bpm = 300
        assertEquals(240, s.bpm)
    }

    @Test fun `first beat is heavy`() {
        val s = BeatScheduler()
        assertEquals(BeatType.HEAVY, s.beatType(1))
        assertEquals(BeatType.LIGHT, s.beatType(2))
        assertEquals(BeatType.HEAVY, s.beatType(3))
    }
}
