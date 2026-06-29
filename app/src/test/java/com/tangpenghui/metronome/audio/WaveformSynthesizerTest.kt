package com.tangpenghui.metronome.audio

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WaveformSynthesizerTest {
    @Test fun `heavy wood block has correct length`() {
        val s = WaveformSynthesizer()
        // 150ms @ 44.1kHz = 6615 samples
        assertEquals(6615, s.synthesizeHeavyWoodBlock(sampleRate = 44100).size)
    }

    @Test fun `heavy wood block decays to near zero at end`() {
        val s = WaveformSynthesizer()
        val samples = s.synthesizeHeavyWoodBlock()
        val tail = samples.takeLast(samples.size / 100)
        val maxTail = tail.maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue(maxTail < 1000, "Expected decay to near zero, but max tail = $maxTail")
    }

    @Test fun `light wood block has same length as heavy`() {
        val s = WaveformSynthesizer()
        assertEquals(s.synthesizeHeavyWoodBlock().size, s.synthesizeLightWoodBlock().size)
    }

    @Test fun `end chime has 2 second duration`() {
        val s = WaveformSynthesizer()
        assertEquals(88200, s.synthesizeEndChime().size)
    }

    @Test fun `end chime last 10 percent is faded to near silence`() {
        val s = WaveformSynthesizer()
        val samples = s.synthesizeEndChime()
        val tail = samples.takeLast(samples.size / 10)
        val maxTail = tail.maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue(maxTail < 5000, "Expected fade-out, but max tail = $maxTail")
    }
}
