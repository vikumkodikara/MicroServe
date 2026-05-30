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
        val currentTabName = when (this) {
            is RequestMainActivity -> HomeBottomNavHelper.TAB_REQUEST
            is MainActivity -> HomeBottomNavHelper.TAB_SERVICE
            is Homepage -> HomeBottomNavHelper.TAB_HOME
            is PostAdsActivity -> HomeBottomNavHelper.TAB_POST
            is PersonalInfoActivity -> HomeBottomNavHelper.TAB_PROFILE
            else -> HomeBottomNavHelper.TAB_HOME
        }
        HomeBottomNavHelper.setup(this, currentTabName)
    }
}
