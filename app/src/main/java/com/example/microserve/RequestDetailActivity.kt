package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityRequestDetailBinding
import com.example.microserve.databinding.ItemBidRowBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class RequestDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }

    private lateinit var binding: ActivityRequestDetailBinding
    private var requestId: String = ""
    private var currentRequest: ServiceRequest? = null
    private var requestListener: ListenerRegistration? = null
    private var bidListener: ListenerRegistration? = null
    private val auth get() = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRequestDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestId = intent.getStringExtra(EXTRA_REQUEST_ID).orEmpty()
        if (requestId.isBlank()) {
            Toast.makeText(this, R.string.request_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupWindowInsets()
        binding.backBtn.setOnClickListener { finish() }
        binding.bidButton.setOnClickListener { openPlaceBid() }
        binding.proceedPaymentButton.setOnClickListener { proceedPayment() }
        binding.finishedButton.setOnClickListener { markFinished() }
    }

    override fun onStart() {
        super.onStart()
        requestListener?.remove()
        requestListener = ServiceRequestRepository.listenById(
            requestId = requestId,
            onUpdate = { request ->
                currentRequest = request
                bindRequest(request)
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        )

        bidListener?.remove()
        bidListener = BidRepository.listenBids(
            requestId = requestId,
            onUpdate = { bids -> bindBids(bids) },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    override fun onStop() {
        requestListener?.remove()
        bidListener?.remove()
        requestListener = null
        bidListener = null
        super.onStop()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentScrollView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            binding.footerBar.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun bindRequest(request: ServiceRequest) {
        val category = CategoryCatalog.findByStoreKey(request.category)
        val serviceName = category?.let { getString(it.nameResId) } ?: request.category
        val avatarRes = category?.imageRes ?: R.drawable.user

        binding.requesterNameText.text = request.requesterName
        binding.requesterAgeText.text = request.city
        binding.requesterAvatar.setImageResource(avatarRes)

        binding.serviceValueText.text = serviceName
        binding.locationValueText.text = request.fullLocation()
        binding.jobValueText.text = formatJobText(request)
        binding.statusValueText.text = formatStatusText(request.status)

        val uid = auth.currentUser?.uid
        val isRequester = uid == request.requesterUid
        val isOpen = request.status == ServiceRequestStatus.OPEN
        val isBidSelected = request.status == ServiceRequestStatus.BID_SELECTED
        val isProviderDone = request.status == ServiceRequestStatus.PROVIDER_DONE

        binding.bidButton.visibility = if (!isRequester && isOpen) View.VISIBLE else View.GONE
        binding.proceedPaymentButton.visibility = if (isRequester && isBidSelected) View.VISIBLE else View.GONE
        binding.finishedButton.visibility = if (isRequester && isProviderDone) View.VISIBLE else View.GONE
    }

    private fun formatJobText(request: ServiceRequest): String {
        val title = request.title.trim()
        val description = request.description.trim()
        return when {
            title.isNotEmpty() && description.isNotEmpty() && !title.equals(description, ignoreCase = true) ->
                "$title ($description)"
            title.isNotEmpty() -> title
            else -> description
        }
    }

    private fun formatStatusText(status: String): String {
        return status.split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun bindBids(bids: List<Bid>) {
        binding.bidsContainer.removeAllViews()
        val request = currentRequest ?: return
        val uid = auth.currentUser?.uid
        val isRequester = uid == request.requesterUid
        val canSelect = isRequester && request.status == ServiceRequestStatus.OPEN
        val inflater = LayoutInflater.from(this)

        if (bids.isEmpty()) {
            binding.previousBidsLabel.visibility = View.GONE
            return
        }

        binding.previousBidsLabel.visibility = View.VISIBLE
        bids.forEach { bid ->
            val rowBinding = ItemBidRowBinding.inflate(inflater, binding.bidsContainer, false)
            rowBinding.bidProviderName.text = bid.providerName
            rowBinding.bidPriceText.text = getString(R.string.bid_price_format, bid.points)
            rowBinding.purchaseButton.visibility = if (canSelect) View.VISIBLE else View.GONE
            rowBinding.purchaseButton.setOnClickListener {
                if (canSelect) {
                    selectBid(bid)
                }
            }
            binding.bidsContainer.addView(rowBinding.root)
        }
    }

    private fun openPlaceBid() {
        startActivity(
            Intent(this, PlaceBidActivity::class.java)
                .putExtra(EXTRA_REQUEST_ID, requestId)
        )
    }

    private fun selectBid(bid: Bid) {
        BidRepository.acceptBid(
            requestId = requestId,
            bid = bid,
            onSuccess = {
                Toast.makeText(this, R.string.bid_selected_success, Toast.LENGTH_SHORT).show()
            },
            onFailure = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun proceedPayment() {
        val request = currentRequest ?: return
        if (request.acceptedPoints <= 0 || request.acceptedProviderUid.isBlank()) {
            Toast.makeText(this, R.string.select_bid_first, Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid ?: return
        PointsRepository.getBalance(
            uid = uid,
            onSuccess = { balance ->
                if (balance < request.acceptedPoints) {
                    Toast.makeText(this, R.string.insufficient_points, Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, WalletActivity::class.java))
                    return@getBalance
                }

                PointsRepository.processEscrowPayment(
                    requesterUid = uid,
                    amount = request.acceptedPoints,
                    onSuccess = {
                        val transaction = ServiceTransaction(
                            requestId = request.id,
                            requestTitle = request.title,
                            requesterUid = uid,
                            requesterName = request.requesterName,
                            providerUid = request.acceptedProviderUid,
                            providerName = request.acceptedProviderName,
                            providerCode = ServiceTransaction.generateProviderCode(request.acceptedProviderUid),
                            amount = request.acceptedPoints
                        )
                        TransactionRepository.createEscrowTransaction(
                            transaction = transaction,
                            onSuccess = { created ->
                                ServiceRequestRepository.update(
                                    requestId = request.id,
                                    fields = mapOf(
                                        ServiceRequest.FIELD_STATUS to ServiceRequestStatus.IN_PROGRESS,
                                        ServiceRequest.FIELD_TRANSACTION_ID to created.id
                                    ),
                                    onSuccess = {
                                        Toast.makeText(this, R.string.payment_success, Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { message ->
                                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            onFailure = { message ->
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onFailure = { message ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onFailure = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun markFinished() {
        val request = currentRequest ?: return
        val transactionId = request.transactionId
        if (transactionId.isBlank()) {
            Toast.makeText(this, R.string.transaction_not_found, Toast.LENGTH_SHORT).show()
            return
        }

        TransactionRepository.markRequesterConfirmed(
            transactionId = transactionId,
            onSuccess = {
                ServiceRequestRepository.update(
                    requestId = request.id,
                    fields = mapOf(ServiceRequest.FIELD_STATUS to ServiceRequestStatus.REQUESTER_CONFIRMED),
                    onSuccess = {
                        startActivity(
                            Intent(this, JobSuccessActivity::class.java)
                                .putExtra(JobSuccessActivity.EXTRA_MESSAGE, getString(R.string.job_success_requester))
                        )
                        finish()
                    },
                    onFailure = { message ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onFailure = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
