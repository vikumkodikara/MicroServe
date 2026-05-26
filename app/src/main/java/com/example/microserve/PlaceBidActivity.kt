package com.example.microserve

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityPlaceBidBinding
import com.example.microserve.databinding.ItemBidRowBinding

class PlaceBidActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }

    private lateinit var binding: ActivityPlaceBidBinding
    private lateinit var request: RequestStore.UserRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPlaceBidBinding.inflate(layoutInflater)
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
        bindBids()
        setupClickListeners()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentScrollView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            binding.footerBar.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun bindBids() {
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
        binding.placeBidSubmitButton.setOnClickListener { submitBid() }
    }

    private fun submitBid() {
        if (!binding.termsCheckbox.isChecked) {
            Toast.makeText(this, R.string.terms_not_agreed, Toast.LENGTH_SHORT).show()
            return
        }

        val amount = binding.bidAmountInput.text.toString().trim().toIntOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, R.string.bid_amount_invalid, Toast.LENGTH_SHORT).show()
            return
        }

        val completionHours = binding.completionTimeInput.text.toString().trim().toIntOrNull()
        if (completionHours == null || completionHours <= 0) {
            Toast.makeText(this, R.string.completion_time_invalid, Toast.LENGTH_SHORT).show()
            return
        }

        BidSampleData.addBid(request.id, amount, completionHours)
        Toast.makeText(this, R.string.bid_submitted_toast, Toast.LENGTH_SHORT).show()
        finish()
    }
}
