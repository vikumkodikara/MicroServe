package com.example.microserve

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var navigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.splashRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        binding.splashRoot.post { startSplashSequence() }
    }

    private fun startSplashSequence() {
        val logoCard = binding.splashLogoCard
        val circle = binding.purpleCircle
        val appName = binding.splashAppName
        val tagline = binding.splashTagline
        val progress = binding.splashProgress

        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val density = resources.displayMetrics.density

        logoCard.scaleX = 0f
        logoCard.scaleY = 0f

        val shiftLeft = 70f * density

        // ── Phase 1: Logo pops up big in center ──
        val phase1 = AnimatorSet()
        phase1.playTogether(
            ObjectAnimator.ofFloat(logoCard, "alpha", 0f, 1f).setDuration(300),
            ObjectAnimator.ofFloat(logoCard, "scaleX", 0f, 1.12f).apply {
                duration = 600; interpolator = OvershootInterpolator(2f)
            },
            ObjectAnimator.ofFloat(logoCard, "scaleY", 0f, 1.12f).apply {
                duration = 600; interpolator = OvershootInterpolator(2f)
            }
        )

        // ── Phase 2: Logo settles back to normal size ──
        val phase2 = AnimatorSet()
        phase2.playTogether(
            ObjectAnimator.ofFloat(logoCard, "scaleX", 1.12f, 1f).setDuration(350),
            ObjectAnimator.ofFloat(logoCard, "scaleY", 1.12f, 1f).setDuration(350)
        )
        phase2.interpolator = DecelerateInterpolator()

        // ── Phase 3: Purple circle expands to fill screen ──
        val maxScale = (screenW * 3f) / (100f * density)
        val phase3 = AnimatorSet()
        phase3.playTogether(
            ObjectAnimator.ofFloat(circle, "alpha", 0f, 1f).setDuration(200),
            ObjectAnimator.ofFloat(circle, "scaleX", 1f, maxScale).setDuration(700),
            ObjectAnimator.ofFloat(circle, "scaleY", 1f, maxScale).setDuration(700)
        )
        phase3.interpolator = AccelerateInterpolator(1.2f)

        // ── Phase 4: Logo slides a little bit to the left ──
        val phase4 = AnimatorSet()
        phase4.playTogether(
            ObjectAnimator.ofFloat(logoCard, "translationX", 0f, -shiftLeft).setDuration(500),
            ObjectAnimator.ofFloat(appName, "translationX", 0f, -shiftLeft).setDuration(500),
            ObjectAnimator.ofFloat(tagline, "translationX", 0f, -shiftLeft).setDuration(500)
        )
        phase4.interpolator = DecelerateInterpolator(1.5f)

        // ── Phase 5: App name + tagline fade in to the right of logo ──
        val phase5 = AnimatorSet()
        phase5.playTogether(
            ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f).setDuration(400),
            ObjectAnimator.ofFloat(appName, "translationY", 16f, 0f).apply {
                duration = 450; interpolator = DecelerateInterpolator()
            },
            ObjectAnimator.ofFloat(tagline, "alpha", 0f, 1f).apply {
                duration = 400; startDelay = 120
            },
            ObjectAnimator.ofFloat(tagline, "translationY", 16f, 0f).apply {
                duration = 450; startDelay = 120; interpolator = DecelerateInterpolator()
            }
        )

        // ── Phase 6: Loading bar appears ──
        val phase6 = ObjectAnimator.ofFloat(progress, "alpha", 0f, 1f).setDuration(300)

        binding.splashRoot.postDelayed({ navigateNext() }, 3200)
        val fullSequence = AnimatorSet()
        fullSequence.playSequentially(phase1, phase2, phase3, phase4, phase5, phase6)
        fullSequence.start()
    }

    /**
     * Routes to the correct screen based on session state:
     * - If logged in as admin → AdminDashboard
     * - If logged in as user → Homepage
     * - Otherwise → Login
     */
    private fun navigateNext() {
        if (navigated || isFinishing) return
        navigated = true

        val hasSession = AppPreferences.isLoggedIn(this)
        val hasFirebaseUser = FirebaseAuth.getInstance().currentUser != null
        val role = AppPreferences.getSessionRole(this)

        val target = when {
            hasSession && hasFirebaseUser && role.equals(UserProfile.ROLE_ADMIN, ignoreCase = true) ->
                AdminDashboardActivity::class.java
            hasSession && hasFirebaseUser ->
                Homepage::class.java
            // Admin with local-only credentials (no Firebase Auth)
            hasSession && role.equals(UserProfile.ROLE_ADMIN, ignoreCase = true) ->
                AdminDashboardActivity::class.java
            else ->
                LoginActivity::class.java
        }

        val root = binding.splashRoot
        root.animate()
            .alpha(0f)
            .setDuration(400)
        binding.splashRoot.animate()
            .alpha(0f).setDuration(400)
            .withEndAction {
                startActivity(
                    Intent(this, target)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                overridePendingTransition(R.anim.splash_fade_in, R.anim.splash_fade_out)
                finish()
            }.start()
    }

    override fun onDestroy() {
        binding.splashRoot.handler?.removeCallbacksAndMessages(null)
        binding.splashRoot.animate().cancel()
        super.onDestroy()
    }
}

