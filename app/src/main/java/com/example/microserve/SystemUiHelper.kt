package com.example.microserve

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Edge-to-edge purple system bars: transparent status/navigation bars with light icons,
 * and helpers so content insets do not show white gaps above purple headers.
 */
object SystemUiHelper {

    fun applyPurpleSystemBars(activity: AppCompatActivity) {
        activity.enableEdgeToEdge()
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    fun setupPurpleHeaderScreen(
        activity: Activity,
        root: View,
        headerView: View,
        bottomSpacerId: Int = R.id.navSystemBarSpacer,
        footerBar: View? = null
    ) {
        root.applyHorizontalSystemBarInsets()
        headerView.applyStatusBarTopInset()
        if (footerBar != null) {
            footerBar.applyPurpleFooterNavInset()
        } else {
            activity.applyNavBarSpacer(bottomSpacerId)
        }
    }

    /** Splash: white screen first, then purple circle animation — light system icons. */
    fun applySplashScreen(activity: AppCompatActivity) {
        activity.enableEdgeToEdge()
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.WHITE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        WindowInsetsControllerCompat(window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    fun setupFullBleedPurpleScreen(activity: Activity, root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        if (root.isAttachedToWindow) {
            ViewCompat.requestApplyInsets(root)
        }
    }
}

/** Status-bar inset on a purple header (do not pad the light root above the header). */
fun View.applyStatusBarTopInset() {
    val basePaddingTop = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        view.setPadding(
            view.paddingLeft,
            basePaddingTop + top,
            view.paddingRight,
            view.paddingBottom
        )
        insets
    }
    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    }
}

/** Horizontal safe-area only on full-screen roots (no top padding). */
fun View.applyHorizontalSystemBarInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(bars.left, view.paddingTop, bars.right, view.paddingBottom)
        insets
    }
    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    }
}

fun Activity.applyNavBarSpacer(spacerId: Int) {
    findViewById<View>(spacerId)?.let { spacer ->
        ViewCompat.setOnApplyWindowInsetsListener(spacer) { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp = view.layoutParams
            if (lp.height != bottom) {
                lp.height = bottom
                view.layoutParams = lp
            }
            insets
        }
        if (spacer.isAttachedToWindow) {
            ViewCompat.requestApplyInsets(spacer)
        }
    }
}

/** Purple footer strip + gesture/nav bar inset (screens without home bottom nav). */
fun View.applyPurpleFooterNavInset(baseHeightDp: Int = 38) {
    val baseHeightPx = (baseHeightDp * resources.displayMetrics.density).toInt()
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        val lp = view.layoutParams
        val target = baseHeightPx + bottom
        if (lp.height != target) {
            lp.height = target
            view.layoutParams = lp
        }
        insets
    }
    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    }
}
