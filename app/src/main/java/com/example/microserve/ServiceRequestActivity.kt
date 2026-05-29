package com.example.microserve

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ServiceRequestActivity : AppCompatActivity() {

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

        setContentView(R.layout.activity_service_request)

        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val btnSend: Button = findViewById(R.id.btnSend)
        btnSend.setOnClickListener {
            // Retrieve simple intent extras to pass forward for billing
            val providerName = intent.getStringExtra("PROVIDER_NAME") ?: "Unknown Provider"
            val category = intent.getStringExtra("CATEGORY") ?: "Service"
            
            val intent = Intent(this, BillActivity::class.java)
            intent.putExtra("PROVIDER_NAME", providerName)
            intent.putExtra("CATEGORY", category)
            startActivity(intent)
        }
    }
}
