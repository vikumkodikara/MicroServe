package com.example.microserve

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog

class SettingsFragmentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fragment_settings)

        val root = findViewById<View>(R.id.settingsFragRoot)
        val header = findViewById<View>(R.id.headerContainer)
        val footer = findViewById<View>(R.id.footerBar)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            header.setPadding(0, systemBars.top, 0, 0)
            footer?.let { f ->
                val lp = f.layoutParams
                lp.height = (48 * resources.displayMetrics.density).toInt() + systemBars.bottom
                f.layoutParams = lp
                f.setPadding(0, 0, 0, systemBars.bottom)
            }
            insets
        }

        try {
            val title = findViewById<TextView>(R.id.tv_title)
            title?.text = "Settings"
            title?.textSize = 35f
            title?.setTextColor(Color.WHITE)
            title?.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        } catch (_: Exception) { }

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchNotifications)?.apply {
            isChecked = AppPreferences.isNotificationsEnabled(this@SettingsFragmentActivity)
            setOnCheckedChangeListener { _, isChecked ->
                AppPreferences.setNotificationsEnabled(this@SettingsFragmentActivity, isChecked)
                Toast.makeText(
                    this@SettingsFragmentActivity,
                    if (isChecked) "Notifications enabled" else "Notifications disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchDarkMode)?.apply {
            isChecked = AppPreferences.isDarkModeEnabled(this@SettingsFragmentActivity)
            setOnCheckedChangeListener { _, isChecked ->
                AppPreferences.setDarkModeEnabled(this@SettingsFragmentActivity, isChecked)
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) {
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                    } else {
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                    }
                )
            }
        }

        findViewById<View>(R.id.btn_logout).setOnClickListener {
            SessionNavigator.clearAuth(this)
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            startActivity(SessionNavigator.loginIntent(this))
            finish()
        }

        findViewById<View>(R.id.tv_edit_profile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<View>(R.id.profileCard)?.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        bindProfileCard()

        findViewById<View>(R.id.btn_personal_info).setOnClickListener {
            startActivity(Intent(this, PersonalInfoActivity::class.java))
        }

        try {
            findViewById<View>(R.id.btn_saved_addresses).setOnClickListener {
                startActivity(Intent(this, SavedAddressActivity::class.java))
            }
        } catch (_: Exception) { }

        try {
            findViewById<View>(R.id.btn_payment_methods).setOnClickListener {
                startActivity(Intent(this, WalletActivity::class.java))
            }
        } catch (_: Exception) { }

        try {
            findViewById<View>(R.id.btn_get_help).setOnClickListener {
                startActivity(Intent(this, GetHelpActivity::class.java))
            }
        } catch (_: Exception) { }

        try {
            findViewById<View>(R.id.btn_privacy_policy).setOnClickListener {
                startActivity(Intent(this, PrivacyPolicyActivity::class.java))
            }
        } catch (_: Exception) { }

        try {
            findViewById<View>(R.id.btn_terms_of_service).setOnClickListener {
                startActivity(Intent(this, TermsOfServiceActivity::class.java))
            }
        } catch (_: Exception) { }

        try {
            findViewById<View>(R.id.btn_language).setOnClickListener {
                showLanguageDialog()
            }
        } catch (_: Exception) { }
    }

    override fun onResume() {
        super.onResume()
        bindProfileCard()
    }

    private fun bindProfileCard() {
        val name = AppPreferences.getSessionName(this)
        findViewById<TextView>(R.id.tv_name)?.text =
            if (name.isNotBlank()) name else getString(R.string.dummy_user_name)

        findViewById<ImageView>(R.id.img_profile)?.let { avatar ->
            ProfilePhotoHelper.loadAvatar(this, avatar, name)
        }
    }

    private fun showLanguageDialog() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_select_language, null)
        dialog.setContentView(view)

        // Make the parent container transparent so our custom rounded neumorphic background is visible without any white corners
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.setBackgroundResource(android.R.color.transparent)

        // Identify current language selection (handle legacy/inconsistent preference storage)
        val rawLanguage = AppPreferences.getLanguage(this)
        val currentLanguage = when (rawLanguage) {
            "en", "English" -> "English"
            "si", "Sinhala" -> "Sinhala"
            "ta", "Tamil" -> "Tamil"
            else -> "English"
        }

        // Style helper to highlight selected language with premium black background and white text
        fun highlightButton(cardId: Int, textId: Int, isSelected: Boolean) {
            val card = view.findViewById<com.google.android.material.card.MaterialCardView>(cardId)
            val text = view.findViewById<TextView>(textId)
            if (isSelected) {
                card.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(Color.BLACK))
                card.strokeColor = Color.BLACK
                text.setTextColor(Color.WHITE)
            } else {
                card.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#F5F5F5")))
                card.strokeColor = Color.parseColor("#E0E0E0")
                text.setTextColor(Color.parseColor("#1A1A1A"))
            }
        }

        highlightButton(R.id.btn_english, R.id.tv_english, currentLanguage == "English")
        highlightButton(R.id.btn_sinhala, R.id.tv_sinhala, currentLanguage == "Sinhala")
        highlightButton(R.id.btn_tamil, R.id.tv_tamil, currentLanguage == "Tamil")

        fun applyLanguage(language: String, langCode: String) {
            AppPreferences.setLanguage(this, language)
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(langCode)
            )
            dialog.dismiss()
            recreate()
        }

        view.findViewById<View>(R.id.btn_english).setOnClickListener {
            applyLanguage("English", "en")
        }
        view.findViewById<View>(R.id.btn_sinhala).setOnClickListener {
            applyLanguage("Sinhala", "si")
        }
        view.findViewById<View>(R.id.btn_tamil).setOnClickListener {
            applyLanguage("Tamil", "ta")
        }

        dialog.show()
    }
}
