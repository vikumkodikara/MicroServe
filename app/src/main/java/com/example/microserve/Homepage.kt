package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityAdminDashboardBinding

class Homepage : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupQuickActions()
        AdminBottomNavHelper.setup(this, AdminBottomNavHelper.TAB_HOME)

        // Seed dummy data and bind stats
        seedDummyTransactionsIfNeeded()
        bindDashboardStats()
    }

    override fun onResume() {
        super.onResume()
        bindDashboardStats()
    }

    /**
     * Seeds 5 dummy transactions (3 success + 2 pending) so the dashboard
     * and Transactions page show working data out of the box.
     */
    private fun seedDummyTransactionsIfNeeded() {
        val existing = TransactionStore.getAllTransactions(this)
        if (existing.isNotEmpty()) return

        // Create provider users first
        val providerA = UserStore.getUserByName(this, "Kamal Gunarathne")
            ?: UserStore.addUser(this, "Kamal Gunarathne", "kamal@example.com", "+94 70 111 2222", type = UserStore.TYPE_PROVIDER)
        val providerB = UserStore.getUserByName(this, "Sampath Dahanayake")
            ?: UserStore.addUser(this, "Sampath Dahanayake", "sampath@example.com", "+94 73 777 8888", type = UserStore.TYPE_PROVIDER)
        val providerC = UserStore.getUserByName(this, "Nimal Perera")
            ?: UserStore.addUser(this, "Nimal Perera", "nimal@example.com", "+94 71 333 4444", type = UserStore.TYPE_PROVIDER)
        val providerD = UserStore.getUserByName(this, "Ruwan Silva")
            ?: UserStore.addUser(this, "Ruwan Silva", "ruwan@example.com", "+94 76 555 6666", type = UserStore.TYPE_PROVIDER)
        val providerE = UserStore.getUserByName(this, "Amara Jayasinghe")
            ?: UserStore.addUser(this, "Amara Jayasinghe", "amara@example.com", "+94 77 999 0000", type = UserStore.TYPE_PROVIDER)

        // 3 SUCCESS transactions (completed)
        val tx1 = TransactionStore.addPendingTransaction(
            context = this,
            providerUserId = providerA.id,
            providerName = providerA.name,
            amount = 2800.0,
            title = "Plumbing Repair \u2014 Kitchen Sink"
        )
        TransactionStore.markTransactionSuccessAndCreditUser(this, tx1.id)

        val tx2 = TransactionStore.addPendingTransaction(
            context = this,
            providerUserId = providerB.id,
            providerName = providerB.name,
            amount = 3500.0,
            title = "Electrical Wiring \u2014 Living Room"
        )
        TransactionStore.markTransactionSuccessAndCreditUser(this, tx2.id)

        val tx3 = TransactionStore.addPendingTransaction(
            context = this,
            providerUserId = providerC.id,
            providerName = providerC.name,
            amount = 4500.0,
            title = "House Painting \u2014 Exterior Walls"
        )
        TransactionStore.markTransactionSuccessAndCreditUser(this, tx3.id)

        // 2 PENDING transactions
        TransactionStore.addPendingTransaction(
            context = this,
            providerUserId = providerD.id,
            providerName = providerD.name,
            amount = 3200.0,
            title = "Garden Cleanup \u2014 Front Yard"
        )

        TransactionStore.addPendingTransaction(
            context = this,
            providerUserId = providerE.id,
            providerName = providerE.name,
            amount = 2500.0,
            title = "Cleaning Service \u2014 2nd Floor"
        )
    }

    /**
     * Binds the Completed count and Revenue values to the dashboard stat cards.
     */
    private fun bindDashboardStats() {
        val completedCount = TransactionStore.getSuccessCount(this)
        val totalRevenue = TransactionStore.getTotalSuccessAmount(this)
        val pendingCount = TransactionStore.getPendingTransactions(this).size

        binding.completedCount.text = completedCount.toString()
        binding.revenueCount.text = TransactionStore.formatAmount(totalRevenue)
        binding.requestsCount.text = pendingCount.toString()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            binding.headerFrame.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun setupQuickActions() {
        binding.quickRequestsBtn.setOnClickListener {
            startActivity(Intent(this, RequestersActivity::class.java))
        }
        binding.quickServicesBtn.setOnClickListener {
            startActivity(Intent(this, ServicesActivity::class.java))
        }
        binding.quickTransactionsBtn.setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java))
        }
        binding.quickFeedbacksBtn.setOnClickListener {
            startActivity(Intent(this, FeedbacksActivity::class.java))
        }
        binding.quickUsersBtn.setOnClickListener {
            startActivity(Intent(this, UsersActivity::class.java))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
