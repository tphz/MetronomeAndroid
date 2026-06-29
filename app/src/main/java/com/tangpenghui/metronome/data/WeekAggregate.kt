package com.tangpenghui.metronome.data

import androidx.room.ColumnInfo

data class WeekAggregate(
    val week: String,
    @ColumnInfo(name = "total_sec") val totalSec: Int,
    @ColumnInfo(name = "day_count") val dayCount: Int
)
