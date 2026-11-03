package com.example.tvsignage

import android.app.Application
import com.example.tvsignage.core.HardwareKeyManager
import com.example.tvsignage.core.SecurePreferences
import com.example.tvsignage.core.SystemLogger
import com.example.tvsignage.service.CrashHandler
import com.example.tvsignage.stats.StatDatabase

class SignageApplication : Application() {

    lateinit var securePreferences: SecurePreferences
        private set

    lateinit var statDatabase: StatDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize Logger
        SystemLogger.init(this)
        SystemLogger.i("SignageApplication", "Initializing TV Signage Player application...")

        // 2. Initialize Crash Handler
        CrashHandler.install(this)

        // 3. Initialize Secure Preferences and Hardware Key
        securePreferences = SecurePreferences(this)
        HardwareKeyManager.getHardwareKey(this)

        // 4. Initialize Database
        statDatabase = StatDatabase.getInstance(this)
        SystemLogger.i("SignageApplication", "Initialization complete. HardwareKey=${HardwareKeyManager.getHardwareKey(this)}")
    }

    companion object {
        lateinit var instance: SignageApplication
            private set
    }
}
