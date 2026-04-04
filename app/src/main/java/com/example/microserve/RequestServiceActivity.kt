package com.example.microserve

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityRequestServiceBinding

class RequestServiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRequestServiceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRequestServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        binding.requestBtn.setOnClickListener {
            val name = binding.nameET.text.toString().trim()
            val title = binding.titleET.text.toString().trim()
            val category = binding.categorySpinner.selectedItem.toString()
            val contact = binding.contactET.text.toString().trim()
            val location = binding.locationET.text.toString().trim()
            val description = binding.descriptionET.text.toString().trim()

            if (category == "-Select-") {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            } else if (name.isEmpty() || title.isEmpty() || contact.isEmpty() || location.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Service Requested Successfully!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
