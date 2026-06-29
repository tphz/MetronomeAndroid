package com.tangpenghui.metronome.engine

fun interface SystemTimeSource { fun nanoTime(): Long }
object RealSystemTimeSource : SystemTimeSource { override fun nanoTime(): Long = System.nanoTime() }
