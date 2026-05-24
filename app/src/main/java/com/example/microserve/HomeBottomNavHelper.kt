package com.example.microserve

import android.content.Intent
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

/**
 * Wires the layered home bottom nav (rectanglenav + subtractnav + ellipsenav).
 *
 * Usage: HomeBottomNavHelper.setup(this, HomeBottomNavHelper.TAB_HOME)
 */
object HomeBottomNavHelper {

    const val TAB_REQUEST = "request"
    const val TAB_SERVICE = "service"
    const val TAB_HOME = "home"
    const val TAB_POST = "post"
    const val TAB_PROFILE = "profile"

    fun setup(activity: AppCompatActivity, currentTab: String = TAB_HOME) {
        if (activity.findViewById<android.view.View>(R.id.customBottomNav) == null) return

        activity.findViewById<LinearLayout>(R.id.navTabRequest)?.setOnClickListener {
            if (currentTab == TAB_REQUEST) return@setOnClickListener
            activity.startActivity(Intent(activity, RequestersActivity::class.java))
            activity.finish()
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        activity.findViewById<LinearLayout>(R.id.navTabService)?.setOnClickListener {
            if (currentTab == TAB_SERVICE) return@setOnClickListener
            activity.startActivity(Intent(activity, ServicesActivity::class.java))
            activity.finish()
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        activity.findViewById<LinearLayout>(R.id.navTabHome)?.setOnClickListener {
            if (currentTab == TAB_HOME) return@setOnClickListener
            activity.startActivity(
                Intent(activity, Homepage::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            activity.finish()
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        activity.findViewById<LinearLayout>(R.id.navTabPost)?.setOnClickListener {
            if (currentTab == TAB_POST) return@setOnClickListener
            activity.startActivity(Intent(activity, PostServiceActivity::class.java))
            activity.finish()
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        activity.findViewById<LinearLayout>(R.id.navTabProfile)?.setOnClickListener {
            if (currentTab == TAB_PROFILE) return@setOnClickListener
            activity.startActivity(Intent(activity, AdminProfileActivity::class.java))
            activity.finish()
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
