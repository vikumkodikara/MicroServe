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

    private lateinit var requesterAdapter: RequesterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupBackButton()
        setupRecyclerView()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        loadPendingRequests()
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
        requesterAdapter = RequesterAdapter(emptyList()) { item ->
            val intent = Intent(this, RequesterDetailsActivity::class.java).apply {
                putExtra("REQUEST_ID", item.id)
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

        binding.rvRequesters.layoutManager = LinearLayoutManager(this)
        binding.rvRequesters.adapter = requesterAdapter
        loadPendingRequests()
    }

    private fun loadPendingRequests() {
        val pendingRequesters = RequestStore.getPendingRequests(this)

        if (pendingRequesters.isEmpty()) {
            binding.rvRequesters.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.rvRequesters.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }

        requesterAdapter.updateItems(pendingRequesters)
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
        private var items: List<RequestStore.UserRequest>,
        private val onItemClick: (RequestStore.UserRequest) -> Unit
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

        fun updateItems(newItems: List<RequestStore.UserRequest>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}
