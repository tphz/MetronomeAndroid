package com.tangpenghui.metronome.audio

class AudioEngine(sampleRate: Int = 44100, bpm: Int = 150) {
    var volume: Float = 0.3f; private set
    var bpm: Int = bpm
    fun start() {}; fun pause() {}; fun stop() {};
    fun playEndChime() {}; fun setVolume(v: Float) { volume = v.coerceIn(0f,1f) }
    fun shutdown() {}
}
