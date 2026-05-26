package com.example.microserve

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
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
    private var isMenuOpen = false
    private var screenWidth = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        screenWidth = resources.displayMetrics.widthPixels.toFloat()

        setupWindowInsets()
        setupUserProfile()
        setupJobsScroll()
        setupDate()
        setupBannerToolsWatermarkScale()
        setupClickListeners()
        setupSideMenu()
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
            findViewById<View>(R.id.navSystemBarSpacer)?.let { spacer ->
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
        binding.settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsFragmentActivity::class.java))
        }

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

    private fun setupSideMenu() {
        binding.menuBtn.setOnClickListener { openMenu() }
        binding.menuCloseBtn.setOnClickListener { closeMenu() }

        binding.menuAccount.setOnClickListener {
            closeMenu()
            startActivity(Intent(this, AdminProfileActivity::class.java))
        }
        binding.menuFeedback.setOnClickListener {
            closeMenu()
            startActivity(Intent(this, FeedbacksActivity::class.java))
        }
        binding.menuAbout.setOnClickListener {
            closeMenu()
            showToast("About us")
        }
        binding.menuContact.setOnClickListener {
            closeMenu()
            showToast("Contact us")
        }
        binding.menuWallet.setOnClickListener {
            closeMenu()
            showToast("Wallet")
        }
    }

    private fun openMenu() {
        if (isMenuOpen) return
        isMenuOpen = true

        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#483C72"))

        val card = binding.mainContentCard
        val back = binding.backShadowCard
        val menu = binding.sideMenuPanel

        menu.visibility = View.VISIBLE

        val mainScale = 0.80f
        val mainShift = screenWidth * -0.50f
        val backScaleX = 0.82f
        val backShift = screenWidth * -0.48f

        val anims = AnimatorSet()
        anims.playTogether(
            ObjectAnimator.ofFloat(card, "scaleX", 1f, mainScale),
            ObjectAnimator.ofFloat(card, "scaleY", 1f, mainScale),
            ObjectAnimator.ofFloat(card, "translationX", 0f, mainShift),
            ObjectAnimator.ofFloat(card, "radius", 0f, 40f),
            ObjectAnimator.ofFloat(card, "cardElevation", 12f, 24f),

            ObjectAnimator.ofFloat(back, "alpha", 0f, 0.6f),
            ObjectAnimator.ofFloat(back, "scaleX", 1f, backScaleX),
            ObjectAnimator.ofFloat(back, "scaleY", 1f, mainScale),
            ObjectAnimator.ofFloat(back, "translationX", 0f, backShift),

            ObjectAnimator.ofFloat(menu, "translationX", screenWidth * 0.5f, 0f),
            ObjectAnimator.ofFloat(menu, "alpha", 0f, 1f)
        )
        anims.duration = 400
        anims.interpolator = DecelerateInterpolator(1.8f)
        anims.start()
    }

    private fun closeMenu() {
        if (!isMenuOpen) return
        isMenuOpen = false

        window.decorView.setBackgroundColor(android.graphics.Color.WHITE)

        val card = binding.mainContentCard
        val back = binding.backShadowCard
        val menu = binding.sideMenuPanel

        val mainScale = 0.80f
        val mainShift = screenWidth * -0.50f
        val backScaleX = 0.82f
        val backShift = screenWidth * -0.48f

        val anims = AnimatorSet()
        anims.playTogether(
            ObjectAnimator.ofFloat(card, "scaleX", mainScale, 1f),
            ObjectAnimator.ofFloat(card, "scaleY", mainScale, 1f),
            ObjectAnimator.ofFloat(card, "translationX", mainShift, 0f),
            ObjectAnimator.ofFloat(card, "radius", 40f, 0f),
            ObjectAnimator.ofFloat(card, "cardElevation", 24f, 12f),

            ObjectAnimator.ofFloat(back, "alpha", 0.6f, 0f),
            ObjectAnimator.ofFloat(back, "scaleX", backScaleX, 1f),
            ObjectAnimator.ofFloat(back, "scaleY", mainScale, 1f),
            ObjectAnimator.ofFloat(back, "translationX", backShift, 0f),

            ObjectAnimator.ofFloat(menu, "translationX", 0f, screenWidth * 0.5f),
            ObjectAnimator.ofFloat(menu, "alpha", 1f, 0f)
        )
        anims.duration = 300
        anims.interpolator = DecelerateInterpolator(1.5f)
        anims.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                menu.visibility = View.INVISIBLE
            }
        })
        anims.start()
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        if (isMenuOpen) {
            closeMenu()
        } else {
            super.onBackPressed()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

