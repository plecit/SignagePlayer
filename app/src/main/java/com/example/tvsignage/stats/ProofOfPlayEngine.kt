package com.example.tvsignage.stats

import com.example.tvsignage.core.SecurePreferences
import com.example.tvsignage.core.SystemLogger
import com.example.tvsignage.xmds.XmdsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProofOfPlayEngine(
    private val statDao: StatDao,
    private val xmdsClient: XmdsClient,
    private val securePreferences: SecurePreferences
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private var activeLayoutId: Long = 0
    private var activeScheduleId: Long = 0
    private var layoutStartTime: Long = 0

    fun onLayoutStart(layoutId: Long, scheduleId: Long) {
        if (!securePreferences.statsEnabled) return
        flushCurrentLayout()

        activeLayoutId = layoutId
        activeScheduleId = scheduleId
        layoutStartTime = System.currentTimeMillis()
    }

    fun onLayoutEnd() {
        flushCurrentLayout()
    }

    private fun flushCurrentLayout() {
        if (activeLayoutId > 0 && layoutStartTime > 0) {
            val endTime = System.currentTimeMillis()
            val durationSec = ((endTime - layoutStartTime) / 1000).coerceAtLeast(1)

            val from = synchronized(dateFormat) { dateFormat.format(Date(layoutStartTime)) }
            val to = synchronized(dateFormat) { dateFormat.format(Date(endTime)) }

            val record = StatRecord(
                type = "layout",
                layoutId = activeLayoutId,
                scheduleId = activeScheduleId,
                mediaId = 0,
                fromDt = from,
                toDt = to,
                duration = durationSec
            )

            scope.launch {
                statDao.insert(record)
            }

            activeLayoutId = 0
            layoutStartTime = 0
        }
    }

    fun recordMediaPlayback(layoutId: Long, scheduleId: Long, mediaId: Long, durationSeconds: Long) {
        if (!securePreferences.statsEnabled || mediaId <= 0) return

        val now = System.currentTimeMillis()
        val start = now - (durationSeconds * 1000)

        val from = synchronized(dateFormat) { dateFormat.format(Date(start)) }
        val to = synchronized(dateFormat) { dateFormat.format(Date(now)) }

        val record = StatRecord(
            type = "media",
            layoutId = layoutId,
            scheduleId = scheduleId,
            mediaId = mediaId,
            fromDt = from,
            toDt = to,
            duration = durationSeconds.coerceAtLeast(1)
        )

        scope.launch {
            statDao.insert(record)
        }
    }

    suspend fun flushPendingStats(hardwareKey: String): Boolean {
        if (!securePreferences.statsEnabled) return true

        val pending = statDao.getPendingStats(100)
        if (pending.isEmpty()) return true

        SystemLogger.i(TAG, "Submitting ${pending.size} proof of play records to CMS")
        val success = xmdsClient.submitStats(hardwareKey, pending)
        if (success) {
            statDao.deleteByIds(pending.map { it.id })
            SystemLogger.i(TAG, "Successfully submitted and cleared ${pending.size} stat records")
            return true
        }
        return false
    }

    companion object {
        private const val TAG = "ProofOfPlayEngine"
    }
}
