package com.example.microserve

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
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

        val switchNotifications = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchNotifications)
        switchNotifications.isChecked = AppPreferences.isNotificationsEnabled(this)
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setNotificationsEnabled(this, isChecked)
            Toast.makeText(this, if (isChecked) "Notifications enabled" else "Notifications disabled", Toast.LENGTH_SHORT).show()
        }

        val switchDarkMode = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchDarkMode)
        switchDarkMode.isChecked = AppPreferences.isDarkModeEnabled(this)
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setDarkModeEnabled(this, isChecked)
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                if (isChecked) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        findViewById<View>(R.id.btn_logout).setOnClickListener {
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<View>(R.id.tv_edit_profile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

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
                startActivity(Intent(this, TermsOfServicesActivity::class.java))
            }
        } catch (_: Exception) { }

        try {
            findViewById<View>(R.id.btn_language).setOnClickListener {
                val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
                val sheetView = layoutInflater.inflate(R.layout.dialog_select_language, null)
                dialog.setContentView(sheetView)

                // Safe-guard to make Bottom Sheet background transparent so rounded corners render perfectly
                dialog.setOnShowListener {
                    val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
                }

                sheetView.findViewById<View>(R.id.btn_lang_english).setOnClickListener {
                    AppPreferences.setLanguage(this, "English")
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                        androidx.core.os.LocaleListCompat.forLanguageTags("en")
                    )
                    Toast.makeText(this, "Language changed to English", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }

                sheetView.findViewById<View>(R.id.btn_lang_sinhala).setOnClickListener {
                    AppPreferences.setLanguage(this, "Sinhala")
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                        androidx.core.os.LocaleListCompat.forLanguageTags("si")
                    )
                    Toast.makeText(this, "Language changed to Sinhala", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }

                sheetView.findViewById<View>(R.id.btn_lang_tamil).setOnClickListener {
                    AppPreferences.setLanguage(this, "Tamil")
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                        androidx.core.os.LocaleListCompat.forLanguageTags("ta")
                    )
                    Toast.makeText(this, "Language changed to Tamil", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }

                dialog.show()
            }
        } catch (_: Exception) { }
    }
}
