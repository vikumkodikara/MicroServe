package com.example.microserve

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RatingActivity : AppCompatActivity() {

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

        setContentView(R.layout.activity_rating)

        val providerName = intent.getStringExtra("PROVIDER_NAME") ?: "Provider"
        val providerUid = intent.getStringExtra("PROVIDER_UID") ?: ""
        val requestId = intent.getStringExtra("REQUEST_ID") ?: ""

        val txtRateTitle: TextView = findViewById(R.id.txtRateTitle)
        val ratingBar: RatingBar = findViewById(R.id.ratingBar)
        val etComment: EditText = findViewById(R.id.etComment)
        val btnSubmit: Button = findViewById(R.id.btnSubmit)

        txtRateTitle.text = "Rate $providerName"

        btnSubmit.setOnClickListener {
            val stars = ratingBar.rating
            val comment = etComment.text.toString().trim()

            if (stars == 0f) {
                Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            val rating = Rating(
                requestId = requestId,
                providerUid = providerUid,
                requesterUid = currentUid,
                stars = stars,
                comment = comment
            )

            // Submit to Firebase
            RatingRepository.submitRating(
                rating = rating,
                onSuccess = {
                    Toast.makeText(this, "Thank you for your review!", Toast.LENGTH_SHORT).show()
                    // Navigate back to main Service page
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                },
                onFailure = { error ->
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
