package com.example.microserve

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityEditPostBinding

class EditPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditPostBinding
    private lateinit var imageAdjuster: PostImageAdjuster
    private var serviceId: String = ""
    private var selectedImagePath: String? = null
    private var imageChanged = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageAdjuster.startAdjust(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityEditPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupImageAdjuster()
        setupSpinner()
        prefillExistingData()
        setupClickListeners()
    }

    private fun setupImageAdjuster() {
        imageAdjuster = PostImageAdjuster(
            activity = this,
            imageView = binding.selectedImagePreview,
            placeholder = binding.addImagePlaceholder,
            adjustControls = binding.imageAdjustControls,
            confirmButton = binding.btnConfirmImageAdjust,
            cancelButton = binding.btnCancelImageAdjust,
            changeHint = binding.tvImageChangeHint,
            adjustHint = binding.tvAdjustImageHint,
            imageContainer = binding.addImageBtn,
            scrollParent = binding.editPostScrollView,
            onImageSaved = { savedPath -> applySavedImage(savedPath) }
        )
    }

    private fun applyWindowInsets() {
        SystemUiHelper.setupPurpleHeaderScreen(
            activity = this,
            root = binding.root,
            headerView = binding.headerContainer,
            footerBar = binding.footerBar
        )
    }

    private fun setupSpinner() {
        ServiceCategorySpinnerAdapter.attach(binding.categorySpinner, this)
    }

    private fun prefillExistingData() {
        serviceId = intent.getStringExtra(ServiceStore.EXTRA_SERVICE_ID).orEmpty()
        val service = if (serviceId.isNotBlank()) {
            ServiceStore.getServiceById(this, serviceId)
        } else {
            null
        }

        val defaultCategory = service?.category ?: intent.getStringExtra("category") ?: "Plumbing"
        val defaultName = service?.providerName ?: intent.getStringExtra("provider_name") ?: ""
        val defaultLocation = service?.location ?: intent.getStringExtra("location") ?: ""
        val defaultContact = service?.contact ?: intent.getStringExtra("contact") ?: ""

        binding.providerNameET.setText(defaultName)
        binding.locationET.setText(defaultLocation)
        binding.contactET.setText(defaultContact)

        service?.imageUri?.takeIf { it.isNotBlank() }?.let { imagePath ->
            selectedImagePath = imagePath
            imageAdjuster.showSavedPreview(imagePath)
            binding.imageActionText.text = getString(R.string.post_ads_image_selected)
        }

        val categoryPosition = ServiceCategorySpinnerAdapter.indexForStoreKey(
            binding.categorySpinner,
            defaultCategory
        )
        binding.categorySpinner.setSelection(categoryPosition)
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.addImageBtn.setOnClickListener {
            if (binding.imageAdjustControls.visibility == View.VISIBLE) return@setOnClickListener
            pickImage.launch("image/*")
        }

        binding.deleteBtn.setOnClickListener {
            if (serviceId.isNotBlank()) {
                ServiceStore.deleteService(this, serviceId)
            }
            showToast(getString(R.string.post_ads_deleted))
            finish()
        }

        binding.saveBtn.setOnClickListener {
            if (!validateFields()) return@setOnClickListener
            if (serviceId.isBlank()) {
                showToast(getString(R.string.post_ads_updated))
                finish()
                return@setOnClickListener
            }
            val category = ServiceCategorySpinnerAdapter.selectedStoreKey(binding.categorySpinner).orEmpty()
            val name = binding.providerNameET.text.toString().trim()
            val location = binding.locationET.text.toString().trim()
            val contact = binding.contactET.text.toString().trim()
            ServiceStore.updateService(
                context = this,
                serviceId = serviceId,
                category = category,
                providerName = name,
                contact = contact,
                location = location,
                imageUri = selectedImagePath,
                replaceImage = imageChanged
            )
            showToast(getString(R.string.post_ads_updated))
            finish()
        }
    }

    private fun validateFields(): Boolean {
        val category = ServiceCategorySpinnerAdapter.selectedStoreKey(binding.categorySpinner)
        val name = binding.providerNameET.text.toString().trim()
        val location = binding.locationET.text.toString().trim()
        val contact = binding.contactET.text.toString().trim()

        return when {
            category.isNullOrBlank() -> {
                showToast("Please select a category")
                false
            }
            name.isEmpty() || location.isEmpty() || contact.isEmpty() -> {
                showToast("Please fill all fields")
                false
            }
            else -> true
        }
    }

    private fun applySavedImage(savedPath: String) {
        val previous = selectedImagePath
        if (!previous.isNullOrBlank() && previous != savedPath) {
            PostImageHelper.deletePostImage(this, previous)
        }
        selectedImagePath = savedPath
        imageChanged = true
        binding.imageActionText.text = getString(R.string.post_ads_image_selected)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
