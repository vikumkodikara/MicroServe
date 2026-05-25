package com.example.microserve

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var navigated = false
    private var animatorSet: AnimatorSet? = null

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
        val logo = binding.splashLogo
        val glow = binding.splashGlow
        val ringIn = binding.neonRingInner
        val ringOut = binding.neonRingOuter
        val name = binding.splashAppName
        val tag = binding.splashTagline
        val progress = binding.splashProgress
        val powered = binding.splashPowered
        val particles = binding.particleView

        logo.scaleX = 0.3f; logo.scaleY = 0.3f
        glow.scaleX = 0.1f; glow.scaleY = 0.1f
        ringIn.scaleX = 0.5f; ringIn.scaleY = 0.5f; ringIn.rotation = -30f
        ringOut.scaleX = 0.3f; ringOut.scaleY = 0.3f; ringOut.rotation = 30f
        name.translationY = 40f
        tag.translationY = 25f

        val anims = mutableListOf<android.animation.Animator>()

        // Particles fade in
        anims += ObjectAnimator.ofFloat(particles, "alpha", 0f, 0.7f).setDuration(1200)

        // Glow bloom (200ms delay)
        anims += anim(glow, "alpha", 0f, 0.9f, 800, 200, DecelerateInterpolator(1.5f))
        anims += anim(glow, "scaleX", 0.1f, 1.15f, 1000, 200, DecelerateInterpolator(1.5f))
        anims += anim(glow, "scaleY", 0.1f, 1.15f, 1000, 200, DecelerateInterpolator(1.5f))

        // Logo zoom in with overshoot (400ms delay)
        anims += anim(logo, "alpha", 0f, 1f, 600, 400)
        anims += anim(logo, "scaleX", 0.3f, 1f, 900, 400, OvershootInterpolator(1.2f))
        anims += anim(logo, "scaleY", 0.3f, 1f, 900, 400, OvershootInterpolator(1.2f))

        // Inner ring spin in (600ms delay)
        anims += anim(ringIn, "alpha", 0f, 1f, 700, 600)
        anims += anim(ringIn, "scaleX", 0.5f, 1f, 800, 600, DecelerateInterpolator())
        anims += anim(ringIn, "scaleY", 0.5f, 1f, 800, 600, DecelerateInterpolator())
        anims += anim(ringIn, "rotation", -30f, 0f, 800, 600)

        // Outer ring spin in (700ms delay)
        anims += anim(ringOut, "alpha", 0f, 0.7f, 700, 700)
        anims += anim(ringOut, "scaleX", 0.3f, 1f, 900, 700, DecelerateInterpolator())
        anims += anim(ringOut, "scaleY", 0.3f, 1f, 900, 700, DecelerateInterpolator())
        anims += anim(ringOut, "rotation", 30f, 0f, 900, 700)

        // App name slide up (1000ms delay)
        anims += anim(name, "alpha", 0f, 1f, 500, 1000)
        anims += anim(name, "translationY", 40f, 0f, 600, 1000, DecelerateInterpolator(1.5f))

        // Tagline (1300ms delay)
        anims += anim(tag, "alpha", 0f, 1f, 400, 1300)
        anims += anim(tag, "translationY", 25f, 0f, 500, 1300)

        // Loading bar (1500ms delay)
        anims += anim(progress, "alpha", 0f, 1f, 400, 1500)

        // Footer text (1700ms delay)
        anims += anim(powered, "alpha", 0f, 1f, 400, 1700)

        // Glow breathing pulse
        anims += ObjectAnimator.ofFloat(glow, "scaleX", 1.15f, 0.95f, 1.1f, 0.98f, 1.05f).apply {
            duration = 2500; startDelay = 1300; interpolator = AccelerateDecelerateInterpolator()
        }
        anims += ObjectAnimator.ofFloat(glow, "scaleY", 1.15f, 0.95f, 1.1f, 0.98f, 1.05f).apply {
            duration = 2500; startDelay = 1300
        }

        // Continuous ring rotations
        anims += ObjectAnimator.ofFloat(ringIn, "rotation", 0f, 360f).apply {
            duration = 12000; startDelay = 1400; repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        anims += ObjectAnimator.ofFloat(ringOut, "rotation", 0f, -360f).apply {
            duration = 18000; startDelay = 1500; repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }

        // Ring alpha pulse
        anims += ObjectAnimator.ofFloat(ringIn, "alpha", 1f, 0.4f, 1f).apply {
            duration = 2000; startDelay = 2000; repeatCount = ValueAnimator.INFINITE
        }
        anims += ObjectAnimator.ofFloat(ringOut, "alpha", 0.7f, 0.2f, 0.7f).apply {
            duration = 3000; startDelay = 2200; repeatCount = ValueAnimator.INFINITE
        }

        animatorSet = AnimatorSet().apply {
            playTogether(anims)
            start()
        }

        binding.splashRoot.postDelayed({ goToLogin() }, 3800)
    }

    private fun anim(
        target: android.view.View, prop: String,
        from: Float, to: Float, dur: Long, delay: Long,
        interp: android.view.animation.Interpolator? = null
    ): ObjectAnimator {
        return ObjectAnimator.ofFloat(target, prop, from, to).apply {
            duration = dur; startDelay = delay
            interp?.let { interpolator = it }
        }
    }

    private fun goToLogin() {
        if (navigated || isFinishing) return
        navigated = true
        binding.splashRoot.animate()
            .alpha(0f).setDuration(500)
            .withEndAction {
                startActivity(Intent(this, LoginActivity::class.java))
                overridePendingTransition(R.anim.splash_fade_in, R.anim.splash_fade_out)
                finish()
            }.start()
    }

    override fun onDestroy() {
        animatorSet?.cancel()
        binding.splashRoot.animate().cancel()
        super.onDestroy()
    }
}
