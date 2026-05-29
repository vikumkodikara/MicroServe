package com.example.microserve

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.statusBarColor = Color.TRANSPARENT
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.statusBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_profile)

        val provider = intent.getSerializableExtra("PROVIDER") as? Provider

        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val txtProviderNameTitle: TextView = findViewById(R.id.txtProviderNameTitle)
        val ratingBarHeader: RatingBar = findViewById(R.id.ratingBarHeader)
        val txtServices: TextView = findViewById(R.id.txtServices)
        val txtProfileLocation: TextView = findViewById(R.id.txtProfileLocation)
        val txtPrice: TextView = findViewById(R.id.txtPrice)
        val txtSchedule: TextView = findViewById(R.id.txtSchedule)

        if (provider != null) {
            txtProviderNameTitle.text = provider.name
            ratingBarHeader.rating = provider.rating
            txtProfileLocation.text = provider.location
            txtServices.text = provider.category
            // Using placeholder text for pricing and schedule since they aren't fully fleshed out in the simplified Provider model yet, 
            // but we can extend this or just leave the mock XML text as is.
        }

        val btnPurchase: Button = findViewById(R.id.btnPurchase)
        btnPurchase.setOnClickListener {
            val intent = Intent(this, ServiceRequestActivity::class.java)
            if (provider != null) {
                intent.putExtra("PROVIDER_NAME", provider.name)
                intent.putExtra("CATEGORY", provider.category)
            }
            startActivity(intent)
        }
    }
}
