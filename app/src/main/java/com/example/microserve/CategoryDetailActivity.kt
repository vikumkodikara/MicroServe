package com.example.microserve

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ListenerRegistration

class CategoryDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_CATEGORY_ID = "category_id"
    }

    private lateinit var chipsContainer: LinearLayout
    private lateinit var providersContainer: LinearLayout
    private lateinit var categoryTitle: TextView
    private lateinit var categoryIcon: ImageView
    private lateinit var currentCategory: CategoryCatalog.Category
    private var requestListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category_detail)

        chipsContainer = findViewById(R.id.chipsContainer)
        providersContainer = findViewById(R.id.providersContainer)
        categoryTitle = findViewById(R.id.tv_category_title)
        categoryIcon = findViewById(R.id.iv_category_icon)

        currentCategory = resolveInitialCategory()

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        bindCategoryHeader()
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

    private fun resolveInitialCategory(): CategoryCatalog.Category {
        val categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID)
        if (!categoryId.isNullOrBlank()) {
            CategoryCatalog.findById(categoryId)?.let { return it }
        }

        val categoryName = intent.getStringExtra(EXTRA_CATEGORY)
        if (!categoryName.isNullOrBlank()) {
            CategoryCatalog.findByStoreKey(categoryName)?.let { return it }
            CategoryCatalog.all.firstOrNull {
                it.displayName.equals(categoryName, ignoreCase = true)
            }?.let { return it }
        }

        return CategoryCatalog.all.first()
    }

    private fun bindCategoryHeader() {
        categoryTitle.text = chipLabel(currentCategory)
        categoryIcon.setImageResource(currentCategory.imageRes)
    }

    private fun buildChips() {
        chipsContainer.removeAllViews()

        for (category in CategoryCatalog.all) {
            val chip = TextView(this).apply {
                text = chipLabel(category)
                tag = category.id
                textSize = 13f
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                gravity = Gravity.CENTER

                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(dpToPx(4), 0, dpToPx(4), 0)
                layoutParams = lp

                setOnClickListener {
                    val selected = CategoryCatalog.findById(category.id) ?: return@setOnClickListener
                    if (selected.id != currentCategory.id) {
                        selectCategory(selected)
                    }
                }
            }
            chipsContainer.addView(chip)
        }

        updateChipStyles()
    }

    private fun selectCategory(category: CategoryCatalog.Category) {
        currentCategory = category
        bindCategoryHeader()
        updateChipStyles()
        listenRequests()
    }

    private fun updateChipStyles() {
        for (index in 0 until chipsContainer.childCount) {
            val chip = chipsContainer.getChildAt(index) as TextView
            val selected = chip.tag == currentCategory.id
            if (selected) {
                chip.setBackgroundResource(R.drawable.chip_selected_bg)
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setBackgroundResource(R.drawable.chip_unselected_bg)
                chip.setTextColor(Color.parseColor("#4a4458"))
            }
        }
    }

    private fun listenRequests() {
        requestListener?.remove()
        requestListener = ServiceRequestRepository.listenOpenByCategories(
            storeKeys = currentCategory.storeKeys,
            onUpdate = { requests ->
                if (isFinishing || isDestroyed) return@listenOpenByCategories
                renderRequests(requests)
            },
            onError = { message ->
                if (isFinishing || isDestroyed) return@listenOpenByCategories
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun renderRequests(requests: List<ServiceRequest>) {
        if (isFinishing || isDestroyed) return

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

    private fun chipLabel(category: CategoryCatalog.Category): String = when (category.id) {
        "electric" -> "Electric"
        "painting" -> "Painting"
        else -> category.storeKeys.first()
    }

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
}
