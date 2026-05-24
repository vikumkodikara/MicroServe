package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.microserve.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var navigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        startLoadingAnimation()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.splashRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }
    }

    private fun startLoadingAnimation() {
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.splash_logo_enter)
        binding.splashLogo.startAnimation(logoAnim)

        binding.splashLogo.postDelayed({
            binding.splashAppName.animate()
                .alpha(1f)
                .setDuration(400)
                .start()
            binding.splashProgress.isVisible = true
        }, 350)

        val holdMs = resources.getInteger(R.integer.splash_hold_duration).toLong()
        val logoMs = resources.getInteger(R.integer.splash_logo_anim_duration).toLong()
        binding.splashRoot.postDelayed({ goToLogin() }, logoMs + holdMs)
    }

    private fun goToLogin() {
        if (navigated || isFinishing) return
        navigated = true

        binding.splashRoot.animate()
            .alpha(0f)
            .setDuration(resources.getInteger(R.integer.splash_exit_anim_duration).toLong())
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
