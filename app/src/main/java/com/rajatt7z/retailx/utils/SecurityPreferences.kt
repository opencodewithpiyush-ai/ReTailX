package com.rajatt7z.retailx.utils

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("SecurityPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PIN_ENABLED = "pin_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_SESSION_TIMEOUT = "session_timeout_minutes"
        private const val KEY_LAST_ACTIVE = "last_active_timestamp"
        private const val KEY_IS_LOCKED = "is_locked"
        private const val KEY_FAILED_ATTEMPTS = "failed_pin_attempts"
        private const val KEY_USER_NAME = "user_display_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val MAX_PIN_ATTEMPTS = 5
    }

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var isPinEnabled: Boolean
        get() = prefs.getBoolean(KEY_PIN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_ENABLED, value).apply()

    var sessionTimeoutMinutes: Int
        get() = prefs.getInt(KEY_SESSION_TIMEOUT, 0) // 0 = never
        set(value) = prefs.edit().putInt(KEY_SESSION_TIMEOUT, value).apply()

    var isLocked: Boolean
        get() = prefs.getBoolean(KEY_IS_LOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOCKED, value).apply()

    var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userRole: String
        get() = prefs.getString(KEY_USER_ROLE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, hashPin(pin)).apply()
        isPinEnabled = true
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return stored == hashPin(pin)
    }

    fun removePin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_PIN_ENABLED, false)
            .apply()
    }

    fun recordActivity() {
        prefs.edit().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply()
    }

    fun shouldLock(): Boolean {
        val timeout = sessionTimeoutMinutes
        if (timeout <= 0) return false // Never timeout
        if (!isPinEnabled && !isBiometricEnabled) return false // No lock methods set

        val lastActive = prefs.getLong(KEY_LAST_ACTIVE, System.currentTimeMillis())
        val elapsed = System.currentTimeMillis() - lastActive
        return elapsed > timeout * 60 * 1000L
    }

    val isMaxAttemptsReached: Boolean
        get() = failedAttempts >= MAX_PIN_ATTEMPTS

    fun resetFailedAttempts() {
        failedAttempts = 0
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
