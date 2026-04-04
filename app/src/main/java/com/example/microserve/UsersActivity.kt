package com.example.microserve

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.microserve.databinding.ActivityUsersBinding

/**
 * Admin Users Management screen with tab filtering.
 * Shows All users, Providers, Requesters, Active, Inactive.
 */
class UsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersBinding
    private lateinit var userAdapter: UserAdapter
    private var currentFilter = FILTER_ALL

    companion object {
        private const val FILTER_ALL = 0
        private const val FILTER_PROVIDERS = 1
        private const val FILTER_REQUESTERS = 2
        private const val FILTER_ACTIVE = 3
        private const val FILTER_INACTIVE = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupBackButton()
        initializeSampleDataIfNeeded()
        setupTabButtons()
        setupRecyclerView()
        setupBottomNavigation()
        loadAllUsers()
    }

    override fun onResume() {
        super.onResume()
        refreshCurrentFilter()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.usersRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.usersHeaderFrame.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun initializeSampleDataIfNeeded() {
        val allUsers = UserStore.getAllUsers(this)
        if (allUsers.isEmpty()) {
            // Add sample users for demo
            UserStore.addUser(this, "Kamal Gunarathne", "kamal@example.com", "+94 70 111 2222", UserStore.TYPE_PROVIDER)
            UserStore.addUser(this, "Nimali Peris", "nimali@example.com", "+94 71 333 4444", UserStore.TYPE_REQUESTER)
            UserStore.addUser(this, "Thakshila Jayaweera", "thakshila@example.com", "+94 72 555 6666", UserStore.TYPE_REQUESTER)
            UserStore.addUser(this, "Sampath Dahanayake", "sampath@example.com", "+94 73 777 8888", UserStore.TYPE_PROVIDER)
            UserStore.addUser(this, "Sarah Silva", "sarah@example.com", "+94 74 999 0000", UserStore.TYPE_REQUESTER)
        }
    }

    private fun setupTabButtons() {
        binding.tabAll.setOnClickListener { setFilter(FILTER_ALL) }
        binding.tabProviders.setOnClickListener { setFilter(FILTER_PROVIDERS) }
        binding.tabRequesters.setOnClickListener { setFilter(FILTER_REQUESTERS) }
        binding.tabActive.setOnClickListener { setFilter(FILTER_ACTIVE) }
        binding.tabInactive.setOnClickListener { setFilter(FILTER_INACTIVE) }
    }

    private fun setFilter(filter: Int) {
        currentFilter = filter
        updateTabStyles()
        when (filter) {
            FILTER_ALL -> loadAllUsers()
            FILTER_PROVIDERS -> loadProviders()
            FILTER_REQUESTERS -> loadRequesters()
            FILTER_ACTIVE -> loadActiveUsers()
            FILTER_INACTIVE -> loadInactiveUsers()
        }
    }

    private fun updateTabStyles() {
        val activeColor = resources.getColor(android.R.color.white, null)
        val inactiveColor = resources.getColor(R.color.admin_text_primary, null)
        val activeBg = R.drawable.tab_active_bg
        val inactiveBg = R.drawable.tab_inactive_bg

        binding.tabAll.apply {
            setBackgroundResource(if (currentFilter == FILTER_ALL) activeBg else inactiveBg)
            setTextColor(if (currentFilter == FILTER_ALL) activeColor else inactiveColor)
        }
        binding.tabProviders.apply {
            setBackgroundResource(if (currentFilter == FILTER_PROVIDERS) activeBg else inactiveBg)
            setTextColor(if (currentFilter == FILTER_PROVIDERS) activeColor else inactiveColor)
        }
        binding.tabRequesters.apply {
            setBackgroundResource(if (currentFilter == FILTER_REQUESTERS) activeBg else inactiveBg)
            setTextColor(if (currentFilter == FILTER_REQUESTERS) activeColor else inactiveColor)
        }
        binding.tabActive.apply {
            setBackgroundResource(if (currentFilter == FILTER_ACTIVE) activeBg else inactiveBg)
            setTextColor(if (currentFilter == FILTER_ACTIVE) activeColor else inactiveColor)
        }
        binding.tabInactive.apply {
            setBackgroundResource(if (currentFilter == FILTER_INACTIVE) activeBg else inactiveBg)
            setTextColor(if (currentFilter == FILTER_INACTIVE) activeColor else inactiveColor)
        }
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(emptyList()) { user ->
            val intent = Intent(this, UserDetailsActivity::class.java).apply {
                putExtra("USER_ID", user.id)
                putExtra("USER_NAME", user.name)
                putExtra("USER_EMAIL", user.email)
                putExtra("USER_PHONE", user.phone)
                putExtra("USER_TYPE", user.type)
                putExtra("USER_STATUS", user.status)
            }
            startActivity(intent)
        }

        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = userAdapter
    }

    private fun loadAllUsers() {
        val users = UserStore.getAllUsers(this)
        updateList(users, "All Users")
    }

    private fun loadProviders() {
        val users = UserStore.getProviders(this)
        updateList(users, "Providers")
    }

    private fun loadRequesters() {
        val users = UserStore.getRequesters(this)
        updateList(users, "Requesters")
    }

    private fun loadActiveUsers() {
        val users = UserStore.getActiveUsers(this)
        updateList(users, "Active Users")
    }

    private fun loadInactiveUsers() {
        val users = UserStore.getInactiveUsers(this)
        updateList(users, "Inactive/Banned Users")
    }

    private fun updateList(users: List<UserStore.User>, label: String) {
        if (users.isEmpty()) {
            binding.rvUsers.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
            binding.emptyStateText.text = "No $label found"
        } else {
            binding.rvUsers.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
        userAdapter.updateItems(users)
    }

    private fun refreshCurrentFilter() {
        setFilter(currentFilter)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Homepage::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, AdminProfileActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_profile
    }

    // ── User Adapter ────────────────────────────────────────────────────
    inner class UserAdapter(
        private var items: List<UserStore.User>,
        private val onUserClick: (UserStore.User) -> Unit
    ) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: CardView = view.findViewById(R.id.userCard)
            val tvName: TextView = view.findViewById(R.id.tvUserName)
            val tvEmail: TextView = view.findViewById(R.id.tvUserEmail)
            val tvStatus: TextView = view.findViewById(R.id.tvUserStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.tvEmail.text = item.email
            holder.tvStatus.text = item.status

            // Set status badge color
            val statusBg = when (item.status) {
                UserStore.STATUS_ACTIVE -> R.drawable.active_tag_bg
                UserStore.STATUS_BANNED -> R.drawable.delete_user_button_bg
                else -> R.drawable.tab_inactive_bg
            }
            holder.tvStatus.setBackgroundResource(statusBg)

            holder.card.setOnClickListener { onUserClick(item) }
        }

        override fun getItemCount() = items.size

        fun updateItems(newItems: List<UserStore.User>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}
