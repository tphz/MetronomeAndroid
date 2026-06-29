package com.tangpenghui.metronome.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class WaveformSynthesizer {
    companion object {
        const val SOUND_DURATION_SEC = 0.15
        const val END_CHIME_DURATION_SEC = 2.0
        const val HEAVY_F1 = 850.0
        const val HEAVY_F2 = 1275.0
        const val HEAVY_ALPHA = 30.0
        const val HEAVY_AMP = 0.5
        const val LIGHT_F1 = 680.0
        const val LIGHT_F2 = 1020.0
        const val LIGHT_ALPHA = 45.0
        const val LIGHT_AMP = 0.32
        const val HARMONIC_MIX = 0.15
        const val CHIME_AMP = 0.22
        const val CHIME_DECAY = 1.8
        const val C5 = 523.25
        const val E5 = 659.25
        const val G5 = 784.00
    }

    fun synthesizeHeavyWoodBlock(sampleRate: Int = 44100): ShortArray {
        val n = (sampleRate * SOUND_DURATION_SEC).toInt()
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            val env = exp(-HEAVY_ALPHA * t).toFloat()
            val s = HEAVY_AMP * env *
                (sin(2 * PI * HEAVY_F1 * t) + HARMONIC_MIX * sin(2 * PI * HEAVY_F2 * t))
            (s * Short.MAX_VALUE).toInt().toShort()
        }
    }

    fun synthesizeLightWoodBlock(sampleRate: Int = 44100): ShortArray {
        val n = (sampleRate * SOUND_DURATION_SEC).toInt()
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            val env = exp(-LIGHT_ALPHA * t).toFloat()
            val s = LIGHT_AMP * env *
                (sin(2 * PI * LIGHT_F1 * t) + HARMONIC_MIX * sin(2 * PI * LIGHT_F2 * t))
            (s * Short.MAX_VALUE).toInt().toShort()
        }
    }

    fun synthesizeEndChime(sampleRate: Int = 44100): ShortArray {
        val n = (sampleRate * END_CHIME_DURATION_SEC).toInt()
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            var s = 0.0
            if (t >= 0f) s += CHIME_AMP * exp(-CHIME_DECAY * t) * sin(2 * PI * C5 * t)
            val te = t - 0.15f
            if (te >= 0f) s += CHIME_AMP * exp(-CHIME_DECAY * te) * sin(2 * PI * E5 * te)
            val tg = t - 0.30f
            if (tg >= 0f) s += CHIME_AMP * exp(-CHIME_DECAY * tg) * sin(2 * PI * G5 * tg)
            (s * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
