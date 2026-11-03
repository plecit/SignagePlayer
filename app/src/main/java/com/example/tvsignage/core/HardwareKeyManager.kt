package com.example.tvsignage.core

import android.content.Context
import java.util.UUID

/**
 * Manages the persistent unique Hardware Key for display identification in Xibo CMS.
 * Generated once as a random UUID v4 and stored permanently.
 */
object HardwareKeyManager {

    private const val PREF_NAME = "hardware_identity_prefs"
    private const val KEY_HARDWARE_KEY = "hardware_key"
    private var cachedKey: String? = null

    @Synchronized
    fun getHardwareKey(context: Context): String {
        cachedKey?.let { return it }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var key = prefs.getString(KEY_HARDWARE_KEY, null)

        if (key.isNullOrBlank()) {
            key = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_HARDWARE_KEY, key).apply()
            SystemLogger.i("HardwareKeyManager", "Generated new persistent Hardware Key: $key")
        }

        cachedKey = key
        return key
    }
}
