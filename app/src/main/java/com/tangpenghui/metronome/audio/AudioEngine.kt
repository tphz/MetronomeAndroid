package com.tangpenghui.metronome.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

class AudioEngine(
    private val sampleRate: Int = 44100,
    bpm: Int = 150
) {
    @Volatile var volume: Float = 0.3f
        private set

    @Volatile var bpm: Int = bpm
        set(value) {
            scheduler.bpm = value
            field = scheduler.bpm
        }

    val visualTrigger: Int
        get() = trigger.getAndSet(0)

    private val scheduler = BeatScheduler(sampleRate, bpm)
    private val trigger = AtomicInteger(0)
    private val beatCount = AtomicLong(0)
    private val beatCountdown = AtomicInteger(0)
    private val isRunning = AtomicInteger(0)

    private val synth = WaveformSynthesizer()
    private val heavySamples = synth.synthesizeHeavyWoodBlock(sampleRate)
    private val lightSamples = synth.synthesizeLightWoodBlock(sampleRate)
    private val endChimeSamples = synth.synthesizeEndChime(sampleRate)

    private data class ActiveSound(
        val samples: ShortArray,
        var pointer: Int,
        var startOffset: Int
    )

    private val activeSounds = mutableListOf<ActiveSound>()
    private val lock = Any()

    private var track: AudioTrack? = null
    private var renderThread: Thread? = null
    private val bufferSizeFrames = 1024

    private fun createTrack(): AudioTrack {
        val channelCount = 2
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val bufferSize = maxOf(minBuf, bufferSizeFrames * channelCount * 4)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        return AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun renderLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val t = track ?: return
        val channelCount = 2
        val buffer = FloatArray(bufferSizeFrames * channelCount)
        try {
            while (isRunning.get() == 1 || activeSounds.isNotEmpty()) {
                renderInto(buffer, bufferSizeFrames, channelCount)
                var written = 0
                while (written < buffer.size) {
                    val n = t.write(buffer, written, buffer.size - written, AudioTrack.WRITE_BLOCKING)
                    if (n < 0) break
                    written += n
                }
            }
        } catch (_: Throwable) {}
    }

    private fun renderInto(buffer: FloatArray, frames: Int, channelCount: Int) {
        for (i in 0 until frames * channelCount) buffer[i] = 0.0f

        var idx = 0
        if (isRunning.get() == 1) {
            while (idx < frames) {
                if (beatCountdown.get() <= 0) {
                    val beatIdx = beatCount.incrementAndGet()
                    val type = scheduler.beatType(beatIdx)
                    val samples = if (type == BeatType.HEAVY) heavySamples else lightSamples
                    synchronized(lock) {
                        activeSounds.add(ActiveSound(samples, 0, idx))
                    }
                    trigger.set(if (type == BeatType.HEAVY) 1 else 2)
                    beatCountdown.set(scheduler.samplesPerBeat)
                }
                val chunk = minOf(frames - idx, beatCountdown.get())
                beatCountdown.addAndGet(-chunk)
                idx += chunk
            }
        } else {
            beatCountdown.set(0)
        }

        val vol = volume
        val finished = mutableListOf<ActiveSound>()
        synchronized(lock) {
            val iter = activeSounds.iterator()
            while (iter.hasNext()) {
                val snd = iter.next()
                val samplesNeeded = frames - snd.startOffset
                val samplesAvailable = snd.samples.size - snd.pointer
                val toWrite = minOf(samplesNeeded, samplesAvailable)
                if (toWrite > 0) {
                    for (k in 0 until toWrite) {
                        val v = snd.samples[snd.pointer + k].toFloat() / Short.MAX_VALUE * vol
                        val base = (snd.startOffset + k) * channelCount
                        for (c in 0 until channelCount) buffer[base + c] += v
                    }
                    snd.pointer += toWrite
                    snd.startOffset = 0
                }
                if (snd.pointer >= snd.samples.size) finished.add(snd)
            }
            for (s in finished) activeSounds.remove(s)
        }
    }

    fun start() {
        if (track == null) {
            track = createTrack()
            track?.play()
        }
        beatCount.set(0)
        beatCountdown.set(0)
        isRunning.set(1)
        if (renderThread == null || renderThread?.isAlive != true) {
            renderThread = thread(name = "AudioRender", isDaemon = true) {
                renderLoop()
            }
        }
    }

    fun pause() { isRunning.set(0) }

    fun stop() {
        isRunning.set(0)
        synchronized(lock) {
            activeSounds.removeAll { it.samples === heavySamples || it.samples === lightSamples }
        }
    }

    fun playEndChime() {
        synchronized(lock) {
            activeSounds.removeAll { it.samples === heavySamples || it.samples === lightSamples }
            activeSounds.add(ActiveSound(endChimeSamples, 0, 0))
        }
    }

    fun setVolume(value: Float) { volume = value.coerceIn(0.0f, 1.0f) }

    fun shutdown() {
        isRunning.set(0)
        synchronized(lock) { activeSounds.clear() }
        track?.let {
            try { it.stop() } catch (_: Throwable) {}
            try { it.release() } catch (_: Throwable) {}
        }
        track = null
    }

    companion object { private const val TAG = "AudioEngine" }
}
