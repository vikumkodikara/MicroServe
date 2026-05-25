package com.example.microserve

import androidx.appcompat.app.AppCompatActivity

object UserBottomNavHelper {

    const val EXTRA_ACTIVE_TAB = "extra_active_tab"

    const val TAB_REQUEST = "request"
    const val TAB_SERVICE = "service"
    const val TAB_HOME = "home"
    const val TAB_POST = "post"
    const val TAB_PROFILE = "profile"

    fun setup(activity: AppCompatActivity, currentTab: String) {
        // Navigation wiring added in follow-up commits.
    }
}
