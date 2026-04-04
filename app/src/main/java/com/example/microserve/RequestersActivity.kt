package com.example.microserve

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.microserve.databinding.ActivityRequestersBinding

/**
 * Displays all pending service requests submitted by users.
 * Tapping a card opens RequesterDetailsActivity for full details.
 */
class RequestersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRequestersBinding

    // ── Sample data model ──────────────────────────────────────────────────────
    data class RequesterItem(
        val id: Int,
        val title: String,
        val category: String,
        val requesterName: String,
        val contact: String,
        val location: String,
        val description: String,
        val status: String = "Pending"
    )

    // ── Sample dataset (replace with real DB / API data) ──────────────────────
    private val sampleRequesters = listOf(
        RequesterItem(
            id = 1,
            title = "Need House Cleaning",
            category = "Cleaning",
            requesterName = "Amila Darshana",
            contact = "+94 77 123 4567",
            location = "Malabe",
            description = "Looking for Home cleaning Service for our Malabe home. There are 4 Rooms and One washroom readmore...."
        ),
        RequesterItem(
            id = 2,
            title = "Need Welding Service",
            category = "Welding",
            requesterName = "Nimal Perera",
            contact = "+94 71 234 5678",
            location = "Wattala / North Wattala / Pappitiw",
            description = "Need professional welding service to fix a metal gate and a small fence around the garden."
        ),
        RequesterItem(
            id = 3,
            title = "Graphic Logo Design Needed",
            category = "Graphic Design",
            requesterName = "Savindi Rathnayaka",
            contact = "+94 76 345 6789",
            location = "Colombo 3",
            description = "Looking for a creative designer to make a modern minimalist logo for my new clothing brand."
        ),
        RequesterItem(
            id = 4,
            title = "Plumbing - Pipe Fix",
            category = "Plumbing",
            requesterName = "Kasun Fernando",
            contact = "+94 70 456 7890",
            location = "Nugegoda",
            description = "Urgent pipe leak fix needed in bathroom. Water is dripping continuously from the ceiling pipe."
        ),
        RequesterItem(
            id = 5,
            title = "Electrical Wiring Help",
            category = "Electrical",
            requesterName = "Thilaka Jayasinghe",
            contact = "+94 75 567 8901",
            location = "Kadawatha",
            description = "Need a licensed electrician to install additional power outlets in the home office room.",
            status = "Completed"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupBackButton()
        setupRecyclerView()
        setupBottomNavigation()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.requestersRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.requestersHeaderFrame.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val pendingRequesters = sampleRequesters.filter { it.status.equals("Pending", ignoreCase = true) }

        if (pendingRequesters.isEmpty()) {
            binding.rvRequesters.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.rvRequesters.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            binding.rvRequesters.layoutManager = LinearLayoutManager(this)
            binding.rvRequesters.adapter = RequesterAdapter(pendingRequesters) { item ->
                // Navigate to details screen
                val intent = Intent(this, RequesterDetailsActivity::class.java).apply {
                    putExtra("REQUESTER_ID", item.id)
                    putExtra("TITLE", item.title)
                    putExtra("CATEGORY", item.category)
                    putExtra("REQUESTER_NAME", item.requesterName)
                    putExtra("CONTACT", item.contact)
                    putExtra("LOCATION", item.location)
                    putExtra("DESCRIPTION", item.description)
                    putExtra("STATUS", item.status)
                }
                startActivity(intent)
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Homepage::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> true
                R.id.nav_settings -> true
                else -> false
            }
        }
    }

    // ── RecyclerView Adapter ───────────────────────────────────────────────────
    inner class RequesterAdapter(
        private val items: List<RequesterItem>,
        private val onItemClick: (RequesterItem) -> Unit
    ) : RecyclerView.Adapter<RequesterAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: CardView = view.findViewById(R.id.requesterCard)
            val tvTitle: TextView = view.findViewById(R.id.tvRequesterTitle)
            val tvCategory: TextView = view.findViewById(R.id.tvRequesterCategory)
            val tvName: TextView = view.findViewById(R.id.tvRequesterName)
            val tvStatus: TextView = view.findViewById(R.id.tvRequesterStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_requester, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.title
            holder.tvCategory.text = item.category
            holder.tvName.text = item.requesterName + " · " + item.location
            holder.tvStatus.text = item.status
            holder.card.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
