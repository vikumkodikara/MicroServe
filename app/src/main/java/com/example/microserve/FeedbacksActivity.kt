package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.microserve.databinding.ActivityFeedbacksBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeedbacksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbacksBinding
    private lateinit var feedbackAdapter: FeedbackAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbacksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupBackButton()
        setupRecycler()
        setupBottomNavigation()
        initializeSampleFeedbacksIfNeeded()
        loadFeedbacks()
    }

    override fun onResume() {
        super.onResume()
        loadFeedbacks()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.feedbacksRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.feedbacksHeaderFrame.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecycler() {
        feedbackAdapter = FeedbackAdapter(emptyList()) { feedback ->
            val deleted = FeedbackStore.deleteFeedback(this, feedback.id)
            if (deleted) {
                Toast.makeText(this, "Feedback deleted", Toast.LENGTH_SHORT).show()
                loadFeedbacks()
            }
        }

        binding.rvFeedbacks.layoutManager = LinearLayoutManager(this)
        binding.rvFeedbacks.adapter = feedbackAdapter
    }

    private fun loadFeedbacks() {
        val items = FeedbackStore.getAllFeedbacks(this)
        if (items.isEmpty()) {
            binding.rvFeedbacks.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.rvFeedbacks.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
        feedbackAdapter.updateItems(items)
    }

    private fun initializeSampleFeedbacksIfNeeded() {
        if (FeedbackStore.getAllFeedbacks(this).isNotEmpty()) return

        FeedbackStore.addFeedback(
            context = this,
            userId = "USR-001",
            userName = "Pasan Priyasanka",
            message = "Very Good Service! Great Experience and fast Response",
            rating = 5,
            createdAt = 1766652900000L
        )

        FeedbackStore.addFeedback(
            context = this,
            userId = "USR-002",
            userName = "Hiranya Pahasara",
            message = "Service Was Okay but took a Little longer",
            rating = 4,
            createdAt = 1767606300000L
        )
    }

    private fun setupBottomNavigation() {
        val homeTab = findViewById<android.widget.LinearLayout>(R.id.navTabHome)
        val profileTab = findViewById<android.widget.LinearLayout>(R.id.navTabProfile)
        val settingsTab = findViewById<android.widget.LinearLayout>(R.id.navTabSettings)
        val bubbleIcon = findViewById<android.widget.ImageView>(R.id.navBubbleIcon)

        homeTab.setOnClickListener {
            startActivity(Intent(this, Homepage::class.java))
            finish()
        }

        profileTab.setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
            finish()
        }

        settingsTab.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }

        bubbleIcon.setImageResource(R.drawable.ic_nav_home)
    }

    inner class FeedbackAdapter(
        private var items: List<FeedbackStore.Feedback>,
        private val onDeleteClick: (FeedbackStore.Feedback) -> Unit
    ) : RecyclerView.Adapter<FeedbackAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: CardView = view.findViewById(R.id.feedbackCard)
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvMessage: TextView = view.findViewById(R.id.tvMessage)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val btnDelete: Button = view.findViewById(R.id.btnDelete)
            val stars: List<ImageView> = listOf(
                view.findViewById(R.id.star1),
                view.findViewById(R.id.star2),
                view.findViewById(R.id.star3),
                view.findViewById(R.id.star4),
                view.findViewById(R.id.star5)
            )
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feedback, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.userName
            holder.tvMessage.text = item.message
            holder.tvDate.text = formatDate(item.createdAt)

            val filledColor = resources.getColor(R.color.admin_stat_orange, null)
            val emptyColor = resources.getColor(R.color.admin_purple_light, null)
            holder.stars.forEachIndexed { index, imageView ->
                if (index < item.rating) {
                    imageView.setColorFilter(filledColor)
                    imageView.alpha = 1f
                } else {
                    imageView.setColorFilter(emptyColor)
                    imageView.alpha = 0.6f
                }
            }

            holder.btnDelete.setOnClickListener { onDeleteClick(item) }
        }

        override fun getItemCount() = items.size

        fun updateItems(newItems: List<FeedbackStore.Feedback>) {
            items = newItems
            notifyDataSetChanged()
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd MMM yyyy - hh.mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}
