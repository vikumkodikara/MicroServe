package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityUserDetailsBinding

/**
 * Displays full details of a single user with block/delete actions.
 */
class UserDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDetailsBinding
    private var userId: String? = null
    private var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        populateDetails()
        setupBackButton()
        setupActionButtons()
        setupBottomNavigation()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.userDetailsRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.detailsHeaderFrame.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun populateDetails() {
        with(intent) {
            userId = getStringExtra("USER_ID")
            userName = getStringExtra("USER_NAME")
            val email = getStringExtra("USER_EMAIL") ?: "N/A"
            val phone = getStringExtra("USER_PHONE") ?: "N/A"
            val type = getStringExtra("USER_TYPE") ?: "User"
            val status = getStringExtra("USER_STATUS") ?: "Active"

            binding.tvUserName.text = userName ?: "Unknown"
            binding.tvUserEmail.text = email
            binding.tvUserPhone.text = phone
            binding.tvUserType.text = type
            binding.tvUserStatus.text = status

            val statusBg = when (status) {
                "Active" -> R.drawable.active_tag_bg
                "Banned" -> R.drawable.delete_user_button_bg
                else -> R.drawable.tab_inactive_bg
            }
            binding.tvUserStatus.setBackgroundResource(statusBg)

            val localUser = userId?.let { UserStore.getUserById(this@UserDetailsActivity, it) }
            if (localUser != null) {
                binding.tvUserCashPoints.text = localUser.cashPoints.toString()
            } else {
                userId?.let { uid ->
                    UserRepository.getProfileById(
                        uid = uid,
                        onSuccess = { profile ->
                            binding.tvUserCashPoints.text = profile.cashPoints.toString()
                        },
                        onFailure = {
                            binding.tvUserCashPoints.text = "0"
                        }
                    )
                } ?: run {
                    binding.tvUserCashPoints.text = "0"
                }
            }
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupActionButtons() {
        binding.btnBlockUser.setOnClickListener {
            val id = userId
            if (id.isNullOrBlank()) {
                Toast.makeText(this, "Unable to block user", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updated = UserStore.banUser(this, id)
            if (updated) {
                Toast.makeText(this, "User banned", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "User already banned or not found", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDeleteUser.setOnClickListener {
            val id = userId
            if (id.isNullOrBlank()) {
                Toast.makeText(this, "Unable to delete user", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val deleted = UserStore.deleteUser(this, id)
            if (deleted) {
                Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        val homeTab = findViewById<android.widget.LinearLayout>(R.id.navTabHome)
        val profileTab = findViewById<android.widget.LinearLayout>(R.id.navTabProfile)
        val settingsTab = findViewById<android.widget.LinearLayout>(R.id.navTabSettings)
        val bubbleIcon = findViewById<android.widget.ImageView>(R.id.navBubbleIcon)

        homeTab.setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
            finishAffinity()
        }
        profileTab.setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
            finishAffinity()
        }
        settingsTab.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finishAffinity()
        }
        bubbleIcon.setImageResource(R.drawable.ic_nav_profile)
    }
}
