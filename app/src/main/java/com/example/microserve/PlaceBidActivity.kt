package com.example.microserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class PlaceBidActivity : AppCompatActivity() {

    private lateinit var bidsContainer: LinearLayout
    private lateinit var bidFormCard: View
    private var requestId: String = ""
    private var currentRequest: ServiceRequest? = null
    private var requestListener: ListenerRegistration? = null
    private var bidListener: ListenerRegistration? = null
    private var latestBids: List<Bid> = emptyList()
    private val auth get() = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_place_bid)

        requestId = intent.getStringExtra(RequestDetailActivity.EXTRA_REQUEST_ID).orEmpty()
        if (requestId.isBlank()) {
            Toast.makeText(this, R.string.request_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bidsContainer = findViewById(R.id.bidsListContainer)
        bidFormCard = findViewById(R.id.bidFormCard)
        val etAmount = findViewById<EditText>(R.id.et_bid_amount)
        val etTime = findViewById<EditText>(R.id.et_completion_time)
        val cbAgree = findViewById<CheckBox>(R.id.cb_agree)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.btn_place_bid).setOnClickListener {
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amountText = etAmount.text.toString().trim()
            val timeText = etTime.text.toString().trim()
            val points = amountText.toIntOrNull()

            when {
                points == null || points <= 0 -> {
                    Toast.makeText(this, R.string.invalid_bid_amount, Toast.LENGTH_SHORT).show()
                }
                !cbAgree.isChecked -> {
                    Toast.makeText(this, R.string.agree_terms_required, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    ServiceRequestRepository.getById(
                        requestId = requestId,
                        onSuccess = { request ->
                            if (request.requesterUid == user.uid) {
                                Toast.makeText(this, R.string.cannot_bid_own_request, Toast.LENGTH_SHORT).show()
                                return@getById
                            }
                            if (request.status != ServiceRequestStatus.OPEN) {
                                Toast.makeText(this, R.string.request_not_open, Toast.LENGTH_SHORT).show()
                                return@getById
                            }

                            val session = AppPreferences.getSessionProfile(this)
                            val providerName = session.name.takeIf { it.isNotBlank() }
                                ?: user.displayName
                                ?: user.email?.substringBefore("@")
                                ?: "Provider"
                            val completionHours = timeText.filter { it.isDigit() }.toIntOrNull() ?: 0

                            BidRepository.placeBid(
                                requestId = requestId,
                                bid = Bid(
                                    providerUid = user.uid,
                                    providerName = providerName,
                                    points = points,
                                    completionHours = completionHours
                                ),
                                onSuccess = {
                                    etAmount.text.clear()
                                    etTime.text.clear()
                                    cbAgree.isChecked = false
                                    Toast.makeText(this, R.string.bid_placed_success, Toast.LENGTH_SHORT).show()
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
        }
    }

    override fun onStart() {
        super.onStart()
        requestListener?.remove()
        requestListener = ServiceRequestRepository.listenById(
            requestId = requestId,
            onUpdate = { request ->
                currentRequest = request
                bindRequestRole(request)
                renderBids(latestBids)
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        )

        bidListener?.remove()
        bidListener = BidRepository.listenBids(
            requestId = requestId,
            onUpdate = { bids ->
                latestBids = bids
                renderBids(bids)
            },
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

    private fun bindRequestRole(request: ServiceRequest) {
        val uid = auth.currentUser?.uid
        val isRequester = uid == request.requesterUid
        bidFormCard.visibility = if (isRequester) View.GONE else View.VISIBLE
    }

    private fun renderBids(bids: List<Bid>) {
        bidsContainer.removeAllViews()
        if (bids.isEmpty()) return

        val request = currentRequest
        val uid = auth.currentUser?.uid
        val canSelect = request != null &&
            uid == request.requesterUid &&
            request.status == ServiceRequestStatus.OPEN

        for (bid in bids) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_previous_bid, bidsContainer, false)
            item.findViewById<TextView>(R.id.tv_bidder_name).text = bid.providerName
            item.findViewById<TextView>(R.id.tv_bid_price).text =
                getString(R.string.bid_price_format, bid.points)

            val purchaseButton = item.findViewById<View>(R.id.btn_purchase)
            purchaseButton.visibility = if (canSelect) View.VISIBLE else View.GONE
            purchaseButton.setOnClickListener {
                if (canSelect) {
                    acceptBid(bid)
                }
            }

            bidsContainer.addView(item)
        }
    }

    private fun acceptBid(bid: Bid) {
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
}
