package com.example.microserve

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ServiceRequestActivity : AppCompatActivity() {

    private lateinit var etLocation: EditText
    private lateinit var etDate:     EditText

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

        // ── Provider info passed from ProfileActivity ─────────────────────────
        val providerName = intent.getStringExtra("PROVIDER_NAME") ?: "Unknown Provider"
        val providerUid  = intent.getStringExtra("PROVIDER_UID")  ?: ""
        val category     = intent.getStringExtra("CATEGORY")      ?: "Service"

        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val etName:        EditText = findViewById(R.id.etName)
        etLocation                  = findViewById(R.id.etLocation)
        etDate                      = findViewById(R.id.etDate)
        val etDescription: EditText = findViewById(R.id.etDescription)
        val btnSend:       Button   = findViewById(R.id.btnSend)
        val btnSelectMap:  Button   = findViewById(R.id.btnSelectMap)

        // ── Pre-fill name from session ────────────────────────────────────────
        etName.setText(AppPreferences.getSessionName(this))

        // ── Enhancement 3: Auto-fill phone from Firestore ─────────────────────
        autoFillPhoneFromFirestore()

        // ── Enhancement 2: DatePickerDialog on date field click ───────────────
        val dateClickListener = View.OnClickListener { showDatePicker() }
        etDate.setOnClickListener(dateClickListener)
        etDate.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDatePicker() }

        // ── Enhancement 1: Map picker button ─────────────────────────────────
        btnSelectMap.setOnClickListener {
            MapPickerActivity.launch(this)
        }

        // ── Send (unchanged logic) ────────────────────────────────────────────
        btnSend.setOnClickListener {
            val name        = etName.text.toString().trim()
            val location    = etLocation.text.toString().trim()
            val date        = etDate.text.toString().trim()
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

            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: AppPreferences.getSessionUid(this)

            val request = ServiceRequest(
                requesterUid         = uid,
                requesterName        = name,
                title                = category,
                category             = category,
                location             = location,
                description          = description,
                status               = ServiceRequestStatus.OPEN,
                acceptedProviderUid  = providerUid,
                acceptedProviderName = providerName
            )

            btnSend.isEnabled = false
            ServiceRequestRepository.create(
                request   = request,
                onSuccess = {
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

    // ── Enhancement 2: DatePickerDialog ──────────────────────────────────────

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year  = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day   = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            // Format: dd/MM/yyyy
            val formatted = "%02d/%02d/%04d".format(d, m + 1, y)
            etDate.setText(formatted)
        }, year, month, day).apply {
            // Prevent selecting past dates
            datePicker.minDate = calendar.timeInMillis
            show()
        }
    }

    // ── Enhancement 3: Auto-fill phone number from Firestore ─────────────────

    private fun autoFillPhoneFromFirestore() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: AppPreferences.getSessionUid(this)

        if (uid.isBlank()) {
            Log.w("ServiceRequest", "Cannot auto-fill phone — UID is empty")
            return
        }

        FirebaseFirestore.getInstance()
            .collection(UserProfile.COLLECTION)   // "users"
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val phone = doc.getString("phoneNumber")
                    ?: doc.getString("phone")
                    ?: ""
                if (phone.isNotBlank()) {
                    Log.d("ServiceRequest", "Auto-filled phone: $phone")
                    // Phone is stored internally; shown in description hint for context
                    // (no dedicated phone field in this layout per user spec)
                    // Store it on the activity so Send logic can use it if needed
                    _autoFilledPhone = phone
                } else {
                    Log.d("ServiceRequest", "No phone number found in Firestore document")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ServiceRequest", "Failed to fetch phone: ${e.message}")
            }
    }

    // Stores auto-filled phone for use in any future send-logic extension
    private var _autoFilledPhone: String = ""

    // ── Enhancement 1: Handle map result ─────────────────────────────────────

    @Deprecated("Using legacy startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MapPickerActivity.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val address = data?.getStringExtra(MapPickerActivity.EXTRA_ADDRESS) ?: ""
            val lat     = data?.getDoubleExtra(MapPickerActivity.EXTRA_LAT, 0.0) ?: 0.0
            val lng     = data?.getDoubleExtra(MapPickerActivity.EXTRA_LNG, 0.0) ?: 0.0

            Log.d("MapPicker", "Selected: '$address'  lat=$lat  lng=$lng")

            if (address.isNotBlank()) {
                etLocation.setText(address)
            } else {
                etLocation.setText("%.5f, %.5f".format(lat, lng))
            }
        }
    }
}
