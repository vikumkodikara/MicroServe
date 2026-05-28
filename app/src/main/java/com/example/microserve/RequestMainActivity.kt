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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class RequestMainActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private var requestListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_request_main)
        setupWindowInsets()

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
                openCategoryDetail(catName)
            }
        }

        HomeBottomNavHelper.setup(this, HomeBottomNavHelper.TAB_REQUEST)
    }

    private fun setupWindowInsets() {
        findViewById<View>(R.id.requestMainRoot).applyHorizontalSystemBarInsets()
        findViewById<View>(R.id.header_bg).applyStatusBarTopInset()
        applyNavBarSpacer(R.id.navSystemBarSpacer)
    }

    override fun onStart() {
        super.onStart()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            renderRequests(emptyList())
            return
        }
        requestListener?.remove()
        requestListener = ServiceRequestRepository.listenByRequester(
            requesterUid = uid,
            onUpdate = { requests -> renderRequests(requests) },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    override fun onStop() {
        requestListener?.remove()
        requestListener = null
        super.onStop()
    }

    private fun renderRequests(requests: List<ServiceRequest>) {
        container.removeAllViews()

        if (requests.isEmpty()) {
            val empty = TextView(this).apply {
                text = getString(R.string.no_requests_yet)
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
            item.findViewById<TextView>(R.id.tv_description).text =
                "${req.city} • ${req.status.replace('_', ' ')}"

            item.setOnClickListener {
                startActivity(
                    Intent(this, RequestDetailActivity::class.java)
                        .putExtra(RequestDetailActivity.EXTRA_REQUEST_ID, req.id)
                )
            }

            val editButton = item.findViewById<View>(R.id.btn_edit)
            val deleteButton = item.findViewById<ImageView>(R.id.btn_delete)
            if (req.status == ServiceRequestStatus.OPEN) {
                editButton.setOnClickListener {
                    startActivity(
                        Intent(this, CreateRequestActivity::class.java)
                            .putExtra(CreateRequestActivity.EXTRA_REQUEST_ID, req.id)
                    )
                }
                deleteButton.setOnClickListener {
                    ServiceRequestRepository.delete(
                        requestId = req.id,
                        onSuccess = {
                            Toast.makeText(this, R.string.request_deleted, Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { message ->
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                editButton.visibility = View.GONE
                deleteButton.visibility = View.GONE
            }

            container.addView(item)
        }
    }

    private fun openCategoryDetail(catName: String) {
        val category = CategoryCatalog.findByStoreKey(catName)
        startActivity(
            Intent(this, CategoryDetailActivity::class.java).apply {
                putExtra(CategoryDetailActivity.EXTRA_CATEGORY, catName)
                category?.id?.let { putExtra(CategoryDetailActivity.EXTRA_CATEGORY_ID, it) }
            }
        )
    }
}
