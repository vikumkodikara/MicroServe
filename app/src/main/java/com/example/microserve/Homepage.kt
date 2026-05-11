package com.example.microserve

import android.content.Intent
import android.os.Bundle
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Navigation to Request Service
        binding.requestServiceBtn.setOnClickListener {
            startActivity(Intent(this, RequestServiceActivity::class.java))
        }

        // Navigation to Post Service
        binding.postServiceBtn.setOnClickListener {
            startActivity(Intent(this, PostServiceActivity::class.java))
        }

        // Navigation to Post Ads (Fixed binding error)
        binding.navPost.setOnClickListener {
            startActivity(Intent(this, PostAdsActivity::class.java))
        }
        
        // Navigation from Bottom Nav
        binding.navRequest.setOnClickListener {
            startActivity(Intent(this, RequestServiceActivity::class.java))
        }
        
        binding.navService.setOnClickListener {
            startActivity(Intent(this, PostServiceActivity::class.java))
        }

        // Navigation to SettingsFragment
        binding.settingsBtn.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
