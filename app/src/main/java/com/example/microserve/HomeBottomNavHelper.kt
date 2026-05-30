package com.example.microserve

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.microserve.ui.components.CurvedBottomNavigationView

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
    
    private val INDEX_TO_TAB = mapOf(
        0 to TAB_REQUEST,
        1 to TAB_SERVICE,
        2 to TAB_HOME,
        3 to TAB_POST,
        4 to TAB_PROFILE
    )

    fun setup(activity: AppCompatActivity, currentTab: String = TAB_HOME) {
        val bottomNav = activity.findViewById<CurvedBottomNavigationView>(R.id.curvedBottomNavView) ?: return

        val currentIndex = TAB_ORDER[currentTab] ?: 2
        bottomNav.setInitialTab(currentIndex)

        bottomNav.onTabSelectedListener = { selectedIndex ->
            val toTab = INDEX_TO_TAB[selectedIndex] ?: TAB_HOME
            if (currentTab != toTab) {
                // Add a small delay to let the animation play out before jumping to new activity
                Handler(Looper.getMainLooper()).postDelayed({
                    navigate(activity, currentTab, toTab)
                }, 250) // wait 250ms for the animation
            }
        }
    }

    private fun navigate(
        activity: AppCompatActivity,
        fromTab: String,
        toTab: String
    ) {
        if (fromTab == toTab) return
        
        val intent = when (toTab) {
            TAB_REQUEST -> Intent(activity, RequestMainActivity::class.java)
            TAB_SERVICE -> Intent(activity, MainActivity::class.java)
            TAB_HOME -> Intent(activity, Homepage::class.java)
            TAB_POST -> Intent(activity, PostAdsActivity::class.java)
            TAB_PROFILE -> Intent(activity, PersonalInfoActivity::class.java)
            else -> Intent(activity, Homepage::class.java)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val fromIndex = TAB_ORDER[fromTab] ?: 2
        val toIndex = TAB_ORDER[toTab] ?: 2

        activity.startActivity(intent)
        activity.finish()

        if (toIndex > fromIndex) {
            activity.overridePendingTransition(R.anim.nav_slide_in_right, R.anim.nav_slide_out_left)
        } else {
            activity.overridePendingTransition(R.anim.nav_slide_in_left, R.anim.nav_slide_out_right)
        }
    }
}
