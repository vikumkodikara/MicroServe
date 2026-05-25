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
import com.example.microserve.databinding.ActivityRequestMainBinding
import com.example.microserve.databinding.ItemCategoryBinding
import com.example.microserve.databinding.ItemRequestRowBinding

class RequestMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRequestMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRequestMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        seedSampleRequestsIfEmpty()
        setupCategoryGrid()
        setupClickListeners()
        UserBottomNavHelper.setup(this, UserBottomNavHelper.TAB_REQUEST)
        refreshRequestsList()
    }

    override fun onResume() {
        super.onResume()
        refreshRequestsList()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainScrollView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            findViewById<View>(R.id.navContainer)?.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun seedSampleRequestsIfEmpty() {
        if (RequestStore.getPendingRequests(this).isNotEmpty()) return

        RequestStore.addRequest(
            context = this,
            category = "Plumbing",
            requesterName = "Demo User",
            contact = "555-0100",
            location = "Home",
            title = "Pipe leak repair",
            description = "Kitchen pipe leak repair"
        )
        RequestStore.addRequest(
            context = this,
            category = "House Painting",
            requesterName = "Demo User",
            contact = "555-0100",
            location = "Home",
            title = "House repainting",
            description = "Full house repainting"
        )
        RequestStore.addRequest(
            context = this,
            category = "Gardening",
            requesterName = "Demo User",
            contact = "555-0100",
            location = "Garden",
            title = "Weed removal",
            description = "Backyard weed removal"
        )
    }

    private fun setupCategoryGrid() {
        binding.categoryGrid.removeAllViews()
        val inflater = LayoutInflater.from(this)

        CategoryCatalog.all.forEach { category ->
            val itemBinding = ItemCategoryBinding.inflate(inflater, binding.categoryGrid, false)
            itemBinding.categoryImage.setImageResource(category.imageRes)
            itemBinding.categoryLabel.text = category.displayName
            itemBinding.root.setOnClickListener {
                openCategoryDetail(category.id)
            }
            binding.categoryGrid.addView(itemBinding.root)
        }
    }

    private fun refreshRequestsList() {
        val requests = RequestStore.getPendingRequests(this)
        binding.requestsContainer.removeAllViews()

        if (requests.isEmpty()) {
            binding.emptyRequestsText.visibility = View.VISIBLE
            return
        }

        binding.emptyRequestsText.visibility = View.GONE
        val inflater = LayoutInflater.from(this)

        requests.forEachIndexed { index, request ->
            val rowBinding = ItemRequestRowBinding.inflate(inflater, binding.requestsContainer, false)
            val categoryLabel = displayCategoryName(request.category)
            rowBinding.requestTitle.text = getString(R.string.request_row_title_format, index + 1, categoryLabel)
            rowBinding.requestDesc.text = request.title

            rowBinding.requestDelete.setOnClickListener {
                RequestStore.deleteRequest(this, request.id)
                Toast.makeText(this, "Request deleted", Toast.LENGTH_SHORT).show()
                refreshRequestsList()
            }

            rowBinding.requestEditBtn.setOnClickListener {
                openRequestForm(request.category)
            }

            binding.requestsContainer.addView(rowBinding.root)
        }
    }

    private fun displayCategoryName(category: String): String {
        return CategoryCatalog.findByStoreKey(category)?.displayName ?: when {
            category.contains("Plumb", ignoreCase = true) -> "Plumber"
            category.contains("Paint", ignoreCase = true) -> "Painter"
            category.contains("Garden", ignoreCase = true) -> "Gardening"
            category.contains("Clean", ignoreCase = true) -> "Cleaning"
            category.contains("Electric", ignoreCase = true) -> "Electric"
            category.contains("Carpent", ignoreCase = true) -> "Handyman"
            else -> category
        }
    }

    private fun setupClickListeners() {
        binding.headerIconContainer.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.requestsButton.setOnClickListener {
            binding.mainScrollView.smoothScrollTo(0, binding.myRequestsLabel.top)
        }
    }

    private fun openCategoryDetail(categoryId: String) {
        startActivity(
            Intent(this, CategoryDetailActivity::class.java)
                .putExtra(CategoryDetailActivity.EXTRA_CATEGORY_ID, categoryId)
        )
    }

    private fun openRequestForm(category: String) {
        startActivity(
            Intent(this, RequestServiceActivity::class.java)
                .putExtra(RequestServiceActivity.EXTRA_CATEGORY, category)
        )
    }
}
