package com.example.tvsignage.schedule

import com.example.tvsignage.core.SystemLogger
import com.example.tvsignage.files.LocalStorageManager
import com.example.tvsignage.xmds.models.ScheduleData
import com.example.tvsignage.xmds.models.ScheduleEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleManager(
    private val storageManager: LocalStorageManager
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private var currentScheduleData: ScheduleData? = null
    private var lastActiveLayoutId: Long = 0L
    private var rotationIndex = 0

    fun updateSchedule(data: ScheduleData) {
        currentScheduleData = data
        SystemLogger.i(TAG, "Schedule updated. DefaultLayout=${data.defaultLayoutId}, Events=${data.events.size}")
    }

    fun getActiveLayout(now: Date = Date()): Long {
        val schedule = currentScheduleData ?: return 0L
        val activeEvents = mutableListOf<ScheduleEvent>()

        for (event in schedule.events) {
            if (isEventActive(event, now)) {
                // Ensure layout XLF is present
                val xlfFile = storageManager.getFile("${event.layoutId}.xlf")
                if (xlfFile.exists() && xlfFile.length() > 0) {
                    activeEvents.add(event)
                }
            }
        }

        if (activeEvents.isEmpty()) {
            val defaultId = schedule.defaultLayoutId
            val defaultFile = storageManager.getFile("$defaultId.xlf")
            return if (defaultFile.exists()) defaultId else 0L
        }

        // Sort by priority descending
        activeEvents.sortByDescending { it.priority }
        val highestPriority = activeEvents.first().priority
        val candidateEvents = activeEvents.filter { it.priority == highestPriority }

        if (candidateEvents.size == 1) {
            val chosen = candidateEvents.first().layoutId
            lastActiveLayoutId = chosen
            return chosen
        }

        // Round robin rotation among candidates with same priority
        rotationIndex = (rotationIndex + 1) % candidateEvents.size
        val chosen = candidateEvents[rotationIndex].layoutId
        lastActiveLayoutId = chosen
        return chosen
    }

    private fun isEventActive(event: ScheduleEvent, now: Date): Boolean {
        return try {
            val from = if (event.fromDt.isNotBlank()) dateFormat.parse(event.fromDt) else null
            val to = if (event.toDt.isNotBlank()) dateFormat.parse(event.toDt) else null

            if (from != null && now.before(from)) return false
            if (to != null && now.after(to)) return false
            true
        } catch (e: Exception) {
            true // If parse fails, treat as always active
        }
    }

    companion object {
        private const val TAG = "ScheduleManager"
    }
}
