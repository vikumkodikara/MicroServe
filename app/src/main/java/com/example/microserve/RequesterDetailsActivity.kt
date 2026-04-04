package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityRequesterDetailsBinding

/**
 * Displays the full details of a single requester/service request.
 * Receives data via Intent extras from RequestersActivity.
 */
class RequesterDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRequesterDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequesterDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        populateDetails()
        setupBackButton()
        setupActionButtons()
        setupBottomNavigation()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.requesterDetailsRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.detailsHeaderFrame.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun populateDetails() {
        with(intent) {
            binding.tvTitle.text = getStringExtra("TITLE") ?: "N/A"
            binding.tvCategory.text = getStringExtra("CATEGORY") ?: "N/A"
            binding.tvRequesterName.text = getStringExtra("REQUESTER_NAME") ?: "N/A"
            binding.tvLocation.text = getStringExtra("LOCATION") ?: "N/A"
            binding.tvStatus.text = getStringExtra("STATUS") ?: "Pending"
            binding.tvDescription.text = getStringExtra("DESCRIPTION") ?: "No description provided."
            binding.tvContact.text = getStringExtra("CONTACT") ?: "N/A"
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupActionButtons() {
        val requesterName = intent.getStringExtra("REQUESTER_NAME") ?: "this user"

        binding.btnBlockUser.setOnClickListener {
            // TODO: integrate with real block logic (API call / DB update)
            Toast.makeText(this, "Blocked $requesterName", Toast.LENGTH_SHORT).show()
        }

        binding.btnDeleteUser.setOnClickListener {
            // TODO: integrate with real delete logic (API call / DB delete)
            Toast.makeText(this, "Deleted $requesterName", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Homepage::class.java))
                    finishAffinity()
                    true
                }
                R.id.nav_profile -> true
                R.id.nav_settings -> true
                else -> false
            }
        }
    }
}
