package com.example.tvsignage.stats

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: StatRecord): Long

    @Query("SELECT * FROM stat_records ORDER BY id ASC LIMIT :limit")
    suspend fun getPendingStats(limit: Int = 100): List<StatRecord>

    @Query("DELETE FROM stat_records WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM stat_records")
    suspend fun getCount(): Int
}
