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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.example.microserve.databinding.ActivityServicesBinding

/**
 * Displays current and pending services provided by service providers.
 * Tabs to switch between Active and Pending services.
 */
class ServicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServicesBinding
    private lateinit var serviceAdapter: ServiceAdapter
    private var currentTab = TAB_ACTIVE

    companion object {
        private const val TAB_ACTIVE = 0
        private const val TAB_PENDING = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupBackButton()
        setupTabButtons()
        setupRecyclerView()
        setupBottomNavigation()
        initializeSampleDataIfNeeded()
        loadActiveServices()
    }

    override fun onResume() {
        super.onResume()
        when (currentTab) {
            TAB_ACTIVE -> loadActiveServices()
            TAB_PENDING -> loadPendingServices()
        }
    }

    private fun initializeSampleDataIfNeeded() {
        val allServices = ServiceStore.getAllServices(this)
        if (allServices.isEmpty()) {
            // Add sample services for demo
            ServiceStore.addService(this, "Plumbing", "Kasun Perera", "+94 70 123 4567", "Malabe, Western Province")
            ServiceStore.addService(this, "Electrical", "Silva Weerahinga", "+94 71 234 5678", "Colombo 3")
            ServiceStore.addService(this, "Cleaning", "Amila Darshana", "+94 77 345 6789", "Kandy, Central Province")
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.servicesRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.servicesHeaderFrame.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupTabButtons() {
        binding.tabCurrentServices.setOnClickListener {
            currentTab = TAB_ACTIVE
            updateTabStyles()
            loadActiveServices()
        }

        binding.tabPendingServices.setOnClickListener {
            currentTab = TAB_PENDING
            updateTabStyles()
            loadPendingServices()
        }

        updateTabStyles()
    }

    private fun updateTabStyles() {
        if (currentTab == TAB_ACTIVE) {
            binding.tabCurrentServices.setBackgroundResource(R.drawable.tab_active_bg)
            binding.tabCurrentServices.setTextColor(resources.getColor(R.color.white, null))
            binding.tabPendingServices.setBackgroundResource(R.drawable.tab_inactive_bg)
            binding.tabPendingServices.setTextColor(resources.getColor(R.color.admin_text_primary, null))
        } else {
            binding.tabCurrentServices.setBackgroundResource(R.drawable.tab_inactive_bg)
            binding.tabCurrentServices.setTextColor(resources.getColor(R.color.admin_text_primary, null))
            binding.tabPendingServices.setBackgroundResource(R.drawable.tab_active_bg)
            binding.tabPendingServices.setTextColor(resources.getColor(R.color.white, null))
        }
    }

    private fun setupRecyclerView() {
        serviceAdapter = ServiceAdapter(
            items = emptyList(),
            onAction = { service, action ->
                when (action) {
                    "block" -> handleBlockService(service)
                    "delete" -> handleDeleteService(service)
                    "confirm" -> handleConfirmService(service)
                }
            }
        )

        binding.rvServices.layoutManager = LinearLayoutManager(this)
        binding.rvServices.adapter = serviceAdapter
    }

    private fun loadActiveServices() {
        val services = ServiceStore.getActiveServices(this)

        if (services.isEmpty()) {
            binding.rvServices.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
            binding.emptyStateText.text = "No Active Services"
        } else {
            binding.rvServices.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }

        serviceAdapter.updateItems(services, TAB_ACTIVE)
    }

    private fun loadPendingServices() {
        val services = ServiceStore.getPendingServices(this)

        if (services.isEmpty()) {
            binding.rvServices.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
            binding.emptyStateText.text = "No Pending Services"
        } else {
            binding.rvServices.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }

        serviceAdapter.updateItems(services, TAB_PENDING)
    }

    private fun handleBlockService(service: ServiceStore.Service) {
        ServiceStore.updateServiceStatus(this, service.id, ServiceStore.STATUS_PENDING)
        Toast.makeText(this, "${service.category} moved to pending approval", Toast.LENGTH_SHORT)
            .show()
        if (currentTab == TAB_ACTIVE) loadActiveServices() else loadPendingServices()
    }

    private fun handleDeleteService(service: ServiceStore.Service) {
        ServiceStore.deleteService(this, service.id)
        Toast.makeText(this, "Service deleted", Toast.LENGTH_SHORT).show()
        if (currentTab == TAB_ACTIVE) loadActiveServices() else loadPendingServices()
    }

    private fun handleConfirmService(service: ServiceStore.Service) {
        val provider = UserStore.getUserByName(this, service.providerName)
            ?: UserStore.addUser(
                context = this,
                name = service.providerName,
                email = "provider_${System.currentTimeMillis()}@microserve.local",
                phone = service.contact,
                type = UserStore.TYPE_PROVIDER
            )

        TransactionStore.addPendingTransaction(
            context = this,
            providerUserId = provider.id,
            providerName = provider.name,
            amount = TransactionStore.estimateAmountForService(service.category)
        )

        ServiceStore.updateServiceStatus(this, service.id, ServiceStore.STATUS_COMPLETED)
        Toast.makeText(this, "Service moved to transactions (Pending)", Toast.LENGTH_SHORT).show()
        loadPendingServices()
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
                    startActivity(Intent(this, UsersActivity::class.java))
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
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    // ── Service Adapter ────────────────────────────────────────────────────
    inner class ServiceAdapter(
        private var items: List<ServiceStore.Service>,
        private val onAction: (ServiceStore.Service, String) -> Unit,
        private var tabType: Int = TAB_ACTIVE
    ) : RecyclerView.Adapter<ServiceAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: CardView = view.findViewById(R.id.serviceCard)
            val tvTitle: TextView = view.findViewById(R.id.tvServiceTitle)
            val tvCategory: TextView = view.findViewById(R.id.tvServiceCategory)
            val tvLocation: TextView = view.findViewById(R.id.tvServiceLocation)
            val tvStatus: TextView = view.findViewById(R.id.tvServiceStatus)
            val btnAction1: Button = view.findViewById(R.id.btnServiceAction1)
            val btnAction2: Button = view.findViewById(R.id.btnServiceAction2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_service_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.title
            holder.tvCategory.text = item.category
            holder.tvLocation.text = "${item.location}"
            holder.tvStatus.text = item.status

            // Active tab buttons: Block | Delete
            if (tabType == TAB_ACTIVE) {
                holder.btnAction1.text = "Block"
                holder.btnAction1.setBackgroundResource(R.drawable.block_button_bg)
                holder.btnAction1.setOnClickListener { onAction(item, "block") }

                holder.btnAction2.text = "Delete"
                holder.btnAction2.setBackgroundResource(R.drawable.delete_user_button_bg)
                holder.btnAction2.setOnClickListener { onAction(item, "delete") }
            }
            // Pending tab buttons: Confirm | Delete
            else {
                holder.btnAction1.text = "Confirm"
                holder.btnAction1.setBackgroundResource(R.drawable.primary_button_bg)
                holder.btnAction1.setOnClickListener { onAction(item, "confirm") }

                holder.btnAction2.text = "Delete"
                holder.btnAction2.setBackgroundResource(R.drawable.delete_user_button_bg)
                holder.btnAction2.setOnClickListener { onAction(item, "delete") }
            }
        }

        override fun getItemCount() = items.size

        fun updateItems(newItems: List<ServiceStore.Service>, tab: Int) {
            items = newItems
            tabType = tab
            notifyDataSetChanged()
        }
    }
}
