package com.example.microserve

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * Wires the home-style bottom nav and directional tab transitions for the user app flow.
 */
object HomeBottomNavHelper {

    const val EXTRA_ACTIVE_TAB = "extra_active_tab"

    const val TAB_REQUEST = "request"
    const val TAB_SERVICE = "service"
    const val TAB_HOME = "home"
    const val TAB_POST = "post"
    const val TAB_PROFILE = "profile"

    private val TAB_ORDER = mapOf(
        TAB_REQUEST to 0,
        TAB_SERVICE to 1,
        TAB_HOME to 2,
        TAB_POST to 3,
        TAB_PROFILE to 4
    )

    fun setup(activity: AppCompatActivity, currentTab: String = TAB_HOME) {
        if (activity.findViewById<View>(R.id.customBottomNav) == null) return

        activity.findViewById<View>(R.id.navTabRequest)?.setOnClickListener {
            navigate(activity, currentTab, TAB_REQUEST) {
                Intent(activity, RequestMainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }

        activity.findViewById<View>(R.id.navTabService)?.setOnClickListener {
            navigate(activity, currentTab, TAB_SERVICE) {
                Intent(activity, PostServiceActivity::class.java)
            }
        }

        activity.findViewById<View>(R.id.navTabHome)?.setOnClickListener {
            navigate(activity, currentTab, TAB_HOME) {
                postAdsIntent(activity, TAB_HOME)
            }
        }

        activity.findViewById<View>(R.id.navTabPost)?.setOnClickListener {
            navigate(activity, currentTab, TAB_POST) {
                postAdsIntent(activity, TAB_POST)
            }
        }

        activity.findViewById<View>(R.id.navTabProfile)?.setOnClickListener {
            navigate(activity, currentTab, TAB_PROFILE) {
                Intent(activity, EditProfileActivity::class.java)
            }
        }
    }

    private fun postAdsIntent(activity: AppCompatActivity, tab: String): Intent {
        return Intent(activity, PostAdsActivity::class.java)
            .putExtra(EXTRA_ACTIVE_TAB, tab)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    private fun navigate(
        activity: AppCompatActivity,
        fromTab: String,
        toTab: String,
        intentBuilder: () -> Intent
    ) {
        if (fromTab == toTab) return

        val fromIndex = TAB_ORDER[fromTab] ?: TAB_ORDER[TAB_HOME]!!
        val toIndex = TAB_ORDER[toTab] ?: TAB_ORDER[TAB_HOME]!!

        activity.startActivity(intentBuilder())
        activity.finish()

        if (toIndex > fromIndex) {
            activity.overridePendingTransition(R.anim.nav_slide_in_right, R.anim.nav_slide_out_left)
        } else {
            activity.overridePendingTransition(R.anim.nav_slide_in_left, R.anim.nav_slide_out_right)
        }
    }
}
