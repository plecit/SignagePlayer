package com.example.tvsignage.stats

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stat_records")
data class StatRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "layout" or "media"
    val layoutId: Long,
    val scheduleId: Long,
    val mediaId: Long = 0,
    val fromDt: String,
    val toDt: String,
    val duration: Long,
    val createdAt: Long = System.currentTimeMillis()
)
