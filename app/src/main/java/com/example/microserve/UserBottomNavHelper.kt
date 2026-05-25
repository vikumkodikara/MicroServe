package com.example.microserve

import android.content.Intent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

object UserBottomNavHelper {

    const val EXTRA_ACTIVE_TAB = "extra_active_tab"

    const val TAB_REQUEST = "request"
    const val TAB_SERVICE = "service"
    const val TAB_HOME = "home"
    const val TAB_POST = "post"
    const val TAB_PROFILE = "profile"

    fun setup(activity: AppCompatActivity, currentTab: String) {
        val navContainer = activity.findViewById<ConstraintLayout>(R.id.navContainer) ?: return
        val navSubtract = activity.findViewById<ImageView>(R.id.navSubtract) ?: return
        val activeEllipse = activity.findViewById<ImageView>(R.id.activeEllipse) ?: return
        val navActiveIcon = activity.findViewById<ImageView>(R.id.navActiveIcon) ?: return

        val navRequest = activity.findViewById<LinearLayout>(R.id.navRequest) ?: return
        val navService = activity.findViewById<LinearLayout>(R.id.navService) ?: return
        val navHome = activity.findViewById<LinearLayout>(R.id.navHome) ?: return
        val navPost = activity.findViewById<LinearLayout>(R.id.navPost) ?: return
        val navProfile = activity.findViewById<LinearLayout>(R.id.navProfile) ?: return

        val requestIcon = activity.findViewById<ImageView>(R.id.navRequestIcon) ?: return
        val requestLabel = activity.findViewById<TextView>(R.id.navRequestLabel) ?: return
        val serviceIcon = activity.findViewById<ImageView>(R.id.navServiceIcon) ?: return
        val serviceLabel = activity.findViewById<TextView>(R.id.navServiceLabel) ?: return
        val homeIcon = activity.findViewById<ImageView>(R.id.navHomeIcon) ?: return
        val homeLabel = activity.findViewById<TextView>(R.id.navHomeLabel) ?: return
        val postSpacer = activity.findViewById<View>(R.id.navPostSpacer) ?: return
        val postLabel = activity.findViewById<TextView>(R.id.navPostLabel) ?: return
        val profileIcon = activity.findViewById<ImageView>(R.id.navProfileIcon) ?: return
        val profileLabel = activity.findViewById<TextView>(R.id.navProfileLabel) ?: return

        navContainer.post {
            applyActiveTabState(
                currentTab,
                navSubtract,
                navActiveIcon,
                requestIcon,
                requestLabel,
                serviceIcon,
                serviceLabel,
                homeIcon,
                homeLabel,
                postSpacer,
                postLabel,
                profileIcon,
                profileLabel
            )

            activeEllipse.scaleX = 0f
            activeEllipse.scaleY = 0f
            activeEllipse.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(100)
                .setInterpolator(OvershootInterpolator(2f))
                .start()
        }

        navRequest.setOnClickListener {
            navigateToTab(activity, currentTab, TAB_REQUEST)
        }
        navService.setOnClickListener {
            navigateToTab(activity, currentTab, TAB_SERVICE)
        }
        navHome.setOnClickListener {
            navigateToTab(activity, currentTab, TAB_HOME)
        }
        navPost.setOnClickListener {
            navigateToTab(activity, currentTab, TAB_POST)
        }
        navProfile.setOnClickListener {
            navigateToTab(activity, currentTab, TAB_PROFILE)
        }
    }
}
