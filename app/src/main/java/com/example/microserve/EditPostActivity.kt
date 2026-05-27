package com.example.microserve

import android.view.View
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImageContract
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityEditPostBinding

class EditPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditPostBinding
    private var serviceId: String = ""
    private var selectedImagePath: String? = null
    private var imageChanged = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            cropImage.launch(PostImagePicker.optionsForGalleryUri(this, uri))
        }
    }

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            applyCroppedImage(result.uriContent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityEditPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupSpinner()
        prefillExistingData()
        setupClickListeners()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupSpinner() {
        val categories = arrayOf("-Select-", "Plumbing", "Electrical", "House Painting", "Carpentry", "Cleaning")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter
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
            PostImageHelper.loadPostImage(binding.selectedImagePreview, imagePath)
            binding.addImagePlaceholder.visibility = View.GONE
            binding.tvImageChangeHint.visibility = View.VISIBLE
            binding.imageActionText.text = getString(R.string.post_ads_image_selected)
        }

        val categoryPosition = (0 until binding.categorySpinner.count)
            .firstOrNull { binding.categorySpinner.getItemAtPosition(it) == defaultCategory }
            ?: 0
        binding.categorySpinner.setSelection(categoryPosition)
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.addImageBtn.setOnClickListener {
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
            val category = binding.categorySpinner.selectedItem.toString()
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
        val category = binding.categorySpinner.selectedItem.toString()
        val name = binding.providerNameET.text.toString().trim()
        val location = binding.locationET.text.toString().trim()
        val contact = binding.contactET.text.toString().trim()

        return when {
            category == "-Select-" -> {
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

    private fun applyCroppedImage(uri: Uri?) {
        if (uri == null) return
        val previous = selectedImagePath
        val savedPath = PostImageHelper.copyPickedImage(this, uri)
        if (savedPath == null) {
            showToast(getString(R.string.post_ads_image_save_failed))
            return
        }
        if (!previous.isNullOrBlank() && previous != savedPath) {
            PostImageHelper.deletePostImage(this, previous)
        }
        selectedImagePath = savedPath
        imageChanged = true
        PostImageHelper.loadPostImage(binding.selectedImagePreview, savedPath)
        binding.addImagePlaceholder.visibility = View.GONE
        binding.tvImageChangeHint.visibility = View.VISIBLE
        binding.imageActionText.text = getString(R.string.post_ads_image_selected)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
