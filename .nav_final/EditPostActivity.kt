package com.example.microserve

import android.net.Uri
import android.os.Bundle
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
    private var selectedImageUri: Uri? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.selectedImagePreview.setImageURI(uri)
            binding.imageActionText.text = "Image selected"
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
        val defaultCategory = intent.getStringExtra("category") ?: "Plumbing"
        val defaultName = intent.getStringExtra("provider_name") ?: "Sunil Perera"
        val defaultLocation = intent.getStringExtra("location") ?: "Galle"
        val defaultContact = intent.getStringExtra("contact") ?: "072587456"

        binding.providerNameET.setText(defaultName)
        binding.locationET.setText(defaultLocation)
        binding.contactET.setText(defaultContact)

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
            imagePicker.launch("image/*")
        }

        binding.deleteBtn.setOnClickListener {
            showToast("Post deleted")
            finish()
        }

        binding.saveBtn.setOnClickListener {
            if (validateFields()) {
                showToast("Post updated successfully")
                finish()
            }
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
