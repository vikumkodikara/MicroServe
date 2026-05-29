package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * A reusable Base Activity that handles the Bottom Navigation logic.
 * Extend this class in your activities (e.g., MainActivity, Homepage, etc.)
 * so you don't have to duplicate the navigation logic.
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onStart() {
        super.onStart()
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        // Find the navigation views. If they are null, it means the current
        // activity doesn't include the bottom navigation layout, which is fine.
        val navRequest = findViewById<View>(R.id.navTabRequest)
        val navService = findViewById<View>(R.id.navTabService)
        val navHome = findViewById<View>(R.id.navTabHome)
        val navPost = findViewById<View>(R.id.navTabPost)
        val navProfile = findViewById<View>(R.id.navTabProfile)

        if (navHome == null) return // Bottom nav not present in this layout

        navRequest?.setOnClickListener { navigateTo(RequestMainActivity::class.java) }
        navService?.setOnClickListener { navigateTo(MainActivity::class.java) } // MainActivity acts as Service tab
        navHome?.setOnClickListener { navigateTo(Homepage::class.java) }
        navPost?.setOnClickListener { navigateTo(PostAdsActivity::class.java) }
        navProfile?.setOnClickListener { navigateTo(PersonalInfoActivity::class.java) }
    }

    private fun navigateTo(targetActivity: Class<out AppCompatActivity>) {
        // Prevent launching the same activity if we are already on it
        if (this::class.java == targetActivity) return

        val intent = Intent(this, targetActivity)
        // Ensure activities don't stack infinitely
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        // Removes transition animation for a smoother tab-switch feel
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
