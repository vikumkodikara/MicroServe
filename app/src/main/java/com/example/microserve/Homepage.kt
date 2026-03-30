package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityHomepageBinding

class Homepage : AppCompatActivity() {

    private lateinit var binding: ActivityHomepageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityHomepageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupClickListeners()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupClickListeners() {
        // Navigation to PostAddActivity
        binding.postServiceBtn.setOnClickListener {
            startActivity(Intent(this, PostAddActivity::class.java))
        }

        binding.navPost.setOnClickListener {
            startActivity(Intent(this, PostAddActivity::class.java))
        }

        // Header buttons
        binding.settingsBtn.setOnClickListener {
            showToast("Settings clicked")
        }

        binding.menuBtn.setOnClickListener {
            showToast("Menu clicked")
        }

        // Action Chips
        binding.requestServiceBtn.setOnClickListener {
            showToast("Request a Service clicked")
        }

        binding.postJobBtn.setOnClickListener {
            startActivity(Intent(this, EditPostActivity::class.java))
        }

        // Bottom Navigation
        binding.navRequest.setOnClickListener {
            showToast("Navigation: Request")
        }

        binding.navService.setOnClickListener {
            showToast("Navigation: Service")
        }

        binding.navHome.setOnClickListener {
            showToast("Navigation: Home")
        }

        binding.navProfile.setOnClickListener {
            showToast("Navigation: Profile")
        }
        
        // Cards
        binding.servicesCard.setOnClickListener {
            showToast("Services Card clicked")
        }
        
        binding.jobCard.setOnClickListener {
            showToast("Previous Job clicked")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}