package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityRequestDetailBinding
import com.example.microserve.databinding.ItemBidRowBinding

class RequestDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }

    private lateinit var binding: ActivityRequestDetailBinding
    private lateinit var request: RequestStore.UserRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRequestDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
        if (requestId.isNullOrBlank()) {
            Toast.makeText(this, R.string.request_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val loadedRequest = RequestStore.getRequestById(this, requestId)
        if (loadedRequest == null) {
            Toast.makeText(this, R.string.request_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        request = loadedRequest

        setupWindowInsets()
        bindRequest(request)
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        if (::request.isInitialized) {
            bindBids(request)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentScrollView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            binding.footerBar.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun bindRequest(request: RequestStore.UserRequest) {
        val profile = BidSampleData.getProfile(request)
        val serviceName = CategoryCatalog.findByStoreKey(request.category)?.displayName
            ?: request.category

        binding.requesterNameText.text = request.requesterName
        binding.requesterAgeText.text = getString(R.string.age_label_format, profile.age)
        binding.requesterAvatar.setImageResource(profile.avatarRes)

        binding.serviceValueText.text = getString(
            R.string.detail_row_format,
            getString(R.string.service_label),
            serviceName
        )
        binding.locationValueText.text = getString(
            R.string.detail_row_format,
            getString(R.string.location_label),
            request.location
        )
        binding.jobValueText.text = getString(
            R.string.detail_row_format,
            getString(R.string.job_label),
            formatJobText(request)
        )

        bindBids(request)
    }

    private fun formatJobText(request: RequestStore.UserRequest): String {
        val title = request.title.trim()
        val description = request.description.trim()
        return when {
            title.isNotEmpty() && description.isNotEmpty() && !title.equals(description, ignoreCase = true) ->
                "$title ($description)"
            title.isNotEmpty() -> title
            else -> description
        }
    }

    private fun bindBids(request: RequestStore.UserRequest) {
        binding.bidsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val bids = BidSampleData.getBidsForRequest(request)

        bids.forEach { bid ->
            val rowBinding = ItemBidRowBinding.inflate(inflater, binding.bidsContainer, false)
            rowBinding.bidProviderName.text = bid.providerName
            rowBinding.bidPriceText.text = getString(R.string.bid_price_format, bid.priceRs)
            rowBinding.purchaseButton.setOnClickListener {
                Toast.makeText(
                    this,
                    getString(R.string.purchase_confirmed_toast, bid.providerName),
                    Toast.LENGTH_SHORT
                ).show()
            }
            binding.bidsContainer.addView(rowBinding.root)
        }
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener { finish() }
        binding.bidButton.setOnClickListener {
            startActivity(
                Intent(this, PlaceBidActivity::class.java)
                    .putExtra(PlaceBidActivity.EXTRA_REQUEST_ID, request.id)
            )
        }
    }
}
