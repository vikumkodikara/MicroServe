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

        setupWindowInsets()
        setupSpinner()
        setupClickListeners()
        UserBottomNavHelper.setup(this, activeTab)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        activeTab = intent.getStringExtra(UserBottomNavHelper.EXTRA_ACTIVE_TAB)
            ?: UserBottomNavHelper.TAB_POST
        UserBottomNavHelper.setup(this, activeTab)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.headerContainer.setPadding(
                binding.headerContainer.paddingLeft,
                systemBars.top + 16,
                binding.headerContainer.paddingRight,
                binding.headerContainer.paddingBottom
            )
            findViewById<android.view.View>(R.id.navContainer)?.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
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

        binding.editBtn.setOnClickListener {
            val intent = Intent(this, EditPostActivity::class.java)
            intent.putExtra("category", "Plumbing")
            intent.putExtra("provider_name", "Sunil")
            intent.putExtra("location", "Galle")
            intent.putExtra("contact", "072587456")
            intent.putExtra("email", "Sunil@gmail.com")
            startActivity(intent)
        }
    }
}
