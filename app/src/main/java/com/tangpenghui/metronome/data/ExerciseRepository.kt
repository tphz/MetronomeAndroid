package com.tangpenghui.metronome.data

import com.tangpenghui.metronome.controller.TimerMode
import kotlinx.coroutines.flow.Flow
import java.time.*
import java.time.temporal.WeekFields
import java.util.Locale

class ExerciseRepository(private val dao: SessionDao) {
    companion object { const val MIN_VALID_DURATION_SEC = 180 }

    fun observeAllSessions(): Flow<List<ExerciseSession>> = dao.observeAllValidSessions()

    suspend fun saveSession(
        durationSec: Int, mode: TimerMode, completed: Boolean, startTimeMs: Long
    ): Result<Unit> = runCatching {
        require(durationSec >= MIN_VALID_DURATION_SEC) {
            "Session duration $durationSec < 180s, filtered out"
        }
        dao.insert(ExerciseSession(
            startTimeMs = startTimeMs,
            endTimeMs = startTimeMs + durationSec * 1000L,
            durationSec = durationSec,
            mode = mode.name,
            completed = completed
        ))
        Unit
    }

    suspend fun aggregateMonth(year: Int, month: Int): List<DayAggregate> {
        val zone = ZoneId.systemDefault()
        val start = YearMonth.of(year, month).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = YearMonth.of(year, month).plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.aggregateMonth(start, end)
    }

    suspend fun aggregateWeek(year: Int, isoWeek: Int): List<DayAggregate> {
        val zone = ZoneId.systemDefault()
        val wf = WeekFields.of(Locale.getDefault())
        val firstDay = LocalDate.now()
            .with(wf.weekBasedYear(), year.toLong())
            .with(wf.weekOfWeekBasedYear(), isoWeek.toLong())
            .with(wf.firstDayOfWeek)
        val startMs = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = firstDay.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.aggregateMonth(startMs, endMs)
    }

    suspend fun getStreakDays(): Int {
        val days = dao.recentActiveDays(365).toSet()
        if (days.isEmpty()) return 0
        val zone = ZoneId.systemDefault()
        var d = LocalDate.now(zone)
        if (d.toString() !in days) d = d.minusDays(1)
        var streak = 0
        while (d.toString() in days) {
            streak++
            d = d.minusDays(1)
        }
        return streak
    }
}
