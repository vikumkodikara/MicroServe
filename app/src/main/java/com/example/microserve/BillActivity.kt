package com.example.microserve

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BillActivity : AppCompatActivity() {

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

        setContentView(R.layout.activity_bill)

        val btnNext: Button = findViewById(R.id.btnNext)
        
        val providerName = intent.getStringExtra("PROVIDER_NAME") ?: "Unknown Provider"
        val category = intent.getStringExtra("CATEGORY") ?: "Service"
        
        val txtBillProvider: TextView = findViewById(R.id.txtBillProvider)
        val txtBillService: TextView = findViewById(R.id.txtBillService)
        
        txtBillProvider.text = "Service Provider: $providerName"
        txtBillService.text = "Service: $category"
        
        btnNext.setOnClickListener {
            val intent = Intent(this, RatingActivity::class.java)
            intent.putExtra("PROVIDER_NAME", providerName)
            startActivity(intent)
        }
    }
}
