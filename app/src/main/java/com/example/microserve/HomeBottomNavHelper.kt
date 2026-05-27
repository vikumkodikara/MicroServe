package com.example.microserve

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * Wires the home-style bottom nav and directional tab transitions.
 *
 * Usage: HomeBottomNavHelper.setup(this, HomeBottomNavHelper.TAB_HOME)
 */
object HomeBottomNavHelper {

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
                Intent(activity, ServiceMainActivity::class.java)
            }
        }

        activity.findViewById<View>(R.id.navTabHome)?.setOnClickListener {
            navigate(activity, currentTab, TAB_HOME) {
                Intent(activity, Homepage::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }

        activity.findViewById<View>(R.id.navTabPost)?.setOnClickListener {
            navigate(activity, currentTab, TAB_POST) {
                Intent(activity, PostAdsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }

        activity.findViewById<View>(R.id.navTabProfile)?.setOnClickListener {
            navigate(activity, currentTab, TAB_PROFILE) {
                Intent(activity, PersonalInfoActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
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
