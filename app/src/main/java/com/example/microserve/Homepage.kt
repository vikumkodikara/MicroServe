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
        // Quick Action buttons
        binding.quickRequestsBtn.setOnClickListener {
            showToast("Requests clicked")
        }

        binding.quickServicesBtn.setOnClickListener {
            showToast("Services clicked")
        }

        binding.quickTransactionsBtn.setOnClickListener {
            showToast("Transactions clicked")
        }

        binding.quickFeedbacksBtn.setOnClickListener {
            startActivity(Intent(this, EditPostActivity::class.java))
        }

        binding.quickUsersBtn.setOnClickListener {
            startActivity(Intent(this, PostAddActivity::class.java))
        }

        // Bottom Navigation actions
        binding.navHome.setOnClickListener {
            showToast("Navigation: Home")
        }

        binding.navProfile.setOnClickListener {
            showToast("Navigation: Profile")
        }

        binding.navSettings.setOnClickListener {
            showToast("Navigation: Settings")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
