package com.tangpenghui.metronome.service

import android.util.Log
import com.tangpenghui.metronome.controller.TimerMode
import com.tangpenghui.metronome.data.ExerciseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SessionRecorder(
    private val repository: ExerciseRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    fun record(durationSec: Int, mode: TimerMode, completed: Boolean, startTimeMs: Long) {
        scope.launch {
            val result = repository.saveSession(durationSec, mode, completed, startTimeMs)
            result.exceptionOrNull()?.let {
                Log.w(TAG, "Save session failed (duration=$durationSec): ${it.message}")
            }
        }
    }

    companion object { private const val TAG = "SessionRecorder" }
}
