package com.tangpenghui.metronome

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class MetronomeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID, "节拍器运行", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "节拍器后台运行时显示"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "metronome_running"
        lateinit var instance: MetronomeApp
            private set
    }
}
