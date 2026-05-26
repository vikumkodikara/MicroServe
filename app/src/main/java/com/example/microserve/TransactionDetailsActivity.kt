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

class TransactionDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionDetailsBinding
    private var transactionId: String? = null
    private var currentTransaction: TransactionStore.Transaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupBackButton()
        setupBottomNavigation()
        setupTransferButton()
        loadTransaction()
    }

    override fun onResume() {
        super.onResume()
        loadTransaction()
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

    private fun loadTransaction() {
        transactionId = intent.getStringExtra("TRANSACTION_ID")
        val id = transactionId
        if (id.isNullOrBlank()) {
            Toast.makeText(this, "Transaction not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val item = TransactionStore.getTransactionById(this, id)
        if (item == null) {
            Toast.makeText(this, "Transaction not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentTransaction = item

        binding.tvProviderId.text = item.providerCode
        binding.tvProviderName.text = item.providerName
        binding.tvAmount.text = TransactionStore.formatAmount(item.amount)
        binding.tvTransactionCode.text = item.transactionCode
        binding.tvStatus.text = item.status

        if (item.status.equals(TransactionStore.STATUS_SUCCESS, ignoreCase = true)) {
            binding.tvHeaderLine1.text = "Transaction Success"
            binding.tvStatus.setBackgroundResource(R.drawable.txn_success_tag_bg)
            binding.tvStatus.setTextColor(resources.getColor(R.color.white, null))
            binding.btnTransfer.visibility = View.GONE
        } else {
            binding.tvHeaderLine1.text = "Transfer Money"
            binding.tvStatus.setBackgroundResource(R.drawable.txn_pending_tag_bg)
            binding.tvStatus.setTextColor(resources.getColor(android.R.color.black, null))
            binding.btnTransfer.visibility = View.VISIBLE
        }
    }

    private fun setupTransferButton() {
        binding.btnTransfer.setOnClickListener {
            val id = transactionId
            if (id.isNullOrBlank()) {
                Toast.makeText(this, "Invalid transaction", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val moved = TransactionStore.markTransactionSuccessAndCreditUser(this, id)
            if (!moved) {
                Toast.makeText(this, "Unable to transfer", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showSuccessDialog()
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
