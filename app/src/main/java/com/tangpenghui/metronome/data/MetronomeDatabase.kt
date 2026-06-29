package com.tangpenghui.metronome.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ExerciseSession::class], version = 1, exportSchema = false)
abstract class MetronomeDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var instance: MetronomeDatabase? = null

        fun get(context: Context): MetronomeDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MetronomeDatabase::class.java,
                "metronome.db"
            ).build().also { instance = it }
        }
    }
}
