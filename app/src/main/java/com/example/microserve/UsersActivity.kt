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
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.microserve.databinding.ActivityUsersBinding
import com.google.firebase.firestore.ListenerRegistration

/**
 * Admin Users Management screen with tab filtering.
 * Loads registered users from Firestore in real time.
 */
class UsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersBinding
    private lateinit var userAdapter: UserAdapter
    private var currentFilter = FILTER_ALL
    private var usersListener: ListenerRegistration? = null
    private var firestoreUsers: List<UserStore.User> = emptyList()

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
        setupTabButtons()
        setupRecyclerView()
        setupBottomNavigation()
    }

    override fun onStart() {
        super.onStart()
        usersListener?.remove()
        usersListener = UserRepository.listenAllUsers(
            onUpdate = { profiles ->
                firestoreUsers = profiles
                    .map { it.toAdminListUser() }
                    .filterNot { it.type.equals(UserStore.TYPE_ADMIN, ignoreCase = true) }
                refreshCurrentFilter()
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                loadLocalFallback()
            }
        )
    }

    override fun onStop() {
        usersListener?.remove()
        usersListener = null
        super.onStop()
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

    private fun loadLocalFallback() {
        firestoreUsers = UserStore.getAllUsers(this)
            .filterNot { it.type.equals(UserStore.TYPE_ADMIN, ignoreCase = true) }
        refreshCurrentFilter()
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
        refreshCurrentFilter()
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

    private fun refreshCurrentFilter() {
        val users = when (currentFilter) {
            FILTER_ALL -> firestoreUsers
            FILTER_PROVIDERS -> firestoreUsers.filter {
                it.type.equals(UserStore.TYPE_PROVIDER, ignoreCase = true)
            }
            FILTER_REQUESTERS -> firestoreUsers.filter {
                it.type.equals(UserStore.TYPE_REQUESTER, ignoreCase = true)
            }
            FILTER_ACTIVE -> firestoreUsers.filter {
                it.status.equals(UserStore.STATUS_ACTIVE, ignoreCase = true)
            }
            FILTER_INACTIVE -> firestoreUsers.filter {
                it.status.equals(UserStore.STATUS_BANNED, ignoreCase = true) ||
                    it.status.equals(UserStore.STATUS_INACTIVE, ignoreCase = true)
            }
            else -> firestoreUsers
        }

        val label = when (currentFilter) {
            FILTER_ALL -> "All Users"
            FILTER_PROVIDERS -> "Providers"
            FILTER_REQUESTERS -> "Requesters"
            FILTER_ACTIVE -> "Active Users"
            FILTER_INACTIVE -> "Inactive/Banned Users"
            else -> "Users"
        }
        updateList(users, label)
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

    private fun setupBottomNavigation() {
        val homeTab = findViewById<android.widget.LinearLayout>(R.id.navTabHome)
        val profileTab = findViewById<android.widget.LinearLayout>(R.id.navTabProfile)
        val settingsTab = findViewById<android.widget.LinearLayout>(R.id.navTabSettings)
        val bubbleIcon = findViewById<android.widget.ImageView>(R.id.navBubbleIcon)

        homeTab.setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
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
        bubbleIcon.setImageResource(R.drawable.ic_nav_profile)
    }

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
