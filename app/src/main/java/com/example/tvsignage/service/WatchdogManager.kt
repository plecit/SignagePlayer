package com.example.tvsignage.service

import android.os.Handler
import android.os.Looper
import com.example.tvsignage.core.SystemLogger

/**
 * Internal watchdog to detect stalled player threads.
 * If no heartbeat is received within the timeout window, triggers a recovery action.
 */
class WatchdogManager(
    private val timeoutMs: Long = 30_000L,
    private val onHangDetected: () -> Unit
) {

    private val handler = Handler(Looper.getMainLooper())
    private var lastHeartbeatTime = System.currentTimeMillis()
    private var isRunning = false

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            val elapsed = System.currentTimeMillis() - lastHeartbeatTime
            if (elapsed > timeoutMs) {
                SystemLogger.e("WatchdogManager", "Playback hang detected! No heartbeat for ${elapsed / 1000}s. Triggering recovery...")
                onHangDetected()
            }
            handler.postDelayed(this, timeoutMs / 2)
        }
    }

    fun start() {
        isRunning = true
        lastHeartbeatTime = System.currentTimeMillis()
        handler.postDelayed(checkRunnable, timeoutMs / 2)
    }

    fun beat() {
        lastHeartbeatTime = System.currentTimeMillis()
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(checkRunnable)
    }
}
