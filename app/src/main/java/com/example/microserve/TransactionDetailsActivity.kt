package com.example.microserve

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityTransactionDetailsBinding
import com.google.firebase.firestore.ListenerRegistration

class TransactionDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRANSACTION_ID = "TRANSACTION_ID"
    }

    private lateinit var binding: ActivityTransactionDetailsBinding
    private var transactionId: String? = null
    private var currentTransaction: ServiceTransaction? = null
    private var transactionListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupBackButton()
        setupBottomNavigation()
        setupTransferButton()

        transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID)
        if (transactionId.isNullOrBlank()) {
            Toast.makeText(this, R.string.transaction_not_found, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        val id = transactionId ?: return
        transactionListener?.remove()
        transactionListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection(ServiceTransaction.COLLECTION)
            .document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, error.localizedMessage, Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    Toast.makeText(this, R.string.transaction_not_found, Toast.LENGTH_SHORT).show()
                    finish()
                    return@addSnapshotListener
                }
                val item = ServiceTransaction.fromMap(snapshot.id, snapshot.data.orEmpty())
                currentTransaction = item
                bindTransaction(item)
            }
    }

    override fun onStop() {
        transactionListener?.remove()
        transactionListener = null
        super.onStop()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.transactionDetailsRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.transactionDetailsHeaderFrame.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun bindTransaction(item: ServiceTransaction) {
        binding.tvProviderId.text = item.providerCode
        binding.tvProviderName.text = item.providerName
        binding.tvAmount.text = ServiceTransaction.formatAmount(item.amount)
        binding.tvTransactionCode.text = item.transactionCode
        binding.tvStatus.text = item.status.replace('_', ' ')

        val canApprove = item.status == ServiceTransactionStatus.AWAITING_ADMIN

        if (item.status == ServiceTransactionStatus.SUCCESS) {
            binding.tvHeaderLine1.text = getString(R.string.transaction_success_title)
            binding.tvStatus.setBackgroundResource(R.drawable.txn_success_tag_bg)
            binding.tvStatus.setTextColor(resources.getColor(R.color.white, null))
            binding.btnTransfer.visibility = View.GONE
        } else {
            binding.tvHeaderLine1.text = if (canApprove) {
                getString(R.string.approve_payout_title)
            } else {
                getString(R.string.transfer_money_title)
            }
            binding.tvStatus.setBackgroundResource(R.drawable.txn_pending_tag_bg)
            binding.tvStatus.setTextColor(resources.getColor(android.R.color.black, null))
            binding.btnTransfer.visibility = if (canApprove) View.VISIBLE else View.GONE
            binding.btnTransfer.text = getString(R.string.approve_transfer)
        }
    }

    private fun setupTransferButton() {
        binding.btnTransfer.setOnClickListener {
            val transaction = currentTransaction
            if (transaction == null || transaction.status != ServiceTransactionStatus.AWAITING_ADMIN) {
                Toast.makeText(this, R.string.transaction_not_ready, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            TransactionRepository.approveTransaction(
                transaction = transaction,
                onSuccess = {
                    ServiceRequestRepository.update(
                        requestId = transaction.requestId,
                        fields = mapOf(ServiceRequest.FIELD_STATUS to ServiceRequestStatus.ADMIN_APPROVED),
                        onSuccess = { showSuccessDialog() },
                        onFailure = { message ->
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            showSuccessDialog()
                        }
                    )
                },
                onFailure = { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun showSuccessDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_transaction_success, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.btnExit).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun setupBottomNavigation() {
        val homeTab = findViewById<android.widget.LinearLayout>(R.id.navTabHome)
        val profileTab = findViewById<android.widget.LinearLayout>(R.id.navTabProfile)
        val settingsTab = findViewById<android.widget.LinearLayout>(R.id.navTabSettings)
        val bubbleIcon = findViewById<android.widget.ImageView>(R.id.navBubbleIcon)

        homeTab.setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
            finishAffinity()
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
}
