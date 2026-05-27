package com.example.microserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class EditFeedbackActivity : AppCompatActivity() {

    private var selectedRating = 0
    private lateinit var stars: List<ImageView>
    private var feedbackId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_feedback)

        feedbackId = intent.getStringExtra("feedback_id") ?: ""
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        stars = listOf(
            findViewById(R.id.star1), findViewById(R.id.star2),
            findViewById(R.id.star3), findViewById(R.id.star4),
            findViewById(R.id.star5)
        )

        for (i in stars.indices) {
            stars[i].setOnClickListener { setRating(i + 1) }
        }

        val etFeedback = findViewById<EditText>(R.id.et_feedback)

        val feedback = FeedbackStore.getAllFeedbacks(this).firstOrNull { it.id == feedbackId }
        if (feedback == null) {
            Toast.makeText(this, "Feedback not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Verify ownership — only the owner can edit
        if (!FeedbackStore.isOwner(this, feedback)) {
            Toast.makeText(this, "You can only edit your own feedback", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etFeedback.setText(feedback.message)
        setRating(feedback.rating)

        findViewById<View>(R.id.btn_update).setOnClickListener {
            val message = etFeedback.text.toString().trim()
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter feedback", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            FeedbackStore.updateFeedback(this, feedbackId, message, selectedRating)
            Toast.makeText(this, "Feedback updated", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<View>(R.id.btn_delete).setOnClickListener {
            showDeleteDialog()
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

    private fun showDeleteDialog() {
        val dialog = android.app.AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog_MinWidth)
            .create()

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_feedback, null)
        dialog.setView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        view.findViewById<View>(R.id.btn_yes_delete).setOnClickListener {
            FeedbackStore.deleteFeedback(this, feedbackId)
            dialog.dismiss()
            Toast.makeText(this, "Feedback deleted", Toast.LENGTH_SHORT).show()
            finish()
        }
        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}
