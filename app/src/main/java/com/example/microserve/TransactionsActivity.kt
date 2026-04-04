package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.microserve.databinding.ActivityTransactionsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionsBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private var currentTab = TAB_PENDING

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
        initializeDummyTransactionsIfNeeded()
        loadPending()
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == TAB_PENDING) loadPending() else loadSuccess()
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
            loadPending()
        }

        binding.tabSuccess.setOnClickListener {
            currentTab = TAB_SUCCESS
            updateTabStyles()
            loadSuccess()
        }

        updateTabStyles()
    }

    private fun setupRecycler() {
        transactionAdapter = TransactionAdapter(emptyList()) { item ->
            val intent = Intent(this, TransactionDetailsActivity::class.java)
            intent.putExtra("TRANSACTION_ID", item.id)
            startActivity(intent)
        }
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = transactionAdapter
    }

    private fun loadPending() {
        val items = TransactionStore.getPendingTransactions(this)
        updateList(items)
    }

    private fun loadSuccess() {
        val items = TransactionStore.getSuccessTransactions(this)
        updateList(items)
    }

    private fun updateList(items: List<TransactionStore.Transaction>) {
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

    private fun initializeDummyTransactionsIfNeeded() {
        val all = TransactionStore.getAllTransactions(this)
        if (all.isNotEmpty()) return

        val providerA = UserStore.getUserByName(this, "Kamal Gunarathne")
            ?: UserStore.addUser(this, "Kamal Gunarathne", "kamal@example.com", "+94 70 111 2222", UserStore.TYPE_PROVIDER)
        val providerB = UserStore.getUserByName(this, "Sampath Dahanayake")
            ?: UserStore.addUser(this, "Sampath Dahanayake", "sampath@example.com", "+94 73 777 8888", UserStore.TYPE_PROVIDER)

        val first = TransactionStore.addPendingTransaction(
            context = this,
            providerUserId = providerA.id,
            providerName = providerA.name,
            amount = 2800.0
        )
        TransactionStore.markTransactionSuccessAndCreditUser(this, first.id)

        TransactionStore.addPendingTransaction(
            context = this,
            providerUserId = providerB.id,
            providerName = providerB.name,
            amount = 3500.0
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

    inner class TransactionAdapter(
        private var items: List<TransactionStore.Transaction>,
        private val onClick: (TransactionStore.Transaction) -> Unit
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
            holder.tvTitle.text = item.title
            holder.tvAmount.text = TransactionStore.formatAmount(item.amount)
            holder.tvTxnId.text = "Txn ID : ${item.transactionCode}"
            holder.tvDate.text = formatDate(item.completedAt ?: item.createdAt)
            holder.tvStatus.text = item.status

            if (item.status.equals(TransactionStore.STATUS_SUCCESS, ignoreCase = true)) {
                holder.tvStatus.setBackgroundResource(R.drawable.txn_success_tag_bg)
                holder.tvStatus.setTextColor(resources.getColor(R.color.white, null))
            } else {
                holder.tvStatus.setBackgroundResource(R.drawable.txn_pending_tag_bg)
                holder.tvStatus.setTextColor(resources.getColor(android.R.color.black, null))
            }

            holder.card.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size

        fun updateItems(newItems: List<TransactionStore.Transaction>) {
            items = newItems
            notifyDataSetChanged()
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd MMM yyyy - hh.mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}
