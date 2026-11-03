package com.example.tvsignage.service

import android.content.Context
import android.content.Intent
import com.example.tvsignage.core.SystemLogger
import com.example.tvsignage.ui.PlayerActivity

class CrashHandler private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        SystemLogger.e("CrashHandler", "FATAL CRASH in thread ${t.name}: ${e.message}", e)

        try {
            // Attempt auto-restart of PlayerActivity
            val intent = Intent(context, PlayerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
        } catch (restartEx: Exception) {
            SystemLogger.e("CrashHandler", "Failed to restart PlayerActivity: ${restartEx.message}")
        }

        defaultHandler?.uncaughtException(t, e)
    }

    companion object {
        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext))
        }
    }
}
