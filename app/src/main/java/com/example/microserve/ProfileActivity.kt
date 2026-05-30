package com.example.microserve

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
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
            
            // Set initial defaults before load
            txtPrice.text = "Loading price..."
            txtSchedule.text = "Loading schedule..."

            Log.d("ProfileActivity", "Provider received: name=${provider.name}, serviceId=${provider.serviceId}")

            if (provider.serviceId.isNotBlank()) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("services")
                    .document(provider.serviceId)
                    .get()
                    .addOnSuccessListener { document ->
                        Log.d("ProfileActivity", "Firestore doc fetched. Exists: ${document.exists()}")
                        if (document.exists()) {
                            Log.d("FirestoreData", "Data: " + document.data)
                            txtServices.text = document.getString("category") ?: provider.category
                            txtProfileLocation.text = document.getString("location") ?: provider.location
                            
                            val interior = document.getLong("interiorCount")?.toInt() ?: 0
                            val exterior = document.getLong("exteriorCount")?.toInt() ?: 0
                            
                            if (interior > 0 || exterior > 0) {
                                val parts = mutableListOf<String>()
                                if (interior > 0) parts.add("Interior: $interior M Points")
                                if (exterior > 0) parts.add("Exterior: $exterior M Points")
                                txtPrice.text = parts.joinToString("\n")
                            } else {
                                txtPrice.text = "Negotiable"
                            }

                            val start = document.getString("startTime") ?: ""
                            val end = document.getString("endTime") ?: ""
                            val days = document.getString("selectedDays") ?: ""
                            
                            if (start.isNotBlank() && end.isNotBlank()) {
                                txtSchedule.text = "$start TO $end\n$days"
                            } else {
                                txtSchedule.text = "Not Specified"
                            }
                        } else {
                            Log.w("ProfileActivity", "Document not found in 'services' for id: ${provider.serviceId}")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileActivity", "Failed to fetch service details", e)
                    }
            } else {
                txtPrice.text = "Negotiable"
                txtSchedule.text = "Not Specified"
            }
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
