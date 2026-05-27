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
    private const val KEY_SESSION_PHONE = "session_phone"
    private const val KEY_SESSION_LOCATION = "session_location"
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
            .putString(KEY_SESSION_PHONE, profile.phone)
            .putString(KEY_SESSION_LOCATION, profile.location)
            .putString(KEY_SESSION_PHOTO_URL, profile.photoUrl)
            .putString(KEY_SESSION_ROLE, profile.role)
            .commit()
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

    fun setSessionPhotoUrl(context: Context, photoUrl: String) {
        prefs(context).edit().putString(KEY_SESSION_PHOTO_URL, photoUrl).apply()
    }

    fun setSessionName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_SESSION_NAME, name.trim()).apply()
    }

    fun getSessionPhone(context: Context): String {
        return prefs(context).getString(KEY_SESSION_PHONE, "") ?: ""
    }

    fun getSessionLocation(context: Context): String {
        return prefs(context).getString(KEY_SESSION_LOCATION, "") ?: ""
    }

    fun getSessionProfile(context: Context): UserProfile {
        return UserProfile(
            uid = getSessionUid(context),
            name = getSessionName(context),
            email = getSessionEmail(context),
            phone = getSessionPhone(context),
            location = getSessionLocation(context),
            photoUrl = getSessionPhotoUrl(context),
            role = getSessionRole(context)
        )
    }

    fun getSessionRole(context: Context): String {
        return prefs(context).getString(KEY_SESSION_ROLE, UserProfile.ROLE_USER) ?: UserProfile.ROLE_USER
    }

    fun clearSession(context: Context) {
        ProfilePhotoHelper.clearPhoto(context)
        prefs(context).edit()
            .remove(KEY_SESSION_UID)
            .remove(KEY_SESSION_NAME)
            .remove(KEY_SESSION_EMAIL)
            .remove(KEY_SESSION_PHONE)
            .remove(KEY_SESSION_LOCATION)
            .remove(KEY_SESSION_PHOTO_URL)
            .remove(KEY_SESSION_ROLE)
            .apply()
    }
    // ── M Points Wallet ──────────────────────────────────────────

    private const val KEY_M_POINTS = "m_points"

    fun getMPoints(context: Context): Int {
        return prefs(context).getInt(KEY_M_POINTS, 10500)
    }

    fun setMPoints(context: Context, points: Int) {
        prefs(context).edit().putInt(KEY_M_POINTS, points.coerceAtLeast(0)).commit()
    }

    fun addMPoints(context: Context, amount: Int): Int {
        val newBalance = getMPoints(context) + amount
        setMPoints(context, newBalance)
        return newBalance
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}

