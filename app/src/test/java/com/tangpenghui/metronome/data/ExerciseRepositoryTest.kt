package com.tangpenghui.metronome.data

import com.tangpenghui.metronome.controller.TimerMode
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class ExerciseRepositoryTest {
    private val dao: SessionDao = mock()
    private val repo = ExerciseRepository(dao)

    @Test fun `sessions under 180s are rejected`() = runTest {
        val result = repo.saveSession(60, TimerMode.MODE_FREE, false, 1_700_000_000_000L)
        assertTrue(result.isFailure)
        verify(dao, never()).insert(any())
    }

    @Test fun `sessions at 180s are accepted with computed end time`() = runTest {
        val start = 1_700_000_000_000L
        val result = repo.saveSession(180, TimerMode.MODE_30MIN, true, start)
        assertTrue(result.isSuccess)
        verify(dao).insert(argThat { s ->
            s.startTimeMs == start &&
            s.endTimeMs == start + 180_000L &&
            s.durationSec == 180 &&
            s.mode == "MODE_30MIN" &&
            s.completed
        })
    }

    @Test fun `observeAllSessions delegates to dao`() = runTest {
        val fake = flowOf(emptyList<ExerciseSession>())
        whenever(dao.observeAllValidSessions()).thenReturn(fake)
        assertSame(fake, repo.observeAllSessions())
    }

    @Test fun `getStreakDays returns 0 when empty`() = runTest {
        whenever(dao.recentActiveDays(365)).thenReturn(emptyList())
        assertEquals(0, repo.getStreakDays())
    }

    @Test fun `getStreakDays counts consecutive days`() = runTest {
        val today = java.time.LocalDate.now()
        whenever(dao.recentActiveDays(365)).thenReturn(listOf(
            today.toString(),
            today.minusDays(1).toString(),
            today.minusDays(2).toString()
        ))
        assertEquals(3, repo.getStreakDays())
    }
}
