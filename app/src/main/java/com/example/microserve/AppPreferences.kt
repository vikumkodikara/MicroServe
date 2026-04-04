package com.example.microserve

import android.content.Context

object AppPreferences {

    private const val PREF_NAME = "app_preferences"
    private const val KEY_DARK_MODE = "dark_mode_enabled"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"
    private const val KEY_LANGUAGE = "language"

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

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
