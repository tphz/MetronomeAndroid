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

/**
 * 高精度音频引擎，负责实现“零漂移”节拍器核心逻辑。
 *
 * 使用 [AudioTrack] 的低延迟模式，并通过采样点计数 (Sample-accurate) 机制
 * 确保节拍在长时间运行下不产生系统时钟抖动带来的误差。
 *
 * @param sampleRate 音频采样率 (默认 44100Hz)
 * @param bpm 初始步频 (BPM)
 */
/**
 * 高精度音频引擎，负责实现“零漂移”节拍器核心逻辑。
 *
 * 使用 [AudioTrack] 的低延迟模式，并通过采样点计数 (Sample-accurate) 机制
 * 确保节拍在长时间运行下不产生系统时钟抖动带来的误差。
 *
 * @param sampleRate 音频采样率 (默认 44100Hz)
 * @param bpm 初始步频 (BPM)
 */
class AudioEngine(
    private val sampleRate: Int = 44100,
    bpm: Int = 150
) {
    /** 当前播放音量 (0.0 - 1.0) */
    @Volatile var volume: Float = 0.3f
        private set

    /** 当前步频，修改时会同步更新调度器 */
    @Volatile var bpm: Int = bpm
        set(value) {
            scheduler.bpm = value
            field = scheduler.bpm
        }

    /**
     * 视觉触发信号。
     * UI 层通过读取此值来驱动 LED 脉动动画。返回后会自动重置为 0。
     */
    val visualTrigger: Int
        get() = trigger.getAndSet(0)

    private val scheduler = BeatScheduler(sampleRate, bpm)
    /** 用于向 UI 发送节拍触发信号 (1: 重音/左脚, 2: 轻音/右脚) */
    private val trigger = AtomicInteger(0)
    /** 当前累计播放的节拍总数 */
    private val beatCount = AtomicLong(0)
    /** 距离下一个节拍还剩余多少个采样点 (核心：驱动零漂移的关键) */
    private val beatCountdown = AtomicInteger(0)
    /** 引擎运行状态 (1: 运行, 0: 已暂停/停止) */
    private val isRunning = AtomictingInt(0)

    // 音频合成器与预生成的波形数据
    private val synth = WaveformSynthesizer()
    private val heavySamples = synth.synthesizeHeavyWoodBlock(sampleRate) // 重音波形
    private val lightSamples = synth.synthesizeLightWoodBlock(sampleRate) // 轻音波形
    private val endChimeSamples = synth.synthesizeEndChime(sampleRate)   // 结束提示音

    /**
     * 表示正在播放中的一个声音实例（如某一次节拍的木鱼声）
     */
    private data class ActiveSound(
        val samples: ShortArray, // 原始 PCM 数据
        var pointer: Int,       // 当前播放到的采样点偏移
        var startOffset: Int    // 该声音在当前音频 Buffer 中的起始位置
    )

    /** 正在进行的活跃声音列表，通过锁保护以实现多线程混音 */
    private val activeSounds = mutableListOf<ActiveSound>()
    private val lock = Any()

    private var track: AudioTrack? = null
    private var renderThread: Thread? = null
    /** 每次渲染循环处理的帧数 */
    private val bufferSizeFrames = 1024


    /**
     * 配置并创建高性能 [AudioTrack] 实例。
     * 
     * 使用 {@code PERFORMANCE_MODE_LOW_latency} 以减少音频缓冲延迟，
     * 并使用 {@code ENCODING_PCM_FLOAT} 以获得更高的动态范围和计算精度。
     */
    private fun createTrack(): AudioTrack {
        val channelCount = 2
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val bufferSize = maxOf(minBuf, bufferSizeFrames * channelCount * 4)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
