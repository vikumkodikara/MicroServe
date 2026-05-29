package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserFeedbacksActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_feedbacks)

        container = findViewById(R.id.feedbackListContainer)
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.fab_add_feedback).setOnClickListener {
            startActivity(Intent(this, CreateFeedbackActivity::class.java))
        }

        FeedbackStore.startListening(this)
    }

    override fun onResume() {
        super.onResume()
        loadFeedbacks()
    }

    override fun onDestroy() {
        FeedbackStore.stopListening()
        super.onDestroy()
    }

    private fun loadFeedbacks() {
        FeedbackStore.loadFromFirestore(this) { feedbacks ->
            renderFeedbacks(feedbacks)
        }
    }

    private fun renderFeedbacks(feedbacks: List<FeedbackStore.Feedback>) {
        container.removeAllViews()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        for (fb in feedbacks) {
            val card = LayoutInflater.from(this)
                .inflate(R.layout.item_user_feedback, container, false)

            card.findViewById<TextView>(R.id.tv_message).text = fb.message
            card.findViewById<TextView>(R.id.tv_date).text = dateFormat.format(Date(fb.createdAt))

            val stars = listOf<ImageView>(
                card.findViewById(R.id.star1),
                card.findViewById(R.id.star2),
                card.findViewById(R.id.star3),
                card.findViewById(R.id.star4),
                card.findViewById(R.id.star5)
            )
            for (i in stars.indices) {
                stars[i].setColorFilter(
                    if (i < fb.rating) 0xFFFFD700.toInt() else 0xFFE0E0E0.toInt()
                )
            }

            val btnMenu = card.findViewById<View>(R.id.btn_menu)
            val isOwner = FeedbackStore.isOwner(this, fb)

            if (isOwner) {
                // Show edit/delete menu only for own feedbacks
                btnMenu.visibility = View.VISIBLE
                btnMenu.setOnClickListener { anchor ->
                    val popup = PopupMenu(this, anchor)
                    popup.menu.add(0, 1, 0, "Edit")
                    popup.menu.add(0, 2, 1, "Delete")
                    popup.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            1 -> {
                                startActivity(
                                    Intent(this, EditFeedbackActivity::class.java)
                                        .putExtra("feedback_id", fb.id)
                                )
                                true
                            }
                            2 -> {
                                showDeleteDialog(fb.id)
                                true
                            }
                            else -> false
                        }
                    }
                    popup.show()
                }

                card.setOnClickListener {
                    startActivity(
                        Intent(this, EditFeedbackActivity::class.java)
                            .putExtra("feedback_id", fb.id)
                    )
                }
            } else {
                // Hide menu for other users' feedbacks
                btnMenu.visibility = View.GONE
                card.isClickable = false
            }

            container.addView(card)
        }

        if (feedbacks.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No feedbacks yet. Tap + to add yours!"
                textSize = 15f
                setTextColor(0xFF999999.toInt())
                setPadding(0, 64, 0, 0)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
            container.addView(empty)
        }
    }

    private fun showDeleteDialog(feedbackId: String) {
        val dialog = android.app.AlertDialog.Builder(this, com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog_MinWidth)
            .create()

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_feedback, null)
        dialog.setView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        view.findViewById<View>(R.id.btn_yes_delete).setOnClickListener {
            FeedbackStore.deleteFeedback(this, feedbackId)
            dialog.dismiss()
            Toast.makeText(this, "Feedback deleted", Toast.LENGTH_SHORT).show()
            loadFeedbacks()
        }
        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}
