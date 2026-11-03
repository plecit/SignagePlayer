package com.example.tvsignage.stats

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [StatRecord::class], version = 1, exportSchema = false)
abstract class StatDatabase : RoomDatabase() {

    abstract fun statDao(): StatDao

    companion object {
        @Volatile
        private var INSTANCE: StatDatabase? = null

        fun getInstance(context: Context): StatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StatDatabase::class.java,
                    "tv_signage_stats.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
