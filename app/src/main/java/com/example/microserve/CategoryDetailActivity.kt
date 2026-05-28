package com.example.microserve

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ListenerRegistration

class CategoryDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY = "category"
    }

    private lateinit var chipsContainer: LinearLayout
    private lateinit var providersContainer: LinearLayout
    private var currentCategory: String = ""
    private var requestListener: ListenerRegistration? = null

    private val allCategories = listOf(
        "Plumbing", "Cleaning", "Gardening", "Painting", "Electric", "Handyman",
        "Carpentry", "Mechanic", "HVAC"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category_detail)

        chipsContainer = findViewById(R.id.chipsContainer)
        providersContainer = findViewById(R.id.providersContainer)

        currentCategory = intent.getStringExtra(EXTRA_CATEGORY) ?: allCategories.first()

        findViewById<TextView>(R.id.tv_category_title).text = currentCategory
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        buildChips()
    }

    override fun onStart() {
        super.onStart()
        listenRequests()
    }

    override fun onStop() {
        requestListener?.remove()
        requestListener = null
        super.onStop()
    }

    private fun buildChips() {
        chipsContainer.removeAllViews()

        for (cat in allCategories) {
            val chip = TextView(this).apply {
                text = cat
                textSize = 13f
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                gravity = Gravity.CENTER

                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(dpToPx(4), 0, dpToPx(4), 0)
                layoutParams = lp

                if (cat == currentCategory) {
                    setBackgroundResource(R.drawable.chip_selected_bg)
                    setTextColor(Color.WHITE)
                } else {
                    setBackgroundResource(R.drawable.chip_unselected_bg)
                    setTextColor(Color.parseColor("#4a4458"))
                }

                setOnClickListener {
                    currentCategory = cat
                    findViewById<TextView>(R.id.tv_category_title).text = cat
                    buildChips()
                    listenRequests()
                }
            }
            chipsContainer.addView(chip)
        }
    }

    private fun listenRequests() {
        requestListener?.remove()
        val storeKeys = CategoryCatalog.findByStoreKey(currentCategory)?.storeKeys ?: listOf(currentCategory)
        requestListener = ServiceRequestRepository.listenOpenByCategories(
            storeKeys = storeKeys,
            onUpdate = { requests -> renderRequests(requests) },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun renderRequests(requests: List<ServiceRequest>) {
        providersContainer.removeAllViews()

        if (requests.isEmpty()) {
            val empty = TextView(this).apply {
                text = getString(R.string.no_open_requests_category)
                textSize = 14f
                setTextColor(0xFF777777.toInt())
                setPadding(0, dpToPx(30), 0, 0)
                gravity = Gravity.CENTER
            }
            providersContainer.addView(empty)
            return
        }

        for (request in requests) {
            val item = LayoutInflater.from(this)
                .inflate(R.layout.item_service_provider, providersContainer, false)
            item.findViewById<TextView>(R.id.tv_provider_name).text = request.title
            item.findViewById<TextView>(R.id.tv_provider_desc).text =
                getString(R.string.request_list_subtitle, request.requesterName, request.city)
            item.setOnClickListener {
                startActivity(
                    Intent(this, RequestDetailActivity::class.java)
                        .putExtra(RequestDetailActivity.EXTRA_REQUEST_ID, request.id)
                )
            }
            providersContainer.addView(item)
        }
    }

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
}
