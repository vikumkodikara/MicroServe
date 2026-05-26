package com.example.microserve

import android.content.Context

object AppPreferences {

    private const val PREF_NAME = "app_preferences"
    private const val KEY_DARK_MODE = "dark_mode_enabled"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"
    private const val KEY_LANGUAGE = "language"

    // Session keys
    private const val KEY_SESSION_UID = "session_uid"
    private const val KEY_SESSION_NAME = "session_name"
    private const val KEY_SESSION_EMAIL = "session_email"
    private const val KEY_SESSION_PHOTO_URL = "session_photo_url"
    private const val KEY_SESSION_ROLE = "session_role"

    fun isDarkModeEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_NOTIFICATIONS, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun getLanguage(context: Context): String {
        return prefs(context).getString(KEY_LANGUAGE, "English") ?: "English"
    }

    fun setLanguage(context: Context, language: String) {
        prefs(context).edit().putString(KEY_LANGUAGE, language).apply()
    }

    // ── Session Management ──────────────────────────────────────

    fun saveSession(context: Context, profile: UserProfile) {
        prefs(context).edit()
            .putString(KEY_SESSION_UID, profile.uid)
            .putString(KEY_SESSION_NAME, profile.name)
            .putString(KEY_SESSION_EMAIL, profile.email)
            .putString(KEY_SESSION_PHOTO_URL, profile.photoUrl)
            .putString(KEY_SESSION_ROLE, profile.role)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getSessionUid(context).isNotEmpty()
    }

    fun getSessionUid(context: Context): String {
        return prefs(context).getString(KEY_SESSION_UID, "") ?: ""
    }

    fun getSessionName(context: Context): String {
        return prefs(context).getString(KEY_SESSION_NAME, "") ?: ""
    }

    fun getSessionEmail(context: Context): String {
        return prefs(context).getString(KEY_SESSION_EMAIL, "") ?: ""
    }

    fun getSessionPhotoUrl(context: Context): String {
        return prefs(context).getString(KEY_SESSION_PHOTO_URL, "") ?: ""
    }

    fun getSessionRole(context: Context): String {
        return prefs(context).getString(KEY_SESSION_ROLE, UserProfile.ROLE_USER) ?: UserProfile.ROLE_USER
    }

    fun clearSession(context: Context) {
        prefs(context).edit()
            .remove(KEY_SESSION_UID)
            .remove(KEY_SESSION_NAME)
            .remove(KEY_SESSION_EMAIL)
            .remove(KEY_SESSION_PHOTO_URL)
            .remove(KEY_SESSION_ROLE)
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}

