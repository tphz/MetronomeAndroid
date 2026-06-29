package com.tangpenghui.metronome.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_sessions")
data class ExerciseSession(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "start_time_ms") val startTimeMs: Long,
    @ColumnInfo(name = "end_time_ms") val endTimeMs: Long,
    @ColumnInfo(name = "duration_sec") val durationSec: Int,
    @ColumnInfo(name = "mode") val mode: String,
    @ColumnInfo(name = "completed") val completed: Boolean
)
