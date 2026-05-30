package com.example.microserve

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ServiceRequestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.statusBarColor = Color.TRANSPARENT
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.statusBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_service_request)

        // Provider info passed from ProfileActivity
        val providerName = intent.getStringExtra("PROVIDER_NAME") ?: "Unknown Provider"
        val providerUid  = intent.getStringExtra("PROVIDER_UID")  ?: ""
        val category     = intent.getStringExtra("CATEGORY")      ?: "Service"

        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val etName:        EditText = findViewById(R.id.etName)
        val etLocation:    EditText = findViewById(R.id.etLocation)
        val etDate:        EditText = findViewById(R.id.etDate)
        val etDescription: EditText = findViewById(R.id.etDescription)
        val btnSend:       Button   = findViewById(R.id.btnSend)

        // Pre-fill name from session
        etName.setText(AppPreferences.getSessionName(this))

        btnSend.setOnClickListener {
            val name        = etName.text.toString().trim()
            val location    = etLocation.text.toString().trim()
            val description = etDescription.text.toString().trim()

            if (name.isBlank()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (description.isBlank()) {
                Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (providerUid.isBlank()) {
                Toast.makeText(this, "Provider information missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            val request = ServiceRequest(
                requesterUid          = uid,
                requesterName         = name,
                title                 = category,
                category              = category,
                location              = location,
                description           = description,
                status                = ServiceRequestStatus.OPEN,
                // Store the provider UID so the provider sees it in My Orders
                acceptedProviderUid   = providerUid,
                acceptedProviderName  = providerName
            )

            btnSend.isEnabled = false
            ServiceRequestRepository.create(
                request   = request,
                onSuccess = { saved ->
                    Toast.makeText(this, "Request sent to $providerName!", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onFailure = { error ->
                    btnSend.isEnabled = true
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
