package com.tangpenghui.metronome.data

import androidx.room.ColumnInfo

data class DayAggregate(
    val day: String,
    @ColumnInfo(name = "total_sec") val totalSec: Int,
    val cnt: Int
)
