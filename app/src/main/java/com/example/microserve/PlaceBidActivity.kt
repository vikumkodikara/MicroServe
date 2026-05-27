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

class PlaceBidActivity : AppCompatActivity() {

    private lateinit var bidsContainer: LinearLayout

    private data class Bid(val name: String, val price: String)

    private val bids = mutableListOf(
        Bid("Anuja Silva", "Rs. 4,000"),
        Bid("Kulathunga Herath", "Rs. 3,500")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_place_bid)

        bidsContainer = findViewById(R.id.bidsListContainer)
        val etAmount = findViewById<EditText>(R.id.et_bid_amount)
        val etTime = findViewById<EditText>(R.id.et_completion_time)
        val cbAgree = findViewById<CheckBox>(R.id.cb_agree)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.btn_place_bid).setOnClickListener {
            val amount = etAmount.text.toString().trim()
            val time = etTime.text.toString().trim()

            if (amount.isBlank()) {
                Toast.makeText(this, "Please enter bid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!cbAgree.isChecked) {
                Toast.makeText(this, "Please agree to terms", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            bids.add(0, Bid("You", "Rs. $amount"))
            etAmount.text.clear()
            etTime.text.clear()
            cbAgree.isChecked = false
            Toast.makeText(this, "Bid placed successfully", Toast.LENGTH_SHORT).show()
            loadBids()
        }

        loadBids()
    }

    private fun loadBids() {
        bidsContainer.removeAllViews()

        for (bid in bids) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_previous_bid, bidsContainer, false)
            item.findViewById<TextView>(R.id.tv_bidder_name).text = bid.name
            item.findViewById<TextView>(R.id.tv_bid_price).text = "Bid Price: ${bid.price}"

            item.findViewById<View>(R.id.btn_purchase).setOnClickListener {
                Toast.makeText(this, "Purchased from ${bid.name}", Toast.LENGTH_SHORT).show()
            }

            bidsContainer.addView(item)
        }
    }
}
