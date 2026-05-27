package com.example.microserve

import android.content.Intent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Helper to set up the custom animated bottom navigation bar.
 *
 * Usage:
 *   AdminBottomNavHelper.setup(this, "home")   // on Homepage
 *   AdminBottomNavHelper.setup(this, "profile") // on AdminProfileActivity
 *   AdminBottomNavHelper.setup(this, "settings") // on SettingsActivity
 *   AdminBottomNavHelper.setup(this, "home")    // on sub-pages like Requesters, etc.
 */
object AdminBottomNavHelper {

    const val TAB_HOME = "home"
    const val TAB_PROFILE = "profile"
    const val TAB_SETTINGS = "settings"

    fun setup(activity: AppCompatActivity, currentTab: String) {
        val root = activity.findViewById<FrameLayout>(R.id.customBottomNav) ?: return

        val bubble = activity.findViewById<FrameLayout>(R.id.navBubble) ?: return
        val bubbleIcon = activity.findViewById<ImageView>(R.id.navBubbleIcon) ?: return

        val tabHome = activity.findViewById<LinearLayout>(R.id.navTabHome) ?: return
        val tabProfile = activity.findViewById<LinearLayout>(R.id.navTabProfile) ?: return
        val tabSettings = activity.findViewById<LinearLayout>(R.id.navTabSettings) ?: return

        val iconHome = activity.findViewById<ImageView>(R.id.navIconHome) ?: return
        val iconProfile = activity.findViewById<ImageView>(R.id.navIconProfile) ?: return
        val iconSettings = activity.findViewById<ImageView>(R.id.navIconSettings) ?: return

        val labelHome = activity.findViewById<TextView>(R.id.navLabelHome) ?: return
        val labelProfile = activity.findViewById<TextView>(R.id.navLabelProfile) ?: return
        val labelSettings = activity.findViewById<TextView>(R.id.navLabelSettings) ?: return

        val targetTab: View = when (currentTab) {
            TAB_HOME -> tabHome
            TAB_PROFILE -> tabProfile
            TAB_SETTINGS -> tabSettings
            else -> tabHome
        }

        val listener = object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (targetTab.width > 0) {
                    root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    applyBubbleState(
                        currentTab, bubble, bubbleIcon,
                        tabHome, tabProfile, tabSettings,
                        iconHome, iconProfile, iconSettings,
                        labelHome, labelProfile, labelSettings
                    )

                    // Entrance animation: scale in with overshoot
                    bubble.visibility = View.VISIBLE
                    bubble.scaleX = 0f
                    bubble.scaleY = 0f
                    bubble.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(400)
                        .setStartDelay(100)
                        .setInterpolator(OvershootInterpolator(2f))
                        .start()
                }
            }
        }
        root.viewTreeObserver.addOnGlobalLayoutListener(listener)

        // Click listeners
        tabHome.setOnClickListener {
            if (currentTab != TAB_HOME) {
                activity.startActivity(
                    Intent(activity, AdminDashboardActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
                activity.finish()
                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }

        tabProfile.setOnClickListener {
            if (currentTab != TAB_PROFILE) {
                activity.startActivity(Intent(activity, AdminProfileActivity::class.java))
                activity.finish()
                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }

        tabSettings.setOnClickListener {
            if (currentTab != TAB_SETTINGS) {
                activity.startActivity(Intent(activity, SettingsActivity::class.java))
                activity.finish()
                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }

    private fun applyBubbleState(
        currentTab: String,
        bubble: FrameLayout,
        bubbleIcon: ImageView,
        tabHome: LinearLayout,
        tabProfile: LinearLayout,
        tabSettings: LinearLayout,
        iconHome: ImageView,
        iconProfile: ImageView,
        iconSettings: ImageView,
        labelHome: TextView,
        labelProfile: TextView,
        labelSettings: TextView
    ) {
        // Determine target tab and icon
        val targetTab: View
        val iconRes: Int

        when (currentTab) {
            TAB_HOME -> {
                targetTab = tabHome
                iconRes = R.drawable.ic_nav_home
                iconHome.visibility = View.INVISIBLE
                labelHome.visibility = View.INVISIBLE
                iconProfile.visibility = View.VISIBLE
                labelProfile.visibility = View.VISIBLE
                iconSettings.visibility = View.VISIBLE
                labelSettings.visibility = View.VISIBLE
            }
            TAB_PROFILE -> {
                targetTab = tabProfile
                iconRes = R.drawable.ic_nav_profile
                iconProfile.visibility = View.INVISIBLE
                labelProfile.visibility = View.INVISIBLE
                iconHome.visibility = View.VISIBLE
                labelHome.visibility = View.VISIBLE
                iconSettings.visibility = View.VISIBLE
                labelSettings.visibility = View.VISIBLE
            }
            TAB_SETTINGS -> {
                targetTab = tabSettings
                iconRes = R.drawable.ic_nav_settings
                iconSettings.visibility = View.INVISIBLE
                labelSettings.visibility = View.INVISIBLE
                iconHome.visibility = View.VISIBLE
                labelHome.visibility = View.VISIBLE
                iconProfile.visibility = View.VISIBLE
                labelProfile.visibility = View.VISIBLE
            }
            else -> {
                targetTab = tabHome
                iconRes = R.drawable.ic_nav_home
                iconHome.visibility = View.INVISIBLE
                labelHome.visibility = View.INVISIBLE
                iconProfile.visibility = View.VISIBLE
                labelProfile.visibility = View.VISIBLE
                iconSettings.visibility = View.VISIBLE
                labelSettings.visibility = View.VISIBLE
            }
        }

        // Set bubble icon
        bubbleIcon.setImageResource(iconRes)

        // Position the bubble centered on the target tab
        val barParent = tabHome.parent as View
        val tabCenterX = targetTab.left + targetTab.width / 2f
        bubble.x = barParent.left + tabCenterX - bubble.width / 2f
    }
}
