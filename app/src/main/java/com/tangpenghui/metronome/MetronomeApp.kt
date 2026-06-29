package com.tangpenghui.metronome

import android.app.Application

class MetronomeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    companion object {
        lateinit var instance: MetronomeApp
            private set
    }
}
