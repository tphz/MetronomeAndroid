package com.tangpenghui.metronome.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.tangpenghui.metronome.MetronomeApp
import com.tangpenghui.metronome.R
import com.tangpenghui.metronome.audio.AudioEngine
import com.tangpenghui.metronome.controller.MetronomeController
import com.tangpenghui.metronome.controller.MetronomeState
import com.tangpenghui.metronome.controller.RunState
import com.tangpenghui.metronome.controller.TimerMode
import com.tangpenghui.metronome.data.ExerciseRepository
import com.tangpenghui.metronome.data.MetronomeDatabase
import com.tangpenghui.metronome.engine.TimerEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.atomic.AtomicLong

class MetronomeService : Service() {

    lateinit var controller: MetronomeController
        private set
    private lateinit var audio: AudioEngine
    private lateinit var timer: TimerEngine
    private lateinit var recorder: SessionRecorder
    private lateinit var mediaSession: MetronomeMediaSession

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessionStartMs = AtomicLong(0L)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Metronome:audio")
        wakeLock?.setReferenceCounted(false)
        audio = AudioEngine()
        timer = TimerEngine()
        recorder = SessionRecorder(ExerciseRepository(MetronomeDatabase.get(this).sessionDao()))
        controller = MetronomeController(audio, timer, scope, onSessionEnd = ::onSessionEnd)
        mediaSession = MetronomeMediaSession(this, scope)
        mediaSession.initialize(controller)
        observeStateForNotification()
    }

    private fun onSessionEnd(durationSec: Int, mode: TimerMode, completed: Boolean) {
        if (durationSec >= ExerciseRepository.MIN_VALID_DURATION_SEC && sessionStartMs.get() > 0) {
            recorder.record(durationSec, mode, completed, sessionStartMs.get())
        }
        sessionStartMs.set(0L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat(RunState.STOPPED, timeLeft = "00:00", bpm = 150)
        return START_STICKY
    }

    private fun startForegroundCompat(state: RunState, timeLeft: String, bpm: Int) {
        val notification = buildNotification(state, timeLeft, bpm)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeStateForNotification() {
        scope.launch {
            controller.state.collectLatest { st ->
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification(
                    st.runState, formatTime(st.timeLeftSec), st.bpm
                ))
                when (st.runState) {
                    RunState.RUNNING -> {
                        if (sessionStartMs.get() == 0L) {
                            sessionStartMs.set(System.currentTimeMillis() - st.timeElapsedSec * 1000L)
                        }
                        acquireWakeLock()
                    }
                    else -> releaseWakeLock()
                }
            }
        }
    }

    private fun acquireWakeLock() {
        wakeLock?.let { if (!it.isHeld) it.acquire() }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    private fun buildNotification(state: RunState, timeLeft: String, bpm: Int): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(this, 0, it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        return NotificationCompat.Builder(this, MetronomeApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_metronome)
            .setContentTitle("超慢跑节拍器 · $bpm BPM")
            .setContentText(when (state) {
                RunState.RUNNING -> "运行中 · 剩余 $timeLeft"
                RunState.PAUSED -> "已暂停 · $timeLeft"
                RunState.STOPPED -> "已停止"
            })
            .setContentIntent(contentIntent)
            .setOngoing(state != RunState.STOPPED)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder = MetronomeBinder(this)

    override fun onDestroy() {
        controller.shutdown()
        mediaSession.release()
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, MetronomeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun formatTime(sec: Int): String {
            val m = sec / 60; val s = sec % 60
            return "%02d:%02d".format(m, s)
        }
    }
}
