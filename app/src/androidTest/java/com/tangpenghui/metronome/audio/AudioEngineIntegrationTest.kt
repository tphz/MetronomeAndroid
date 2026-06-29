package com.tangpenghui.metronome.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioEngineIntegrationTest {
    @Test fun engine_can_be_constructed_and_disposed_without_crashing() {
        val engine = AudioEngine(sampleRate = 44100, bpm = 150)
        engine.shutdown()
    }

    @Test fun volume_is_clamped_to_unit_interval() {
        val engine = AudioEngine()
        engine.setVolume(2.0f)
        assertEquals(1.0f, engine.volume, 1e-6f)
        engine.setVolume(-1.0f)
        assertEquals(0.0f, engine.volume, 1e-6f)
    }

    @Test fun bpm_is_clamped_via_scheduler() {
        val engine = AudioEngine()
        engine.bpm = 500
        assertEquals(240, engine.bpm)
    }
}
