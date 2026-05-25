package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityPostAdsBinding

class PostAdsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostAdsBinding
    private var activeTab: String = UserBottomNavHelper.TAB_POST

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPostAdsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activeTab = intent.getStringExtra(UserBottomNavHelper.EXTRA_ACTIVE_TAB)
            ?: UserBottomNavHelper.TAB_POST

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupSpinner()
        setupClickListeners()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        activeTab = intent.getStringExtra(UserBottomNavHelper.EXTRA_ACTIVE_TAB)
            ?: UserBottomNavHelper.TAB_POST
    }

    private fun setupSpinner() {
        val categories = arrayOf("-Select-", "Plumbing", "Electrical", "House Painting", "Carpentry", "Cleaning")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            finish()
        }

        // Navigate to EditPostActivity when clicking the Edit button in Previous Posts
        binding.editBtn.setOnClickListener {
            val intent = Intent(this, EditPostActivity::class.java)
            // Passing sample data to pre-fill the edit screen
            intent.putExtra("category", "Plumbing")
            intent.putExtra("provider_name", "Sunil")
            intent.putExtra("location", "Galle")
            intent.putExtra("contact", "072587456")
            intent.putExtra("email", "Sunil@gmail.com")
            startActivity(intent)
        }
    }
}
