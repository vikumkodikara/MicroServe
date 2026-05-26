package com.example.microserve

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivitySettingsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var initializingSwitches = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupBackButton()
        setupBottomNavigation()
        setupInitialState()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        bindAdminProfileCard()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.settingsRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.settingsHeaderFrame.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, AdminProfileActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_settings
    }

    private fun setupInitialState() {
        binding.switchNotifications.isChecked = AppPreferences.isNotificationsEnabled(this)
        binding.switchDarkMode.isChecked = AppPreferences.isDarkModeEnabled(this)
        binding.tvLanguageValue.text = AppPreferences.getLanguage(this)
        bindAdminProfileCard()
        initializingSwitches = false
    }

    private fun bindAdminProfileCard() {
        val admin = UserStore.getOrCreateAdminUser(this)
        binding.tvSettingsAdminName.text = admin.name
        binding.tvSettingsEditProfile.text = admin.email
    }

    private fun setupClickListeners() {
        binding.profileCard.setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (initializingSwitches) return@setOnCheckedChangeListener
            AppPreferences.setNotificationsEnabled(this, isChecked)
            Toast.makeText(this, if (isChecked) "Notifications enabled" else "Notifications disabled", Toast.LENGTH_SHORT).show()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (initializingSwitches) return@setOnCheckedChangeListener
            AppPreferences.setDarkModeEnabled(this, isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.rowLanguage.setOnClickListener {
            showLanguagePicker()
        }

        binding.rowGetHelp.setOnClickListener {
            openSupportEmail()
        }

        binding.rowPrivacyPolicy.setOnClickListener {
            showInfoDialog(
                title = "Privacy Policy",
                message = "MicroServe stores your activity data only for service management. Contact support for account data requests."
            )
        }

        binding.rowTerms.setOnClickListener {
            showInfoDialog(
                title = "Terms of Services",
                message = "By using MicroServe, you agree to responsible usage, valid service requests, and platform moderation rules."
            )
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            GoogleSignIn.getClient(
                this,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut()

            AppPreferences.clearSession(this)

            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun showLanguagePicker() {
        val languages = arrayOf("English", "Sinhala", "Tamil")
        val currentLanguage = AppPreferences.getLanguage(this)
        val selectedIndex = languages.indexOf(currentLanguage).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setSingleChoiceItems(languages, selectedIndex) { dialog, which ->
                val language = languages[which]
                AppPreferences.setLanguage(this, language)
                binding.tvLanguageValue.text = language
                Toast.makeText(this, "Language set to $language", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openSupportEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:support@microserve.local")
            putExtra(Intent.EXTRA_SUBJECT, "MicroServe Help Request")
        }

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
