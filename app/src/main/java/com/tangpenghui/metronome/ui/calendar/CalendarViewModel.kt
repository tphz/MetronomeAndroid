package com.tangpenghui.metronome.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangpenghui.metronome.data.ExerciseRepository
import com.tangpenghui.metronome.data.ExerciseSession
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth

data class CalendarUiState(
    val monthDays: Map<String, Int> = emptyMap(),
    val streakDays: Int = 0,
    val totalDaysThisMonth: Int = 0,
    val totalSecondsThisMonth: Int = 0
)

class CalendarViewModel(private val repository: ExerciseRepository) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAllSessions().collect { sessions ->
                _state.value = _state.value.copy(streakDays = computeStreak(sessions))
            }
        }
        refreshMonth(YearMonth.now().year, YearMonth.now().monthValue)
    }

    fun refreshMonth(year: Int, month: Int) {
        viewModelScope.launch {
            val agg = repository.aggregateMonth(year, month)
            _state.value = _state.value.copy(
                monthDays = agg.associate { it.day to it.totalSec },
                totalDaysThisMonth = agg.size,
                totalSecondsThisMonth = agg.sumOf { it.totalSec }
            )
        }
    }

    private fun computeStreak(sessions: List<ExerciseSession>): Int {
        if (sessions.isEmpty()) return 0
        val days = sessions.map {
            Instant.ofEpochMilli(it.startTimeMs).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()
        var d = LocalDate.now()
        if (d !in days) d = d.minusDays(1)
        var streak = 0
        while (d in days) { streak++; d = d.minusDays(1) }
        return streak
    }
}
