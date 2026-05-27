package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class RequestMainActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_request_main)

        container = findViewById(R.id.requestsListContainer)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.btn_requests).setOnClickListener {
            startActivity(Intent(this, CreateRequestActivity::class.java))
        }

        val categoryMap = mapOf(
            R.id.cat_plumbing to "Plumbing",
            R.id.cat_gardening to "Gardening",
            R.id.cat_cleaning to "Cleaning",
            R.id.cat_painting to "Painting",
            R.id.cat_electric to "Electric",
            R.id.cat_handyman to "Handyman",
            R.id.cat_carpentry to "Carpentry",
            R.id.cat_mechanic to "Mechanic",
            R.id.cat_hvac to "HVAC"
        )
        for ((viewId, catName) in categoryMap) {
            findViewById<View>(viewId).setOnClickListener {
                startActivity(
                    Intent(this, CategoryDetailActivity::class.java)
                        .putExtra("category", catName)
                )
            }
        }

        HomeBottomNavHelper.setup(this, HomeBottomNavHelper.TAB_REQUEST)
        loadSampleRequests()
    }

    override fun onResume() {
        super.onResume()
        loadSampleRequests()
    }

    private fun seedSampleData() {
        val prefs = getSharedPreferences("request_store", MODE_PRIVATE)
        if (prefs.getBoolean("seeded", false)) return
        RequestStore.addRequest(this, "Sisira Kumara", "Plumber", "Plumbing", "0771234567", "Colombo", "Pipe leak repair")
        RequestStore.addRequest(this, "Nimal Herath", "Painter", "Painting", "0789876543", "Kandy", "House repainting")
        RequestStore.addRequest(this, "Sunil Rathnayake", "Gardening", "Gardening", "0761112233", "Galle", "Weed removal")
        prefs.edit().putBoolean("seeded", true).apply()
    }

    private fun loadSampleRequests() {
        container.removeAllViews()

        seedSampleData()
        val requests = RequestStore.getAllRequests(this)

        if (requests.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No requests yet. Tap 'Requests' to create one."
                textSize = 14f
                setTextColor(0xFF777777.toInt())
                setPadding(0, 40, 0, 0)
                gravity = android.view.Gravity.CENTER
            }
            container.addView(empty)
            return
        }

        requests.forEachIndexed { index, req ->
            val item = LayoutInflater.from(this).inflate(R.layout.item_my_request, container, false)

            item.findViewById<TextView>(R.id.tv_number).text = "${index + 1}."
            item.findViewById<TextView>(R.id.tv_title).text = req.title.ifBlank { "Request" }
            item.findViewById<TextView>(R.id.tv_description).text = req.description.ifBlank { req.category }

            item.findViewById<View>(R.id.btn_edit).setOnClickListener {
                startActivity(
                    Intent(this, CreateRequestActivity::class.java)
                        .putExtra("request_id", req.id)
                )
            }

            item.findViewById<ImageView>(R.id.btn_delete).setOnClickListener {
                RequestStore.deleteRequest(this, req.id)
                Toast.makeText(this, "Request deleted", Toast.LENGTH_SHORT).show()
                loadSampleRequests()
            }

            container.addView(item)
        }
    }
}
