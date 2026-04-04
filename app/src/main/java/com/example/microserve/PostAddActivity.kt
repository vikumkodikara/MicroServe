package com.example.microserve

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.microserve.databinding.ActivityPostAddBinding

class PostAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostAddBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPostAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()
        setupClickListeners()
    }

    private fun setupSpinner() {
        val categories = arrayOf("-Select-", "Plumbing", "Electrical", "House Painting", "Carpentry", "Cleaning")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.addImageBtn.setOnClickListener {
            Toast.makeText(this, "Opening Gallery...", Toast.LENGTH_SHORT).show()
        }

        binding.postBtn.setOnClickListener {
            val name = binding.providerNameET.text.toString().trim()
            val location = binding.locationET.text.toString().trim()
            val contact = binding.contactET.text.toString().trim()
            val category = binding.categorySpinner.selectedItem.toString()

            if (category == "-Select-") {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            } else if (name.isEmpty() || location.isEmpty() || contact.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                // Create/register user if not exists
                val existingUser = UserStore.getAllUsers(this)
                    .firstOrNull { it.name.equals(name, ignoreCase = true) }
                
                if (existingUser == null) {
                    UserStore.addUser(
                        context = this,
                        name = name,
                        email = "provider_${System.currentTimeMillis()}@microserve.local",
                        phone = contact,
                        type = UserStore.TYPE_PROVIDER
                    )
                }

                // Add the service
                ServiceStore.addService(
                    context = this,
                    category = category,
                    providerName = name,
                    contact = contact,
                    location = location
                )
                Toast.makeText(this, "Service posted successfully!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}