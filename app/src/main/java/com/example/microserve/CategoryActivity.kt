package com.example.microserve

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge to edge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.statusBarColor = Color.TRANSPARENT
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.statusBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_category)

        val categoryName = intent.getStringExtra("CATEGORY_NAME") ?: "Category"
        
        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }
        
        val txtCategoryTitle: TextView = findViewById(R.id.txtCategoryTitle)
        txtCategoryTitle.text = "$categoryName Category"
        
        val imgCategory: ImageView = findViewById(R.id.imgCategory)
        // Dynamically set image based on category if needed, using a fallback for now.
        imgCategory.setImageResource(R.drawable.img_plumbing)

        val rvProviders: RecyclerView = findViewById(R.id.rvProviders)
        rvProviders.layoutManager = LinearLayoutManager(this)
        
        val providers = listOf(
            Provider("1", "Sisira Kumara", "Kalutara, Western Province", 4.0f, categoryName),
            Provider("2", "Sunil Perera", "Mathugama, Western Province", 3.0f, categoryName),
            Provider("3", "Mayura Silva", "Malabe, Western Province", 4.5f, categoryName)
        )
        
        val adapter = ProviderAdapter(providers) { provider ->
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("PROVIDER", provider)
            startActivity(intent)
        }
        rvProviders.adapter = adapter
    }
}
