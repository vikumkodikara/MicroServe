package com.example.microserve

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class MicroServeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        enableFirestoreOfflineCache()
        applySavedTheme()
        registerSystemUiCallbacks()
        applySavedLanguage()
        // Defer admin seeding so it does not race with user sign-in at startup.
        Handler(Looper.getMainLooper()).postDelayed({
            if (FirebaseAuth.getInstance().currentUser == null) {
                UserRepository.ensureAdminExists()
            }
        }, 4000L)
    }

    private fun enableFirestoreOfflineCache() {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }

    private fun applySavedTheme() {
        val mode = if (AppPreferences.isDarkModeEnabled(this)) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun registerSystemUiCallbacks() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is AppCompatActivity && activity !is SplashActivity) {
                    SystemUiHelper.applyPurpleSystemBars(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {
                if (activity is AppCompatActivity && activity !is SplashActivity) {
                    SystemUiHelper.applyPurpleSystemBars(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private fun applySavedLanguage() {
        val language = AppPreferences.getLanguage(this)
        val langCode = when (language) {
            "Sinhala" -> "si"
            "Tamil" -> "ta"
            else -> "en"
        }
        AppCompatDelegate.setApplicationLocales(
            androidx.core.os.LocaleListCompat.forLanguageTags(langCode)
        )
    }
}
