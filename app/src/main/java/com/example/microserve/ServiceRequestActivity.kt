package com.example.microserve

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

class ServiceRequestActivity : AppCompatActivity() {

    private lateinit var etLocation: EditText
    private lateinit var etDate:     EditText
    private var _autoFilledPhone:    String = ""

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

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

        // ── Provider info from previous screen ────────────────────────────────
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

        // ── Auto-fill phone from Firestore ────────────────────────────────────
        autoFillPhoneFromFirestore()

        // ── Auto-detect GPS location via FusedLocationProviderClient ─────────
        autoDetectCurrentLocation()

        // ── DatePickerDialog on date field tap ────────────────────────────────
        etDate.setOnClickListener      { showDatePicker() }
        etDate.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDatePicker() }

        // ── Manual map selection ──────────────────────────────────────────────
        btnSelectMap.setOnClickListener {
            MapPickerActivity.launch(this)
        }

        // ── Send (logic unchanged) ────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    // Enhancement: Auto-detect GPS location with FusedLocationProviderClient
    // ─────────────────────────────────────────────────────────────────────────

    private fun autoDetectCurrentLocation() {
        // Check permissions first
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permission — result handled in onRequestPermissionsResult
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        fetchFusedLocation()
    }

    private fun fetchFusedLocation() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        // Use getCurrentLocation for an accurate fresh fix
        val cts = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("LocationDebug", "GPS fix: lat=${location.latitude} lng=${location.longitude}")
                    val address = reverseGeocode(location.latitude, location.longitude)
                    if (!address.isNullOrBlank()) {
                        etLocation.setText(address)
                        Log.d("LocationDebug", "Auto-filled location: $address")
                    }
                } else {
                    // No fresh fix — fall back to last known location
                    Log.w("LocationDebug", "getCurrentLocation returned null, trying lastLocation")
                    fusedClient.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            val address = reverseGeocode(last.latitude, last.longitude)
                            if (!address.isNullOrBlank()) etLocation.setText(address)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("LocationDebug", "Location fetch failed: ${e.message}")
            }
    }

    /** Android Geocoder: converts lat/lng → readable address string. */
    private fun reverseGeocode(lat: Double, lng: Double): String? {
        return try {
            val geocoder   = Geocoder(this, Locale.getDefault())
            val addresses  = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                buildString {
                    addr.subLocality?.let    { append("$it, ") }
                    addr.locality?.let       { append("$it") }
                    if (isEmpty()) addr.adminArea?.let { append(it) }
                }.trimEnd(',', ' ').ifBlank { null }
            } else null
        } catch (e: Exception) {
            Log.e("LocationDebug", "Geocoder error: ${e.message}")
            null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission granted — now fetch location
            fetchFusedLocation()
        } else {
            Log.w("LocationDebug", "Location permission denied by user")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DatePickerDialog
    // ─────────────────────────────────────────────────────────────────────────

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                etDate.setText("%02d/%02d/%04d".format(day, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = calendar.timeInMillis   // block past dates
            show()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phone auto-fill from Firestore
    // ─────────────────────────────────────────────────────────────────────────

    private fun autoFillPhoneFromFirestore() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: AppPreferences.getSessionUid(this)

        if (uid.isBlank()) return

        FirebaseFirestore.getInstance()
            .collection(UserProfile.COLLECTION)
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                _autoFilledPhone = doc.getString("phoneNumber")
                    ?: doc.getString("phone")
                    ?: ""
                Log.d("ServiceRequest", "Auto-filled phone: '$_autoFilledPhone'")
            }
            .addOnFailureListener { e ->
                Log.e("ServiceRequest", "Phone fetch error: ${e.message}")
            }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Map picker result handler
    // ─────────────────────────────────────────────────────────────────────────

    @Deprecated("Using legacy startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MapPickerActivity.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val address = data?.getStringExtra(MapPickerActivity.EXTRA_ADDRESS) ?: ""
            val lat     = data?.getDoubleExtra(MapPickerActivity.EXTRA_LAT, 0.0) ?: 0.0
            val lng     = data?.getDoubleExtra(MapPickerActivity.EXTRA_LNG, 0.0) ?: 0.0

            Log.d("MapPicker", "Selected: '$address'  lat=$lat  lng=$lng")
            etLocation.setText(address.ifBlank { "%.5f, %.5f".format(lat, lng) })
        }
    }
}
