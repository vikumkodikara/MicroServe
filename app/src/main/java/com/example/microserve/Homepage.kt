package com.example.microserve

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import com.bumptech.glide.Glide
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
        setupUserProfile()
        setupJobsScroll()
        setupDate()
        setupBannerToolsWatermarkScale()
        setupClickListeners()
        HomeBottomNavHelper.setup(this, HomeBottomNavHelper.TAB_HOME)
    }

    /**
     * Loads the logged-in user's name and profile photo from session.
     */
    private fun setupUserProfile() {
        val name = AppPreferences.getSessionName(this)
        val photoUrl = AppPreferences.getSessionPhotoUrl(this)

        // Set user name (fallback to "User" if empty)
        binding.userName.text = if (name.isNotBlank()) name else "User"

        // Load profile photo
        if (photoUrl.isNotBlank()) {
            Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.navprofile)
                .error(buildInitialsDrawable(name))
                .into(binding.profileAvatar)
        } else {
            binding.profileAvatar.setImageDrawable(buildInitialsDrawable(name))
        }
    }

    /**
     * Creates a circular drawable with the user's initial letter.
     */
    private fun buildInitialsDrawable(name: String): Drawable {
        val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
        val colors = intArrayOf(
            Color.parseColor("#6C63FF"),
            Color.parseColor("#FF6584"),
            Color.parseColor("#43B581"),
            Color.parseColor("#FAA61A"),
            Color.parseColor("#F47B67")
        )
        val bgColor = colors[initial.hashCode().and(0x7FFFFFFF) % colors.size]

        return object : Drawable() {
            private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
            private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = 22f
                isFakeBoldText = true
            }

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val radius = minOf(bounds.width(), bounds.height()) / 2f
                canvas.drawCircle(cx, cy, radius, bgPaint)
                textPaint.textSize = radius * 0.9f
                val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
                canvas.drawText(initial, cx, textY, textPaint)
            }

            override fun setAlpha(alpha: Int) { bgPaint.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { bgPaint.colorFilter = colorFilter }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun getOpacity(): Int = PixelFormat.OPAQUE
            override fun getIntrinsicWidth() = 128
            override fun getIntrinsicHeight() = 128
        }
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

    /** Sticky title sits over the list — forward drags so NestedScrollView still scrolls. */
    private fun setupJobsScroll() {
        binding.previouslyJobsSectionHeader.setOnTouchListener { _, event ->
            binding.scrollView.dispatchTouchEvent(event)
            true
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            // Extend purple below the bar art — do not pad/squash the 68dp nav layers
            findViewById<android.view.View>(R.id.navSystemBarSpacer)?.let { spacer ->
                val lp = spacer.layoutParams
                if (lp.height != systemBars.bottom) {
                    lp.height = systemBars.bottom
                    spacer.layoutParams = lp
                }
            }
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

