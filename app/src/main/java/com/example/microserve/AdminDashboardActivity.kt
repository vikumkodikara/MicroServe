package com.example.microserve

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityAdminDashboardBinding

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupQuickActions()
        AdminBottomNavHelper.setup(this, AdminBottomNavHelper.TAB_HOME)
        bindStats()
    }

    override fun onResume() {
        super.onResume()
        bindStats()
    }

    private fun setupWindowInsets() {
        binding.main.applyHorizontalSystemBarInsets()
        binding.headerFrame.applyStatusBarTopInset()
        applyNavBarSpacer(R.id.adminNavSystemBarSpacer)
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

    private fun bindStats() {
        val pendingRequests = RequestStore.getPendingRequests(this).size
        val completedTransactions = TransactionStore.getSuccessCount(this)
        val feedbackCount = FeedbackStore.getFeedbackCount(this)
        val revenue = TransactionStore.getTotalSuccessAmount(this)

        binding.requestsCount.text = pendingRequests.toString()
        binding.completedCount.text = completedTransactions.toString()
        binding.feedbacksCount.text = feedbackCount.toString()
        binding.revenueCount.text = TransactionStore.formatAmount(revenue)
    }
}
