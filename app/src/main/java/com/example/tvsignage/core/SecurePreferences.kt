package com.example.tvsignage.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages encrypted storage for sensitive player credentials (CMS URL, Server Key, Admin PIN).
 * Uses EncryptedSharedPreferences with fallback to standard private preferences if Keystore is unavailable.
 */
class SecurePreferences(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        SystemLogger.w(TAG, "Keystore unavailable, falling back to private SharedPreferences", e)
        context.getSharedPreferences(PREFS_FILENAME + "_fallback", Context.MODE_PRIVATE)
    }

    var cmsUrl: String
        get() = prefs.getString(KEY_CMS_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CMS_URL, value.trim().trimEnd('/')).apply()

    var serverKey: String
        get() = prefs.getString(KEY_SERVER_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SERVER_KEY, value.trim()).apply()

    var displayName: String
        get() = prefs.getString(KEY_DISPLAY_NAME, "Android-TV-Player") ?: "Android-TV-Player"
        set(value) = prefs.edit().putString(KEY_DISPLAY_NAME, value.trim()).apply()

    var isAuthorised: Boolean
        get() = prefs.getBoolean(KEY_IS_AUTHORISED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_AUTHORISED, value).apply()

    var collectIntervalSeconds: Int
        get() = prefs.getInt(KEY_COLLECT_INTERVAL, DEFAULT_COLLECT_INTERVAL)
        set(value) = prefs.edit().putInt(KEY_COLLECT_INTERVAL, value.coerceAtLeast(10)).apply()

    var statsEnabled: Boolean
        get() = prefs.getBoolean(KEY_STATS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_STATS_ENABLED, value).apply()

    var adminPin: String
        get() = prefs.getString(KEY_ADMIN_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
        set(value) = prefs.edit().putString(KEY_ADMIN_PIN, value).apply()

    var trustSelfSignedSsl: Boolean
        get() = prefs.getBoolean(KEY_TRUST_SELF_SIGNED, false)
        set(value) = prefs.edit().putBoolean(KEY_TRUST_SELF_SIGNED, value).apply()

    var xmrChannel: String
        get() = prefs.getString(KEY_XMR_CHANNEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_XMR_CHANNEL, value).apply()

    var xmrPubKey: String
        get() = prefs.getString(KEY_XMR_PUBKEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_XMR_PUBKEY, value).apply()

    var timezone: String
        get() = prefs.getString(KEY_TIMEZONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TIMEZONE, value).apply()

    fun isConfigured(): Boolean {
        return cmsUrl.isNotBlank() && serverKey.isNotBlank()
    }

    companion object {
        private const val TAG = "SecurePreferences"
        private const val PREFS_FILENAME = "tv_signage_secure_prefs"

        private const val KEY_CMS_URL = "cms_url"
        private const val KEY_SERVER_KEY = "server_key"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_IS_AUTHORISED = "is_authorised"
        private const val KEY_COLLECT_INTERVAL = "collect_interval"
        private const val KEY_STATS_ENABLED = "stats_enabled"
        private const val KEY_ADMIN_PIN = "admin_pin"
        private const val KEY_TRUST_SELF_SIGNED = "trust_self_signed"
        private const val KEY_XMR_CHANNEL = "xmr_channel"
        private const val KEY_XMR_PUBKEY = "xmr_pubkey"
        private const val KEY_TIMEZONE = "timezone"

        const val DEFAULT_COLLECT_INTERVAL = 60 // 60 seconds default
        const val DEFAULT_PIN = "0000"
    }
}
