package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import com.example.microserve.databinding.ActivityHomeBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Homepage : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupDate()
        setupBannerToolsWatermarkScale()
        setupClickListeners()
        HomeBottomNavHelper.setup(this, HomeBottomNavHelper.TAB_HOME)
    }

    /**
     * Blows up the soft tool watermark so it reads clearly in the shallow banner strip.
     */
    private fun setupBannerToolsWatermarkScale() {
        binding.bannerToolsBackdrop.doOnLayout {
            val iv = binding.bannerToolsBackdrop
            if (iv.width == 0 || iv.height == 0) return@doOnLayout
            iv.pivotX = iv.width * 0.5f
            iv.pivotY = iv.height * 0.5f
            val s = 2.35f
            iv.scaleX = s
            iv.scaleY = s
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Pad content only — bottom nav stays full-width purple bar above gesture area
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            findViewById<android.view.View>(R.id.customBottomNav)?.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupDate() {
        val dateFormat = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault())
        binding.dateText.text = dateFormat.format(Date())
    }

    private fun setupClickListeners() {
        // Settings button
        binding.settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Action chips
        binding.chipPostService.setOnClickListener {
            startActivity(Intent(this, PostServiceActivity::class.java))
        }

        binding.chipRequestService.setOnClickListener {
            startActivity(Intent(this, RequestServiceActivity::class.java))
        }

        binding.chipPostAds.setOnClickListener {
            startActivity(Intent(this, PostAdsActivity::class.java))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
