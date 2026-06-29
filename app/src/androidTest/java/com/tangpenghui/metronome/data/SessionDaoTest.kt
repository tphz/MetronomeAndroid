package com.tangpenghui.metronome.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDaoTest {
    private lateinit var db: MetronomeDatabase
    private lateinit var dao: SessionDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MetronomeDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.sessionDao()
    }

    @After fun teardown() { db.close() }

    @Test fun insert_and_observe_returns_session() = runBlocking {
        val now = System.currentTimeMillis()
        val id = dao.insert(ExerciseSession(startTimeMs = now, endTimeMs = now + 600_000, durationSec = 600, mode = "MODE_30MIN", completed = true))
        val list = dao.observeAllValidSessions().first()
        assertEquals(1, list.size)
        assertEquals(id, list[0].id)
    }

    @Test fun short_sessions_are_filtered_out() = runBlocking {
        val now = System.currentTimeMillis()
        dao.insert(ExerciseSession(startTimeMs = now, endTimeMs = now + 60_000, durationSec = 60, mode = "MODE_FREE", completed = false))
        dao.insert(ExerciseSession(startTimeMs = now, endTimeMs = now + 300_000, durationSec = 300, mode = "MODE_FREE", completed = false))
        val list = dao.observeAllValidSessions().first()
        assertEquals(1, list.size)
        assertEquals(300, list[0].durationSec)
    }

    @Test fun aggregate_month_groups_by_day() = runBlocking {
        val day1 = 1_700_000_000_000L
        val day2 = day1 + 86_400_000L
        dao.insert(ExerciseSession(startTimeMs = day1, endTimeMs = day1 + 1_800_000, durationSec = 1800, mode = "MODE_30MIN", completed = true))
        dao.insert(ExerciseSession(startTimeMs = day1 + 3_600_000, endTimeMs = day1 + 5_400_000, durationSec = 1800, mode = "MODE_30MIN", completed = true))
        dao.insert(ExerciseSession(startTimeMs = day2, endTimeMs = day2 + 1_800_000, durationSec = 1800, mode = "MODE_45MIN", completed = true))
        val agg = dao.aggregateMonth(day1 - 1, day2 + 86_400_000)
        assertEquals(2, agg.size)
        assertEquals(5400, agg.sumOf { it.totalSec })
    }
}
