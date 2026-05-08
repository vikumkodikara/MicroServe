package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityAdminProfileBinding

class AdminProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminProfileBinding
    private lateinit var adminUser: UserStore.User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupBottomNavigation()
        setupClickListeners()
        loadAdminData()
    }

    override fun onResume() {
        super.onResume()
        loadAdminData()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.adminProfileRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.profileHeaderFrame.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Homepage::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_profile
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSaveChanges.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadAdminData() {
        adminUser = UserStore.getOrCreateAdminUser(this)
        binding.etUsername.setText(adminUser.name)
        binding.etEmail.setText(adminUser.email)
        binding.etCurrentPassword.setText("")
        binding.etNewPassword.setText("")
        binding.etConfirmPassword.setText("")
    }

    private fun saveProfile() {
        val name = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val currentPassword = binding.etCurrentPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (name.isBlank() || email.isBlank() || currentPassword.isBlank()) {
            Toast.makeText(this, "Username, email, and current password are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }

        val wantsPasswordUpdate = newPassword.isNotBlank() || confirmPassword.isNotBlank()
        if (wantsPasswordUpdate) {
            if (newPassword.length < 6) {
                Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(this, "New password and confirm password do not match", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val result = UserStore.updateAdminProfile(
            context = this,
            adminId = adminUser.id,
            name = name,
            email = email,
            currentPassword = currentPassword,
            newPassword = if (wantsPasswordUpdate) newPassword else null
        )

        Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
        if (result.success) {
            loadAdminData()
        }
    }
}
