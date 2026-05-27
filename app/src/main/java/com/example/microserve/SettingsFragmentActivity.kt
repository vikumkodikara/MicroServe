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
            val lp = footer.layoutParams
            lp.height = (48 * resources.displayMetrics.density).toInt() + systemBars.bottom
            footer.layoutParams = lp
            footer.setPadding(0, 0, 0, systemBars.bottom)
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
        val dialog = android.app.AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog_MinWidth)
            .create()

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_select_language, null)
        dialog.setView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        view.findViewById<View>(R.id.btn_english).setOnClickListener {
            AppPreferences.setLanguage(this, "en")
            Toast.makeText(this, "Language set to English", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_sinhala).setOnClickListener {
            AppPreferences.setLanguage(this, "si")
            Toast.makeText(this, "Language set to Sinhala", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_tamil).setOnClickListener {
            AppPreferences.setLanguage(this, "ta")
            Toast.makeText(this, "Language set to Tamil", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}
