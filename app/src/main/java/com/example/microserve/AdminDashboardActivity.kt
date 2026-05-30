package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityAdminDashboardBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val listeners = mutableListOf<ListenerRegistration>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupQuickActions()
        AdminBottomNavHelper.setup(this, AdminBottomNavHelper.TAB_HOME)
        AdminDashboardSeeder.seedIfNeeded(this)
        bindStatsFromFirestore()
    }

    override fun onResume() {
        super.onResume()
        // Listeners are already active; no need to re-bind
    }

    override fun onDestroy() {
        super.onDestroy()
        listeners.forEach { it.remove() }
        listeners.clear()
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

    private fun bindStatsFromFirestore() {
        val firestore = FirebaseFirestore.getInstance()

        // ── Pending requests count (service_requests where status == "open") ──
        listeners += firestore.collection(ServiceRequest.COLLECTION)
            .whereEqualTo(ServiceRequest.FIELD_STATUS, ServiceRequestStatus.OPEN)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("AdminDashboard", "Request listener failed", error)
                    return@addSnapshotListener
                }
                val count = snapshot?.size() ?: 0
                binding.requestsCount.text = count.toString()
            }

        // ── Completed transactions count ──
        listeners += firestore.collection(ServiceTransaction.COLLECTION)
            .whereEqualTo(ServiceTransaction.FIELD_STATUS, ServiceTransactionStatus.SUCCESS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("AdminDashboard", "Transaction listener failed", error)
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents.orEmpty()
                val count = docs.size
                val totalRevenue = docs.sumOf { doc ->
                    (doc.getLong(ServiceTransaction.FIELD_AMOUNT) ?: 0L).toInt()
                }
                binding.completedCount.text = count.toString()
                binding.revenueCount.text = ServiceTransaction.formatAmount(totalRevenue)
            }

        // ── Feedbacks count ──
        listeners += firestore.collection("feedbacks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("AdminDashboard", "Feedback listener failed", error)
                    return@addSnapshotListener
                }
                val count = snapshot?.size() ?: 0
                binding.feedbacksCount.text = count.toString()
            }
    }
}
