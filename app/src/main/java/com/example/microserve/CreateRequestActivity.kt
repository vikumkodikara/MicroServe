package com.example.microserve

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class CreateRequestActivity : AppCompatActivity() {

    private var editingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_request)

        val etName = findViewById<EditText>(R.id.et_name)
        val etTitle = findViewById<EditText>(R.id.et_title)
        val spinner = findViewById<Spinner>(R.id.spinner_category)
        val etContact = findViewById<EditText>(R.id.et_contact)
        val etLocation = findViewById<EditText>(R.id.et_location)
        val etDescription = findViewById<EditText>(R.id.et_description)

        val categories = arrayOf("-Select-", "Plumbing", "Gardening", "Cleaning", "Painting", "Electric", "Handyman", "Carpentry", "Mechanic", "HVAC")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        editingId = intent.getStringExtra("request_id")
        if (editingId != null) {
            val req = RequestStore.getAllRequests(this).find { it.id == editingId }
            if (req != null) {
                etName.setText(req.name)
                etTitle.setText(req.title)
                etContact.setText(req.contact)
                etLocation.setText(req.location)
                etDescription.setText(req.description)
                val catIndex = categories.indexOf(req.category)
                if (catIndex >= 0) spinner.setSelection(catIndex)
            }
        }

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.btn_post).setOnClickListener {
            val name = etName.text.toString().trim()
            val title = etTitle.text.toString().trim()
            val category = spinner.selectedItem?.toString() ?: ""
            val contact = etContact.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val description = etDescription.text.toString().trim()

            if (name.isBlank() || title.isBlank()) {
                Toast.makeText(this, "Please fill in name and title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (editingId != null) {
                RequestStore.updateRequest(this, editingId!!, name, title, category, contact, location, description)
                Toast.makeText(this, "Request updated", Toast.LENGTH_SHORT).show()
            } else {
                RequestStore.addRequest(this, name, title, category, contact, location, description)
                Toast.makeText(this, "Request posted", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
