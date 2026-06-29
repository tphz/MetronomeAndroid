package com.tangpenghui.metronome.service

import android.os.Binder
import com.tangpenghui.metronome.controller.MetronomeController

class MetronomeBinder(private val service: MetronomeService) : Binder() {
    fun controller(): MetronomeController = service.controller
    fun service(): MetronomeService = service
}
