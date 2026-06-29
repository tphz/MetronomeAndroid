package com.tangpenghui.metronome.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Suppress("RoomWarnings")
@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: ExerciseSession): Long

    @Query("SELECT * FROM exercise_sessions WHERE duration_sec >= 180 ORDER BY start_time_ms DESC")
    fun observeAllValidSessions(): Flow<List<ExerciseSession>>

    @Query("""
        SELECT strftime('%Y-%m-%d', start_time_ms/1000, 'unixepoch', 'localtime') AS day,
               SUM(duration_sec) AS total_sec,
               COUNT(*) AS cnt
        FROM exercise_sessions
        WHERE start_time_ms BETWEEN :fromMs AND :toMs
        GROUP BY day
    """)
    suspend fun aggregateMonth(fromMs: Long, toMs: Long): List<DayAggregate>

    @Query("""
        SELECT strftime('%Y-%W', start_time_ms/1000, 'unixepoch', 'localtime') AS week,
               SUM(duration_sec) AS total_sec,
               COUNT(DISTINCT strftime('%Y-%m-%d', start_time_ms/1000, 'unixepoch', 'localtime')) AS day_count
        FROM exercise_sessions
        WHERE start_time_ms BETWEEN :fromMs AND :toMs
        GROUP BY week
    """)
    suspend fun aggregateWeek(fromMs: Long, toMs: Long): List<WeekAggregate>

    @Query("""
        SELECT DISTINCT strftime('%Y-%m-%d', start_time_ms/1000, 'unixepoch', 'localtime') AS day
        FROM exercise_sessions
        WHERE duration_sec >= 180
        ORDER BY day DESC
        LIMIT :limit
    """)
    suspend fun recentActiveDays(limit: Int): List<String>
}
