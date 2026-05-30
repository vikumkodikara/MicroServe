package com.example.microserve

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class BillActivity : AppCompatActivity() {

    private var requestListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.statusBarColor = Color.TRANSPARENT
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.statusBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_bill)

        val txtBillProvider:    TextView = findViewById(R.id.txtBillProvider)
        val txtBillService:     TextView = findViewById(R.id.txtBillService)
        val txtBillDescription: TextView = findViewById(R.id.txtBillDescription)
        val txtBillClient:      TextView = findViewById(R.id.txtBillClient)
        val txtBillTotal:       TextView = findViewById(R.id.txtBillTotal)
        val chkPaid:            CheckBox = findViewById(R.id.chkPaid)
        val chkComplete:        CheckBox = findViewById(R.id.chkComplete)
        val btnNext:            Button   = findViewById(R.id.btnNext)

        val requestId    = intent.getStringExtra("REQUEST_ID")    ?: ""
        val providerName = intent.getStringExtra("PROVIDER_NAME") ?: "Unknown Provider"
        val category     = intent.getStringExtra("CATEGORY")      ?: "Service"

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Fallback display while loading
        txtBillProvider.text    = "Service Provider: $providerName"
        txtBillService.text     = "Service: $category"
        txtBillDescription.text = ""
        txtBillClient.text      = ""
        txtBillTotal.text       = "Total: — M Points"
        chkPaid.isChecked       = false
        chkComplete.isChecked   = false
        chkPaid.isClickable     = false
        chkComplete.isClickable = false
        btnNext.visibility      = View.GONE

        if (requestId.isBlank()) {
            // Legacy fallback: load from transactionId
            val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: ""
            if (transactionId.isNotBlank()) {
                loadFromTransaction(transactionId, txtBillProvider, txtBillService,
                    txtBillDescription, txtBillClient, txtBillTotal,
                    chkPaid, chkComplete, btnNext, uid)
            }
            return
        }

        // Real-time listener on the request
        requestListener = ServiceRequestRepository.listenById(
            requestId = requestId,
            onUpdate  = { request ->
                txtBillService.text     = "Service: ${request.category.ifBlank { request.title }}"
                txtBillDescription.text = "Description: ${request.description}"
                txtBillClient.text      = "Client: ${request.requesterName}"
                txtBillProvider.text    = "Service Provider: ${request.acceptedProviderName}"
                txtBillTotal.text       = "Total: ${request.acceptedPoints} M Points"

                val isPaid     = request.status == ServiceRequestStatus.IN_PROGRESS ||
                                 request.status == ServiceRequestStatus.PROVIDER_DONE ||
                                 request.status == ServiceRequestStatus.REQUESTER_CONFIRMED ||
                                 request.status == ServiceRequestStatus.ADMIN_APPROVED
                val isComplete = request.status == ServiceRequestStatus.REQUESTER_CONFIRMED ||
                                 request.status == ServiceRequestStatus.ADMIN_APPROVED

                chkPaid.isChecked     = isPaid
                chkComplete.isChecked = isComplete

                val isRequester = uid == request.requesterUid
                val isProvider  = uid == request.acceptedProviderUid

                // ── Pay button (customer only, when approved & not yet paid) ─────────
                if (isRequester && request.status == ServiceRequestStatus.BID_SELECTED) {
                    btnNext.visibility = View.VISIBLE
                    btnNext.text       = "Pay Now"
                    btnNext.setOnClickListener {
                        handlePayment(request, uid)
                    }
                }
                // ── Complete button (provider when paid) ──────────────────────────────
                else if (isProvider && request.status == ServiceRequestStatus.IN_PROGRESS) {
                    btnNext.visibility = View.VISIBLE
                    btnNext.text       = "Mark Complete"
                    btnNext.setOnClickListener {
                        markProviderDone(request)
                    }
                }
                // ── Confirm Complete (customer when provider marked done) ─────────────
                else if (isRequester && request.status == ServiceRequestStatus.PROVIDER_DONE) {
                    btnNext.visibility = View.VISIBLE
                    btnNext.text       = "Confirm & Rate"
                    btnNext.setOnClickListener {
                        confirmAndRate(request)
                    }
                }
                // ── Navigate to Rating after fully confirmed ──────────────────────────
                else if (isRequester && (request.status == ServiceRequestStatus.REQUESTER_CONFIRMED ||
                                         request.status == ServiceRequestStatus.ADMIN_APPROVED)) {
                    btnNext.visibility = View.VISIBLE
                    btnNext.text       = "Rate Provider"
                    btnNext.setOnClickListener {
                        startActivity(
                            Intent(this, RatingActivity::class.java).apply {
                                putExtra("PROVIDER_NAME", request.acceptedProviderName)
                                putExtra("PROVIDER_UID",  request.acceptedProviderUid)
                                putExtra("REQUEST_ID",    request.id)
                            }
                        )
                    }
                } else {
                    btnNext.visibility = View.GONE
                }
            },
            onError = { error ->
                Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ── Payment flow — delegated to Cloud Function ───────────────────────────

    private fun handlePayment(request: ServiceRequest, uid: String) {
        if (request.acceptedPoints <= 0 || request.acceptedProviderUid.isBlank()) {
            Toast.makeText(this, "Invalid payment details", Toast.LENGTH_SHORT).show()
            return
        }

        val btnNext: Button = findViewById(R.id.btnNext)
        btnNext.isEnabled = false
        btnNext.text      = "Processing…"

        // Cloud Function handles: balance check, deduction, escrow credit, status update
        CloudFunctions.processEscrowPayment(
            requestId = request.id,
            onSuccess = { transactionId ->
                runOnUiThread {
                    btnNext.isEnabled = true
                    Toast.makeText(
                        this,
                        "Payment successful! M Points held in escrow.",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Status listener will automatically update the UI via real-time snapshot
                }
            },
            onFailure = { error ->
                runOnUiThread {
                    btnNext.isEnabled = true
                    btnNext.text      = "Pay Now"
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    // If insufficient points, send to wallet
                    if (error.contains("Insufficient", ignoreCase = true)) {
                        startActivity(Intent(this, WalletActivity::class.java))
                    }
                }
            }
        )
    }

    // ── Provider marks job done ───────────────────────────────────────────────

    private fun markProviderDone(request: ServiceRequest) {
        val transactionId = request.transactionId
        if (transactionId.isNotBlank()) {
            TransactionRepository.markProviderDone(
                transactionId = transactionId,
                onSuccess     = {
                    ServiceRequestRepository.update(
                        requestId = request.id,
                        fields    = mapOf(ServiceRequest.FIELD_STATUS to ServiceRequestStatus.PROVIDER_DONE),
                        onSuccess = {
                            Toast.makeText(this, "Marked as done. Waiting for customer confirmation.", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
                    )
                },
                onFailure = { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    // ── Customer confirms, Cloud Function releases escrow ─────────────────────

    private fun confirmAndRate(request: ServiceRequest) {
        val transactionId = request.transactionId
        if (transactionId.isBlank()) {
            Toast.makeText(this, "Transaction not found", Toast.LENGTH_SHORT).show()
            return
        }

        val btnNext: Button = findViewById(R.id.btnNext)
        btnNext.isEnabled = false
        btnNext.text      = "Confirming…"

        // Cloud Function handles: escrow deduction, provider credit, status updates
        CloudFunctions.releaseEscrowToProvider(
            requestId     = request.id,
            transactionId = transactionId,
            onSuccess = {
                runOnUiThread {
                    btnNext.isEnabled = true
                    Toast.makeText(
                        this,
                        "Job complete! M Points released to provider.",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(
                        Intent(this, RatingActivity::class.java).apply {
                            putExtra("PROVIDER_NAME", request.acceptedProviderName)
                            putExtra("PROVIDER_UID",  request.acceptedProviderUid)
                            putExtra("REQUEST_ID",    request.id)
                        }
                    )
                }
            },
            onFailure = { error ->
                runOnUiThread {
                    btnNext.isEnabled = true
                    btnNext.text      = "Confirm & Rate"
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // ── Legacy: load from transaction ID ─────────────────────────────────────

    private fun loadFromTransaction(
        transactionId: String,
        txtBillProvider: TextView, txtBillService: TextView,
        txtBillDescription: TextView, txtBillClient: TextView, txtBillTotal: TextView,
        chkPaid: CheckBox, chkComplete: CheckBox, btnNext: Button, uid: String
    ) {
        TransactionRepository.getById(
            transactionId = transactionId,
            onSuccess     = { transaction ->
                txtBillProvider.text    = "Service Provider: ${transaction.providerName}"
                txtBillService.text     = "Service: ${transaction.requestTitle}"
                txtBillClient.text      = "Client: ${transaction.requesterName}"
                txtBillTotal.text       = "Total: ${transaction.amount} M Points"
                chkPaid.isChecked       = true

                btnNext.visibility = View.VISIBLE
                btnNext.setOnClickListener {
                    startActivity(
                        Intent(this, RatingActivity::class.java).apply {
                            putExtra("PROVIDER_NAME", transaction.providerName)
                            putExtra("PROVIDER_UID",  transaction.providerUid)
                            putExtra("REQUEST_ID",    transaction.requestId)
                        }
                    )
                }
            },
            onFailure = { error ->
                Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        requestListener?.remove()
    }
}
