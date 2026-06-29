package com.tangpenghui.metronome.controller

enum class TimerMode(val displayName: String) {
    MODE_30MIN("30分钟"),
    MODE_45MIN("45分钟"),
    MODE_FREE("自由模式"),
    MODE_CUSTOM("自定义");

    fun toMinutes(customMinutes: Int): Int = when (this) {
        MODE_30MIN -> 30
        MODE_45MIN -> 45
        MODE_FREE -> 0
        MODE_CUSTOM -> customMinutes
    }

    companion object {
        fun fromMinutes(min: Int): TimerMode = when (min) {
            30 -> MODE_30MIN
            45 -> MODE_45MIN
            0 -> MODE_FREE
            else -> MODE_CUSTOM
        }
    }
}
