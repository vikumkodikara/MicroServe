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
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class BillActivity : AppCompatActivity() {

    private var requestListener: ListenerRegistration? = null

    /**
     * Resolves the current user's UID using three layers:
     *  1. FirebaseAuth.currentUser  (normal case)
     *  2. AppPreferences session    (fallback if Auth hasn't restored yet)
     *  3. Empty string              (triggers login-required guard)
     */
    private fun resolveUid(): String {
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (firebaseUid.isNotBlank()) {
            Log.d("AuthDebug", "UID from FirebaseAuth: $firebaseUid")
            return firebaseUid
        }
        val sessionUid = AppPreferences.getSessionUid(this)
        Log.w("AuthDebug", "FirebaseAuth returned null — falling back to AppPreferences uid='$sessionUid'")
        return sessionUid
    }

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

        // Log auth state at startup so we can diagnose null-uid issues
        val startupUid = resolveUid()
        Log.d("AuthDebug", "BillActivity started — uid='$startupUid' requestId='$requestId'")

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
                    chkPaid, chkComplete, btnNext, resolveUid())
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

                // ── Resolve UID fresh on every snapshot update ───────────────────────
                // This avoids the stale-null problem when FirebaseAuth restores async.
                val currentUid  = resolveUid()
                val isRequester = currentUid == request.requesterUid
                val isProvider  = currentUid == request.acceptedProviderUid

                Log.d("AuthDebug", "Snapshot update — currentUid='$currentUid' " +
                    "requesterUid='${request.requesterUid}' " +
                    "providerUid='${request.acceptedProviderUid}' " +
                    "status='${request.status}' " +
                    "isRequester=$isRequester isProvider=$isProvider")

                // ── Pay button (customer only, when bid selected & not yet paid) ──────
                if (isRequester && request.status == ServiceRequestStatus.BID_SELECTED) {
                    btnNext.visibility = View.VISIBLE
                    btnNext.text       = "Pay Now"
                    btnNext.setOnClickListener {
                        handlePayment(request, currentUid)
                    }
                }
                // ── Complete button (provider when paid) ──────────────────────────
                else if (isProvider && request.status == ServiceRequestStatus.IN_PROGRESS) {
                    btnNext.visibility = View.VISIBLE
                    btnNext.text       = "Mark Complete"
                    btnNext.setOnClickListener {
                        markProviderDone(request)
                    }
                }
                // ── Confirm Complete (customer when provider marked done) ────────────
                else if (isRequester && request.status == ServiceRequestStatus.PROVIDER_DONE) {
                    btnNext.visibility = View.VISIBLE
                    btnNext.text       = "Confirm & Rate"
                    btnNext.setOnClickListener {
                        confirmAndRate(request)
                    }
                }
                // ── Navigate to Rating after fully confirmed ─────────────────────
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

    // ── Payment flow — direct Firestore (rules allow signed-in users) ──────────

    private fun handlePayment(request: ServiceRequest, uid: String) {
        // Use requesterUid from the ServiceRequest itself as the authoritative user ID.
        // This avoids any FirebaseAuth.currentUser timing issue, since the user's UID
        // is already stored inside the document they own.
        val requesterUid = request.requesterUid.ifBlank { uid }

        Log.d("AuthCheck", "User ID: ${FirebaseAuth.getInstance().currentUser?.uid ?: "null (using requesterUid=$requesterUid)"}")
        Log.d("PaymentDebug", "Pay Now — requestId='${request.id}' requesterUid='$requesterUid' points=${request.acceptedPoints}")

        if (request.id.isBlank()) {
            Log.e("PaymentDebug", "requestId is BLANK — cannot proceed")
            Toast.makeText(this, "Error: request ID is missing. Please reopen this bill.", Toast.LENGTH_LONG).show()
            return
        }
        if (request.acceptedPoints <= 0 || request.acceptedProviderUid.isBlank()) {
            Toast.makeText(this, "Invalid payment details", Toast.LENGTH_SHORT).show()
            return
        }

        val btnNext: Button = findViewById(R.id.btnNext)
        btnNext.isEnabled = false
        btnNext.text      = "Processing…"

        // Step 1: Check balance using requesterUid from the document
        PointsRepository.getBalance(
            uid       = requesterUid,
            onSuccess = { balance ->
                if (balance < request.acceptedPoints) {
                    runOnUiThread {
                        btnNext.isEnabled = true
                        btnNext.text      = "Pay Now"
                        Toast.makeText(this,
                            "Insufficient M Points. You have $balance, need ${request.acceptedPoints}.",
                            Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, WalletActivity::class.java))
                    }
                    return@getBalance
                }

                // Step 2: Deduct from customer and credit escrow
                PointsRepository.processEscrowPayment(
                    requesterUid = requesterUid,
                    amount       = request.acceptedPoints,
                    onSuccess    = {
                        // Step 3: Create transaction record
                        val txn = ServiceTransaction(
                            requestId     = request.id,
                            requestTitle  = request.title.ifBlank { request.category },
                            requesterUid  = requesterUid,
                            requesterName = request.requesterName,
                            providerUid   = request.acceptedProviderUid,
                            providerName  = request.acceptedProviderName,
                            providerCode  = ServiceTransaction.generateProviderCode(request.acceptedProviderUid),
                            amount        = request.acceptedPoints
                        )
                        TransactionRepository.createEscrowTransaction(
                            transaction = txn,
                            onSuccess   = { created ->
                                // Step 4: Update request status
                                ServiceRequestRepository.update(
                                    requestId = request.id,
                                    fields    = mapOf(
                                        ServiceRequest.FIELD_STATUS         to ServiceRequestStatus.IN_PROGRESS,
                                        ServiceRequest.FIELD_TRANSACTION_ID to created.id
                                    ),
                                    onSuccess = {
                                        runOnUiThread {
                                            btnNext.isEnabled = true
                                            Toast.makeText(this,
                                                "Payment successful! M Points held in escrow.",
                                                Toast.LENGTH_SHORT).show()
                                            // Real-time listener updates the UI automatically
                                        }
                                    },
                                    onFailure = { err ->
                                        runOnUiThread {
                                            btnNext.isEnabled = true
                                            btnNext.text = "Pay Now"
                                            Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            },
                            onFailure = { err ->
                                runOnUiThread {
                                    btnNext.isEnabled = true
                                    btnNext.text = "Pay Now"
                                    Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    },
                    onFailure = { err ->
                        runOnUiThread {
                            btnNext.isEnabled = true
                            btnNext.text = "Pay Now"
                            Toast.makeText(this, err, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            },
            onFailure = { err ->
                runOnUiThread {
                    btnNext.isEnabled = true
                    btnNext.text = "Pay Now"
                    Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
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

    // ── Customer confirms — direct Firestore escrow release ───────────────────

    private fun confirmAndRate(request: ServiceRequest) {
        val transactionId = request.transactionId
        Log.d("PaymentDebug", "Confirm & Rate — requestId='${request.id}' transactionId='$transactionId'")

        if (transactionId.isBlank()) {
            Log.e("PaymentDebug", "transactionId is BLANK — cannot release escrow")
            Toast.makeText(this, "Transaction not found", Toast.LENGTH_SHORT).show()
            return
        }

        val btnNext: Button = findViewById(R.id.btnNext)
        btnNext.isEnabled = false
        btnNext.text      = "Confirming…"

        // Step 1: Mark requester confirmed in transaction record
        TransactionRepository.markRequesterConfirmed(
            transactionId = transactionId,
            onSuccess = {
                // Step 2: Fetch transaction to get provider UID and amount
                TransactionRepository.getById(
                    transactionId = transactionId,
                    onSuccess = { txn ->
                        // Step 3: Release escrow → provider balance
                        TransactionRepository.approveTransaction(
                            transaction = txn,
                            onSuccess = {
                                // Step 4: Update request status to admin_approved
                                ServiceRequestRepository.update(
                                    requestId = request.id,
                                    fields    = mapOf(ServiceRequest.FIELD_STATUS to ServiceRequestStatus.ADMIN_APPROVED),
                                    onSuccess = {
                                        runOnUiThread {
                                            btnNext.isEnabled = true
                                            Toast.makeText(this,
                                                "Job complete! M Points released to provider.",
                                                Toast.LENGTH_SHORT).show()
                                            startActivity(
                                                Intent(this, RatingActivity::class.java).apply {
                                                    putExtra("PROVIDER_NAME", request.acceptedProviderName)
                                                    putExtra("PROVIDER_UID",  request.acceptedProviderUid)
                                                    putExtra("REQUEST_ID",    request.id)
                                                }
                                            )
                                        }
                                    },
                                    onFailure = { err ->
                                        runOnUiThread {
                                            btnNext.isEnabled = true
                                            btnNext.text = "Confirm & Rate"
                                            Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            },
                            onFailure = { err ->
                                runOnUiThread {
                                    btnNext.isEnabled = true
                                    btnNext.text = "Confirm & Rate"
                                    Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    },
                    onFailure = { err ->
                        runOnUiThread {
                            btnNext.isEnabled = true
                            btnNext.text = "Confirm & Rate"
                            Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            onFailure = { err ->
                runOnUiThread {
                    btnNext.isEnabled = true
                    btnNext.text = "Confirm & Rate"
                    Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
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
