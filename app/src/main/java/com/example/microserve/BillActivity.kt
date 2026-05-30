package com.example.microserve

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class BillActivity : AppCompatActivity() {

    private var requestListener: ListenerRegistration? = null
    private var balanceListener: ListenerRegistration? = null

    private lateinit var txtBillProvider: TextView
    private lateinit var txtBillProviderPhone: TextView
    private lateinit var providerContactRow: View
    private lateinit var btnCallProvider: View
    private lateinit var txtBillService: TextView
    private lateinit var txtBillDescription: TextView
    private lateinit var txtBillClient: TextView
    private lateinit var txtBillTotal: TextView
    private lateinit var txtWalletBalance: TextView
    private lateinit var walletStrip: View
    private lateinit var btnNext: Button
    private lateinit var stepDot1: View
    private lateinit var stepDot2: View
    private lateinit var stepDot3: View
    private lateinit var stepLine1: View
    private lateinit var stepLine2: View
    private lateinit var stepLabel1: TextView
    private lateinit var stepLabel2: TextView
    private lateinit var stepLabel3: TextView

    private var currentRequest: ServiceRequest? = null
    private var walletBalance: Int = 0
    private var cachedProviderPhoneUid: String? = null
    private var cachedProviderPhone: String = ""
    private var providerPhoneLoadInProgress = false

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
        setContentView(R.layout.activity_bill)

        bindViews()
        setupWindowInsets()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnTopUp).setOnClickListener {
            startActivity(Intent(this, WalletActivity::class.java))
        }

        val requestId    = intent.getStringExtra("REQUEST_ID")    ?: ""
        val providerName = intent.getStringExtra("PROVIDER_NAME") ?: "Unknown Provider"
        val category     = intent.getStringExtra("CATEGORY")      ?: "Service"

        // ── Intent verification: print raw extras so we can spot null/empty IDs ──
        Log.d("DEBUG_ID", "Intent extras — REQUEST_ID raw  : '${intent.getStringExtra("REQUEST_ID")}'")
        Log.d("DEBUG_ID", "Intent extras — requestId final : '$requestId'")
        Log.d("DEBUG_ID", "Intent extras — PROVIDER_NAME   : '$providerName'")
        Log.d("DEBUG_ID", "Intent extras — CATEGORY        : '$category'")

        // Log auth state at startup so we can diagnose null-uid issues
        val startupUid = resolveUid()
        Log.d("AuthDebug", "BillActivity started — uid='$startupUid' requestId='$requestId'")

        txtBillProvider.text    = providerName
        txtBillService.text     = category
        txtBillDescription.text = ""
        txtBillClient.text      = ""
        txtBillTotal.text       = getString(R.string.bill_m_points_format, getString(R.string.bill_loading_total))
        btnNext.visibility      = View.GONE
        updateStatusStepper(ServiceRequestStatus.BID_SELECTED)

        if (requestId.isBlank()) {
            val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: ""
            if (transactionId.isNotBlank()) {
                loadFromTransaction(transactionId)
            }
            return
        }

        requestListener = ServiceRequestRepository.listenById(
            requestId = requestId,
            onUpdate  = { request ->
                val currentUid = resolveUid()
                Log.d("AuthDebug", "Snapshot update — currentUid='$currentUid' " +
                    "requesterUid='${request.requesterUid}' " +
                    "providerUid='${request.acceptedProviderUid}' " +
                    "status='${request.status}'")
                
                currentRequest = request
                bindRequest(request, currentUid)
            },
            onError = { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        startBalanceListener()
    }

    override fun onPause() {
        super.onPause()
        balanceListener?.remove()
        balanceListener = null
    }

    override fun onDestroy() {
        super.onDestroy()
        requestListener?.remove()
    }

    private fun bindViews() {
        txtBillProvider    = findViewById(R.id.txtBillProvider)
        txtBillProviderPhone = findViewById(R.id.txtBillProviderPhone)
        providerContactRow = findViewById(R.id.providerContactRow)
        btnCallProvider    = findViewById(R.id.btnCallProvider)
        txtBillService     = findViewById(R.id.txtBillService)
        txtBillDescription = findViewById(R.id.txtBillDescription)
        txtBillClient      = findViewById(R.id.txtBillClient)
        txtBillTotal       = findViewById(R.id.txtBillTotal)
        txtWalletBalance   = findViewById(R.id.txtWalletBalance)
        walletStrip        = findViewById(R.id.walletStrip)
        btnNext            = findViewById(R.id.btnNext)
        stepDot1           = findViewById(R.id.stepDot1)
        stepDot2           = findViewById(R.id.stepDot2)
        stepDot3           = findViewById(R.id.stepDot3)
        stepLine1          = findViewById(R.id.stepLine1)
        stepLine2          = findViewById(R.id.stepLine2)
        stepLabel1         = findViewById(R.id.stepLabel1)
        stepLabel2         = findViewById(R.id.stepLabel2)
        stepLabel3         = findViewById(R.id.stepLabel3)
    }

    private fun setupWindowInsets() {
        SystemUiHelper.setupPurpleHeaderScreen(
            activity = this,
            root = findViewById(R.id.billRoot),
            headerView = findViewById(R.id.headerSection),
            footerBar = findViewById(R.id.footerBar)
        )
    }

    private fun startBalanceListener() {
        val uid = resolveUid()
        if (uid.isBlank()) {
            walletStrip.visibility = View.GONE
            return
        }

        balanceListener?.remove()
        balanceListener = PointsRepository.listenBalance(
            uid = uid,
            onUpdate = { balance ->
                walletBalance = balance
                updateWalletStrip()
            },
            onError = {
                walletStrip.visibility = View.GONE
            }
        )
    }

    private fun updateWalletStrip() {
        val request = currentRequest
        val uid = resolveUid()
        val isRequesterAwaitingPayment = request != null &&
            uid == request.requesterUid &&
            request.status == ServiceRequestStatus.BID_SELECTED

        if (isRequesterAwaitingPayment) {
            walletStrip.visibility = View.VISIBLE
            txtWalletBalance.text = getString(
                R.string.bill_your_balance
            ) + ": " + MPointsPaymentHelper.formatMPoints(walletBalance)
        } else {
            walletStrip.visibility = View.GONE
        }
    }

    private fun bindRequest(request: ServiceRequest, uid: String) {
        txtBillService.text     = request.category.ifBlank { request.title }
        txtBillDescription.text = request.description
        txtBillClient.text      = request.requesterName
        txtBillProvider.text    = request.acceptedProviderName
        txtBillTotal.text       = MPointsPaymentHelper.formatMPoints(request.acceptedPoints)

        updateStatusStepper(request.status)
        updateWalletStrip()
        updateProviderContact(request, uid)

        val isRequester = uid == request.requesterUid
        val isProvider  = uid == request.acceptedProviderUid

        when {
            isRequester && request.status == ServiceRequestStatus.BID_SELECTED -> {
                btnNext.visibility = View.VISIBLE
                btnNext.text       = getString(R.string.bill_pay_now)
                btnNext.setOnClickListener { handlePayment(request) }
            }
            isProvider && request.status == ServiceRequestStatus.IN_PROGRESS -> {
                btnNext.visibility = View.VISIBLE
                btnNext.text       = getString(R.string.bill_mark_complete)
                btnNext.setOnClickListener { markProviderDone(request) }
            }
            isRequester && request.status == ServiceRequestStatus.PROVIDER_DONE -> {
                btnNext.visibility = View.VISIBLE
                btnNext.text       = getString(R.string.bill_confirm_and_rate)
                btnNext.setOnClickListener { confirmAndRate(request) }
            }
            isRequester && (request.status == ServiceRequestStatus.REQUESTER_CONFIRMED ||
                request.status == ServiceRequestStatus.ADMIN_APPROVED) -> {
                btnNext.visibility = View.VISIBLE
                btnNext.text       = getString(R.string.bill_rate_provider)
                btnNext.setOnClickListener {
                    startActivity(
                        Intent(this, RatingActivity::class.java).apply {
                            putExtra("PROVIDER_NAME", request.acceptedProviderName)
                            putExtra("PROVIDER_UID",  request.acceptedProviderUid)
                            putExtra("REQUEST_ID",    request.id)
                        }
                    )
                }
            }
            else -> {
                btnNext.visibility = View.GONE
            }
        }
    }

    private fun updateProviderContact(request: ServiceRequest, uid: String) {
        val isRequester = uid == request.requesterUid
        val hasProvider = request.acceptedProviderUid.isNotBlank() &&
            request.status != ServiceRequestStatus.OPEN

        if (!isRequester || !hasProvider) {
            providerContactRow.visibility = View.GONE
            return
        }

        providerContactRow.visibility = View.VISIBLE
        btnCallProvider.isEnabled = false
        btnCallProvider.alpha = 0.5f
        providerContactRow.setOnClickListener(null)

        if (request.acceptedProviderUid != cachedProviderPhoneUid) {
            cachedProviderPhone = ""
            providerPhoneLoadInProgress = false
        }

        if (request.acceptedProviderUid == cachedProviderPhoneUid && cachedProviderPhone.isNotBlank()) {
            showProviderPhone(cachedProviderPhone)
            return
        }

        if (providerPhoneLoadInProgress && request.acceptedProviderUid == cachedProviderPhoneUid) {
            txtBillProviderPhone.text = getString(R.string.bill_contact_loading)
            return
        }

        providerPhoneLoadInProgress = true
        cachedProviderPhoneUid = request.acceptedProviderUid
        txtBillProviderPhone.text = getString(R.string.bill_contact_loading)

        UserRepository.getPhoneByUid(
            uid = request.acceptedProviderUid,
            onSuccess = { phone ->
                runOnUiThread {
                    providerPhoneLoadInProgress = false
                    cachedProviderPhone = phone
                    showProviderPhone(phone)
                }
            },
            onFailure = {
                runOnUiThread {
                    providerPhoneLoadInProgress = false
                    cachedProviderPhone = ""
                    txtBillProviderPhone.text = getString(R.string.bill_contact_unavailable)
                    btnCallProvider.isEnabled = false
                    btnCallProvider.alpha = 0.5f
                    btnCallProvider.setOnClickListener(null)
                }
            }
        )
    }

    private fun showProviderPhone(phone: String) {
        txtBillProviderPhone.text = phone
        btnCallProvider.isEnabled = true
        btnCallProvider.alpha = 1f
        btnCallProvider.setOnClickListener { dialPhoneNumber(phone) }
        providerContactRow.setOnClickListener { dialPhoneNumber(phone) }
    }

    private fun dialPhoneNumber(phone: String) {
        val normalized = phone.filter { it.isDigit() || it == '+' }
        if (normalized.isBlank()) {
            Toast.makeText(this, R.string.bill_contact_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$normalized")))
    }

    private fun updateStatusStepper(status: String) {
        val activeColor = ContextCompat.getColor(this, R.color.purple_nav)
        val inactiveColor = 0xFFE0E0E0.toInt()
        val activeLabelColor = ContextCompat.getColor(this, R.color.purple_dark)
        val inactiveLabelColor = 0xFF888888.toInt()

        val step = when (status) {
            ServiceRequestStatus.BID_SELECTED -> 1
            ServiceRequestStatus.IN_PROGRESS,
            ServiceRequestStatus.PROVIDER_DONE -> 2
            ServiceRequestStatus.REQUESTER_CONFIRMED,
            ServiceRequestStatus.ADMIN_APPROVED -> 3
            else -> 1
        }

        setStepDot(stepDot1, step >= 1, activeColor, inactiveColor)
        setStepDot(stepDot2, step >= 2, activeColor, inactiveColor)
        setStepDot(stepDot3, step >= 3, activeColor, inactiveColor)
        stepLine1.setBackgroundColor(if (step >= 2) activeColor else inactiveColor)
        stepLine2.setBackgroundColor(if (step >= 3) activeColor else inactiveColor)

        stepLabel1.setTextColor(if (step >= 1) activeLabelColor else inactiveLabelColor)
        stepLabel2.setTextColor(if (step >= 2) activeLabelColor else inactiveLabelColor)
        stepLabel3.setTextColor(if (step >= 3) activeLabelColor else inactiveLabelColor)

        val bold = android.graphics.Typeface.DEFAULT_BOLD
        val normal = android.graphics.Typeface.DEFAULT
        stepLabel1.typeface = if (step == 1) bold else normal
        stepLabel2.typeface = if (step == 2) bold else normal
        stepLabel3.typeface = if (step == 3) bold else normal
    }

    private fun setStepDot(dot: View, active: Boolean, activeColor: Int, inactiveColor: Int) {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(if (active) activeColor else inactiveColor)
        dot.background = drawable
    }

    private fun handlePayment(request: ServiceRequest) {
        // My detailed logs
        Log.d("DEBUG_ID",     "Processing Request ID: ${request.id}")
        Log.d("AuthCheck",    "User ID: ${resolveUid()}")
        Log.d("PaymentDebug", "Pay Now — customerId='${request.requesterUid}' providerId='${request.acceptedProviderUid}' cost=${request.acceptedPoints}")

        if (request.id.isBlank()) {
            Log.e("DEBUG_ID", "requestId is BLANK — cannot proceed")
            Toast.makeText(this, R.string.bill_request_id_missing, Toast.LENGTH_LONG).show()
            return
        }
        if (request.acceptedPoints <= 0 || request.acceptedProviderUid.isBlank()) {
            Toast.makeText(this, R.string.bill_invalid_payment_details, Toast.LENGTH_SHORT).show()
            return
        }

        MPointsPaymentHelper.showPaymentDialog(
            activity = this,
            amount = request.acceptedPoints,
            providerName = request.acceptedProviderName
        ) {
            executeEscrowPayment(request)
        }
    }

    private fun executeEscrowPayment(request: ServiceRequest) {
        btnNext.isEnabled = false
        btnNext.text      = getString(R.string.bill_processing)

        // My PRE-CHECK
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection(ServiceRequest.COLLECTION)
            .document(request.id)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    Log.e("DEBUG_ID", "PRE-CHECK FAILED: document '${request.id}' does NOT exist in '${ServiceRequest.COLLECTION}'")
                    runOnUiThread {
                        btnNext.isEnabled = true
                        btnNext.text      = getString(R.string.bill_pay_now)
                        Toast.makeText(this, "Request record missing in database. Please go back and reopen.", Toast.LENGTH_LONG).show()
                    }
                    return@addOnSuccessListener
                }

                Log.d("DEBUG_ID", "PRE-CHECK PASSED: document '${request.id}' found in '${ServiceRequest.COLLECTION}'")

                // Incoming Cloud Function call
                CloudFunctions.processPayment(
                    requestId = request.id,
                    customerId = request.requesterUid,
                    providerId = request.acceptedProviderUid,
                    cost = request.acceptedPoints,
                    onSuccess = { transactionId ->
                        runOnUiThread {
                            btnNext.isEnabled = true
                            Toast.makeText(this, R.string.bill_payment_success_escrow, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            btnNext.isEnabled = true
                            btnNext.text      = getString(R.string.bill_pay_now)
                            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                            if (error.contains("Insufficient", ignoreCase = true)) {
                                startActivity(Intent(this, WalletActivity::class.java))
                            }
                        }
                    }
                )
            }
            .addOnFailureListener { e ->
                Log.e("DEBUG_ID", "PRE-CHECK network error: ${e.message}")
                runOnUiThread {
                    btnNext.isEnabled = true
                    btnNext.text      = getString(R.string.bill_pay_now)
                    Toast.makeText(this, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
    }

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
                            Toast.makeText(this, R.string.bill_mark_done_success, Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
                    )
                },
                onFailure = { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    private fun confirmAndRate(request: ServiceRequest) {
        val transactionId = request.transactionId
        Log.d("PaymentDebug", "Confirm & Rate — requestId='${request.id}' transactionId='$transactionId'")

        if (transactionId.isBlank()) {
            Toast.makeText(this, R.string.transaction_not_found, Toast.LENGTH_SHORT).show()
            return
        }

        btnNext.isEnabled = false
        btnNext.text      = getString(R.string.bill_confirming)

        // Incoming Cloud Function
        CloudFunctions.releasePayment(
            requestId     = request.id,
            transactionId = transactionId,
            onSuccess = {
                runOnUiThread {
                    btnNext.isEnabled = true
                    Log.d("PaymentDebug", "Release success")
                    Toast.makeText(this, R.string.bill_job_complete_released, Toast.LENGTH_SHORT).show()
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
                    btnNext.text      = getString(R.string.bill_confirm_and_rate)
                    Log.e("PaymentDebug", "Release failed: $error")
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun loadFromTransaction(transactionId: String) {
        TransactionRepository.getById(
            transactionId = transactionId,
            onSuccess     = { transaction ->
                txtBillProvider.text    = transaction.providerName
                txtBillService.text     = transaction.requestTitle
                txtBillClient.text      = transaction.requesterName
                txtBillTotal.text       = MPointsPaymentHelper.formatMPoints(transaction.amount)
                updateStatusStepper(ServiceRequestStatus.ADMIN_APPROVED)
                walletStrip.visibility  = View.GONE
                providerContactRow.visibility = View.GONE

                if (transaction.providerUid.isNotBlank()) {
                    UserRepository.getPhoneByUid(
                        uid = transaction.providerUid,
                        onSuccess = { phone ->
                            runOnUiThread {
                                providerContactRow.visibility = View.VISIBLE
                                showProviderPhone(phone)
                            }
                        },
                        onFailure = { /* contact optional for legacy flow */ }
                    )
                }

                btnNext.visibility = View.VISIBLE
                btnNext.text       = getString(R.string.bill_rate_provider)
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
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
