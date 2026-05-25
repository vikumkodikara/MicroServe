package com.example.microserve

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var navigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.splashRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        startSplashSequence()
    }

    private fun startSplashSequence() {
        val logo = binding.splashLogo
        val glow = binding.splashGlow
        val appName = binding.splashAppName
        val tagline = binding.splashTagline
        val progress = binding.splashProgress

        logo.scaleX = 0.5f
        logo.scaleY = 0.5f
        glow.scaleX = 0.3f
        glow.scaleY = 0.3f
        appName.translationY = 30f
        tagline.translationY = 20f

        // Phase 1: Logo scales up with overshoot + fades in (0–800ms)
        val logoFadeIn = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f).setDuration(600)
        val logoScaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.5f, 1f).setDuration(800)
        val logoScaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.5f, 1f).setDuration(800)
        logoScaleX.interpolator = OvershootInterpolator(1.5f)
        logoScaleY.interpolator = OvershootInterpolator(1.5f)

        // Phase 2: Glow pulses in (200–900ms)
        val glowFadeIn = ObjectAnimator.ofFloat(glow, "alpha", 0f, 0.8f).setDuration(700)
        val glowScaleX = ObjectAnimator.ofFloat(glow, "scaleX", 0.3f, 1.1f).setDuration(900)
        val glowScaleY = ObjectAnimator.ofFloat(glow, "scaleY", 0.3f, 1.1f).setDuration(900)
        glowFadeIn.startDelay = 200
        glowScaleX.startDelay = 200
        glowScaleY.startDelay = 200
        glowScaleX.interpolator = DecelerateInterpolator()
        glowScaleY.interpolator = DecelerateInterpolator()

        // Phase 3: App name slides up + fades in (500–1000ms)
        val nameFadeIn = ObjectAnimator.ofFloat(appName, "alpha", 0f, 1f).setDuration(500)
        val nameSlideUp = ObjectAnimator.ofFloat(appName, "translationY", 30f, 0f).setDuration(500)
        nameFadeIn.startDelay = 500
        nameSlideUp.startDelay = 500
        nameSlideUp.interpolator = DecelerateInterpolator()

        // Phase 4: Tagline fades in (700–1100ms)
        val tagFadeIn = ObjectAnimator.ofFloat(tagline, "alpha", 0f, 1f).setDuration(400)
        val tagSlideUp = ObjectAnimator.ofFloat(tagline, "translationY", 20f, 0f).setDuration(400)
        tagFadeIn.startDelay = 700
        tagSlideUp.startDelay = 700

        // Phase 5: Progress bar fades in (900–1200ms)
        val progressFadeIn = ObjectAnimator.ofFloat(progress, "alpha", 0f, 1f).setDuration(300)
        progressFadeIn.startDelay = 900

        // Glow breathing pulse after initial appear
        val glowPulseX = ObjectAnimator.ofFloat(glow, "scaleX", 1.1f, 0.9f, 1.05f, 0.95f, 1f)
        glowPulseX.duration = 2000
        glowPulseX.startDelay = 1100
        glowPulseX.interpolator = AccelerateDecelerateInterpolator()
        val glowPulseY = ObjectAnimator.ofFloat(glow, "scaleY", 1.1f, 0.9f, 1.05f, 0.95f, 1f)
        glowPulseY.duration = 2000
        glowPulseY.startDelay = 1100

        val allAnimations = AnimatorSet()
        allAnimations.playTogether(
            logoFadeIn, logoScaleX, logoScaleY,
            glowFadeIn, glowScaleX, glowScaleY,
            nameFadeIn, nameSlideUp,
            tagFadeIn, tagSlideUp,
            progressFadeIn,
            glowPulseX, glowPulseY
        )
        allAnimations.start()

        binding.splashRoot.postDelayed({ goToLogin() }, 3200)
    }

    private fun goToLogin() {
        if (navigated || isFinishing) return
        navigated = true

        val root = binding.splashRoot
        root.animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction {
                startActivity(Intent(this, LoginActivity::class.java))
                overridePendingTransition(R.anim.splash_fade_in, R.anim.splash_fade_out)
                finish()
            }
            .start()
    }

    override fun onDestroy() {
        binding.splashRoot.animate().cancel()
        super.onDestroy()
    }
}
