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
    private var currentMetrics = DashboardMetrics()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupClickListeners()
        setupBottomNavigation()
        refreshMetrics()
    }

    override fun onResume() {
        super.onResume()
        refreshMetrics()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupClickListeners() {
        binding.quickRequestsBtn.setOnClickListener {
            showToast("Opening service requests")
            startActivity(Intent(this, RequestersActivity::class.java))
        }

        binding.quickServicesBtn.setOnClickListener {
            showToast("Opening services management")
            startActivity(Intent(this, ServicesActivity::class.java))
        }

        binding.quickTransactionsBtn.setOnClickListener {
            showToast("Opening transaction records")
        }

        binding.quickFeedbacksBtn.setOnClickListener {
            showToast("Opening feedback management")
            startActivity(Intent(this, EditPostActivity::class.java))
        }

        binding.quickUsersBtn.setOnClickListener {
            showToast("Opening user management")
            startActivity(Intent(this, UsersActivity::class.java))
        }

        binding.statRequestsCard.setOnClickListener {
            showToast("Total service requests: ${currentMetrics.requests}")
        }

        binding.statCompletedCard.setOnClickListener {
            showToast("Completed transactions: ${currentMetrics.completed}")
        }

        binding.statFeedbacksCard.setOnClickListener {
            showToast("Total feedback submissions: ${currentMetrics.feedbacks}")
        }

        binding.statRevenueCard.setOnClickListener {
            showToast("Current revenue: Rs.${currentMetrics.revenue}")
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    showToast("Navigating to Home")
                    true
                }
                R.id.nav_profile -> {
                    showToast("Navigating to Profile")
                    true
                }
                R.id.nav_settings -> {
                    showToast("Navigating to Settings")
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun refreshMetrics() {
        val dbMetrics = fetchDashboardMetricsFromDatabase()
        currentMetrics = dbMetrics ?: DashboardMetrics()
        updateMetricsUi(currentMetrics)
    }

    private fun updateMetricsUi(metrics: DashboardMetrics) {
        binding.requestsCount.text = metrics.requests.toString()
        binding.completedCount.text = metrics.completed.toString()
        binding.feedbacksCount.text = metrics.feedbacks.toString()
        binding.revenueCount.text = "Rs.${metrics.revenue}"
    }

    private fun fetchDashboardMetricsFromDatabase(): DashboardMetrics? {
        val pendingCount = RequestStore.getPendingRequests(this).size
        val activeServicesCount = ServiceStore.getActiveServices(this).size
        return DashboardMetrics(requests = pendingCount, completed = activeServicesCount)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    data class DashboardMetrics(
        val requests: Int = 0,
        val completed: Int = 0,
        val feedbacks: Int = 0,
        val revenue: Int = 0
    )
}
