package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityCategoryDetailBinding
import com.example.microserve.databinding.ItemCategoryRequestBinding

class CategoryDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY_ID = "extra_category_id"
    }

    private lateinit var binding: ActivityCategoryDetailBinding
    private var currentCategory: CategoryCatalog.Category = CategoryCatalog.all.first()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCategoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentCategory = resolveInitialCategory()
        setupWindowInsets()
        setupClickListeners()
        setupCategoryChips()
        loadCategory(currentCategory)
    }

    override fun onResume() {
        super.onResume()
        refreshRequestList()
    }

    private fun resolveInitialCategory(): CategoryCatalog.Category {
        val categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID)
        return CategoryCatalog.findById(categoryId.orEmpty()) ?: CategoryCatalog.all.first()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentScrollView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            binding.footerBar.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener { finish() }
    }

    private fun setupCategoryChips() {
        binding.chipContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        CategoryCatalog.all.forEach { category ->
            val chipView = inflater.inflate(R.layout.item_category_chip, binding.chipContainer, false)
            val chipText = chipView.findViewById<TextView>(R.id.chipText)
            chipText.text = category.displayName
            chipText.tag = category.id

            chipText.setOnClickListener {
                if (currentCategory.id != category.id) {
                    loadCategory(category)
                }
            }

            binding.chipContainer.addView(chipView)
        }
    }

    private fun loadCategory(category: CategoryCatalog.Category) {
        currentCategory = category
        CategorySampleData.seedIfEmpty(this, category)
        updateHeader(category)
        updateChipSelection(category.id)
        refreshRequestList()
    }

    private fun updateHeader(category: CategoryCatalog.Category) {
        binding.categoryTitleText.text = category.displayName
        binding.categoryHeaderImage.setImageResource(category.imageRes)
    }

    private fun updateChipSelection(selectedId: String) {
        for (index in 0 until binding.chipContainer.childCount) {
            val chipText = binding.chipContainer.getChildAt(index).findViewById<TextView>(R.id.chipText)
            val isSelected = chipText.tag == selectedId
            chipText.setBackgroundResource(
                if (isSelected) R.drawable.category_chip_selected else R.drawable.category_chip_default
            )
            chipText.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isSelected) R.color.white else R.color.black
                )
            )
        }
    }

    private fun refreshRequestList() {
        val requests = RequestStore.getPendingRequestsByCategory(this, currentCategory.storeKeys)
        binding.requestsContainer.removeAllViews()

        if (requests.isEmpty()) {
            binding.emptyRequestsText.visibility = View.VISIBLE
            return
        }

        binding.emptyRequestsText.visibility = View.GONE
        val inflater = LayoutInflater.from(this)

        requests.forEach { request ->
            val rowBinding = ItemCategoryRequestBinding.inflate(inflater, binding.requestsContainer, false)
            rowBinding.requestName.text = request.requesterName
            rowBinding.requestDescription.text = getString(
                R.string.description_label_format,
                request.description.ifBlank { request.title }
            )
            rowBinding.root.setOnClickListener {
                startActivity(
                    Intent(this, RequestDetailActivity::class.java)
                        .putExtra(RequestDetailActivity.EXTRA_REQUEST_ID, request.id)
                )
            }
            binding.requestsContainer.addView(rowBinding.root)
        }
    }
}
