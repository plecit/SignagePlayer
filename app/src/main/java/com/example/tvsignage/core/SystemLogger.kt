package com.example.tvsignage.core

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

data class LogEntry(
    val timestamp: String,
    val category: String,
    val level: String,
    val message: String
)

object SystemLogger {

    private const val TAG = "TVSignage"
    private const val MAX_BUFFER_SIZE = 500

    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun init(context: Context) {
        // Initialization if file logging or persistence needed
    }

    fun d(category: String, message: String) {
        Log.d("$TAG:$category", message)
        append("DEBUG", category, message)
    }

    fun i(category: String, message: String) {
        Log.i("$TAG:$category", message)
        append("INFO", category, message)
    }

    fun w(category: String, message: String, throwable: Throwable? = null) {
        val msg = if (throwable != null) "$message: ${Log.getStackTraceString(throwable)}" else message
        Log.w("$TAG:$category", msg)
        append("WARN", category, msg)
    }

    fun e(category: String, message: String, throwable: Throwable? = null) {
        val msg = if (throwable != null) "$message: ${Log.getStackTraceString(throwable)}" else message
        Log.e("$TAG:$category", msg)
        append("ERROR", category, msg)
    }

    private fun append(level: String, category: String, message: String) {
        val time = synchronized(dateFormat) { dateFormat.format(Date()) }
        logQueue.offer(LogEntry(time, category, level, message))
        while (logQueue.size > MAX_BUFFER_SIZE) {
            logQueue.poll()
        }
    }

    fun drainLogs(max: Int = 100): List<LogEntry> {
        val list = mutableListOf<LogEntry>()
        var count = 0
        while (count < max) {
            val item = logQueue.poll() ?: break
            list.add(item)
            count++
        }
        return list
    }
}
