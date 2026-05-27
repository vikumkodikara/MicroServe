package com.example.microserve

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class CreateFeedbackActivity : AppCompatActivity() {

    private var selectedRating = 0
    private lateinit var stars: List<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_feedback)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        stars = listOf(
            findViewById(R.id.star1), findViewById(R.id.star2),
            findViewById(R.id.star3), findViewById(R.id.star4),
            findViewById(R.id.star5)
        )

        for (i in stars.indices) {
            stars[i].setOnClickListener { setRating(i + 1) }
        }

        findViewById<View>(R.id.btn_create).setOnClickListener {
            val feedback = findViewById<EditText>(R.id.et_feedback).text.toString().trim()

            if (selectedRating == 0) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (feedback.isEmpty()) {
                Toast.makeText(this, "Please enter feedback", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ownerUid = AppPreferences.getSessionUid(this)
            val userName = AppPreferences.getSessionName(this).ifBlank { "User" }

            FeedbackStore.addFeedback(
                context = this,
                ownerUid = ownerUid,
                userName = userName,
                message = feedback,
                rating = selectedRating
            )
            Toast.makeText(this, "Feedback created", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setRating(rating: Int) {
        selectedRating = rating
        for (i in stars.indices) {
            stars[i].setColorFilter(
                if (i < rating) 0xFF4a4458.toInt() else 0xFFCCCCCC.toInt()
            )
        }
    }
}
