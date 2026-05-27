package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.microserve.databinding.ActivityTransactionsBinding
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionsBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private var currentTab = TAB_PENDING
    private var pendingListener: ListenerRegistration? = null
    private var successListener: ListenerRegistration? = null
    private var pendingItems: List<ServiceTransaction> = emptyList()
    private var successItems: List<ServiceTransaction> = emptyList()

    companion object {
        private const val TAB_PENDING = 0
        private const val TAB_SUCCESS = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupBackButton()
        setupTabs()
        setupRecycler()
        setupBottomNavigation()
    }

    override fun onStart() {
        super.onStart()
        pendingListener?.remove()
        successListener?.remove()

        pendingListener = TransactionRepository.listenPending(
            onUpdate = { items ->
                pendingItems = items
                if (currentTab == TAB_PENDING) updateList(items)
            },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )

        successListener = TransactionRepository.listenSuccess(
            onUpdate = { items ->
                successItems = items
                if (currentTab == TAB_SUCCESS) updateList(items)
            },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    override fun onStop() {
        pendingListener?.remove()
        successListener?.remove()
        pendingListener = null
        successListener = null
        super.onStop()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.transactionsRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.transactionsHeaderFrame.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupTabs() {
        binding.tabPending.setOnClickListener {
            currentTab = TAB_PENDING
            updateTabStyles()
            updateList(pendingItems)
        }

        binding.tabSuccess.setOnClickListener {
            currentTab = TAB_SUCCESS
            updateTabStyles()
            updateList(successItems)
        }

        updateTabStyles()
    }

    private fun setupRecycler() {
        transactionAdapter = TransactionAdapter(emptyList()) { item ->
            startActivity(
                Intent(this, TransactionDetailsActivity::class.java)
                    .putExtra(TransactionDetailsActivity.EXTRA_TRANSACTION_ID, item.id)
            )
        }
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = transactionAdapter
    }

    private fun updateList(items: List<ServiceTransaction>) {
        if (items.isEmpty()) {
            binding.rvTransactions.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.rvTransactions.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
        transactionAdapter.updateItems(items)
    }

    private fun updateTabStyles() {
        if (currentTab == TAB_PENDING) {
            binding.tabPending.setBackgroundResource(R.drawable.tab_active_bg)
            binding.tabPending.setTextColor(resources.getColor(R.color.white, null))
            binding.tabSuccess.setBackgroundResource(R.drawable.tab_inactive_bg)
            binding.tabSuccess.setTextColor(resources.getColor(R.color.admin_text_primary, null))
        } else {
            binding.tabPending.setBackgroundResource(R.drawable.tab_inactive_bg)
            binding.tabPending.setTextColor(resources.getColor(R.color.admin_text_primary, null))
            binding.tabSuccess.setBackgroundResource(R.drawable.tab_active_bg)
            binding.tabSuccess.setTextColor(resources.getColor(R.color.white, null))
        }
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
        bubbleIcon.setImageResource(R.drawable.ic_nav_home)
    }

    inner class TransactionAdapter(
        private var items: List<ServiceTransaction>,
        private val onClick: (ServiceTransaction) -> Unit
    ) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: CardView = view.findViewById(R.id.transactionCard)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvAmount: TextView = view.findViewById(R.id.tvAmount)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvTxnId: TextView = view.findViewById(R.id.tvTransactionId)
            val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.requestTitle.ifBlank { item.providerName }
            holder.tvAmount.text = ServiceTransaction.formatAmount(item.amount)
            holder.tvTxnId.text = "Txn ID : ${item.transactionCode}"
            holder.tvDate.text = formatDate(item.adminApprovedAt ?: item.requesterConfirmedAt ?: item.paidAt ?: item.createdAt)
            holder.tvStatus.text = item.status.replace('_', ' ')

            if (item.status == ServiceTransactionStatus.SUCCESS) {
                holder.tvStatus.setBackgroundResource(R.drawable.txn_success_tag_bg)
                holder.tvStatus.setTextColor(resources.getColor(R.color.white, null))
            } else {
                holder.tvStatus.setBackgroundResource(R.drawable.txn_pending_tag_bg)
                holder.tvStatus.setTextColor(resources.getColor(android.R.color.black, null))
            }

            holder.card.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size

        fun updateItems(newItems: List<ServiceTransaction>) {
            items = newItems
            notifyDataSetChanged()
        }

        private fun formatDate(timestamp: Long?): String {
            if (timestamp == null) return "-"
            val sdf = SimpleDateFormat("dd MMM yyyy - hh.mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}
