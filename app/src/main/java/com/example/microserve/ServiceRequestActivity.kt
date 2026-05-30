package com.example.microserve

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
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

    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var spinnerProvince: Spinner
    private lateinit var spinnerDistrict: Spinner
    private lateinit var spinnerCity:     Spinner
    private lateinit var etLocation:      EditText   // GPS/map-override field
    private lateinit var etDate:          EditText
    private var _autoFilledPhone: String = ""

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    // ── Same location data as PostServiceActivity ─────────────────────────────
    private val locationData = mapOf(
        "Western" to mapOf(
            "Colombo"  to listOf("Colombo 01","Colombo 02","Dehiwala","Moratuwa","Maharagama"),
            "Gampaha"  to listOf("Gampaha","Negombo","Kelaniya","Kadawatha"),
            "Kalutara" to listOf("Kalutara","Panadura","Horana","Matugama")
        ),
        "Central" to mapOf(
            "Kandy"       to listOf("Kandy","Peradeniya","Katugastota","Gampola"),
            "Matale"      to listOf("Matale","Dambulla","Sigiriya"),
            "Nuwara Eliya" to listOf("Nuwara Eliya","Hatton","Talawakelle")
        ),
        "Southern" to mapOf(
            "Galle"      to listOf("Galle","Hikkaduwa","Ambalangoda","Elpitiya"),
            "Matara"     to listOf("Matara","Weligama","Dickwella","Akuressa"),
            "Hambantota" to listOf("Hambantota","Tangalle","Beliatta","Ambalantota")
        ),
        "Northern" to mapOf(
            "Jaffna"     to listOf("Jaffna","Chavakachcheri","Point Pedro","Nallur"),
            "Kilinochchi" to listOf("Kilinochchi","Pallai","Paranthan"),
            "Mannar"     to listOf("Mannar","Murunkan","Pesalai"),
            "Mullaitivu" to listOf("Mullaitivu","Puthukkudiyiruppu","Oddusuddan"),
            "Vavuniya"   to listOf("Vavuniya","Cheddikulam","Omanthai")
        ),
        "Eastern" to mapOf(
            "Trincomalee" to listOf("Trincomalee","Kinniya","Mutur"),
            "Batticaloa"  to listOf("Batticaloa","Kattankudy","Eravur"),
            "Ampara"      to listOf("Ampara","Kalmunai","Akkaraipattu")
        ),
        "North Western" to mapOf(
            "Kurunegala" to listOf("Kurunegala","Kuliyapitiya","Polgahawela","Narammala"),
            "Puttalam"   to listOf("Puttalam","Chilaw","Wennappuwa")
        ),
        "North Central" to mapOf(
            "Anuradhapura" to listOf("Anuradhapura","Kekirawa","Tambuttegama","Eppawala"),
            "Polonnaruwa"  to listOf("Polonnaruwa","Kaduruwela","Medirigiriya")
        ),
        "Uva" to mapOf(
            "Badulla"    to listOf("Badulla","Bandarawela","Haputale","Mahiyanganaya"),
            "Monaragala" to listOf("Monaragala","Wellawaya","Bibile","Kataragama")
        ),
        "Sabaragamuwa" to mapOf(
            "Ratnapura" to listOf("Ratnapura","Balangoda","Pelmadulla","Embilipitiya"),
            "Kegalle"   to listOf("Kegalle","Mawanella","Warakapola","Rambukkana")
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
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

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val etName:        EditText = findViewById(R.id.etName)
        spinnerProvince             = findViewById(R.id.spinnerProvince)
        spinnerDistrict             = findViewById(R.id.spinnerDistrict)
        spinnerCity                 = findViewById(R.id.spinnerCity)
        etLocation                  = findViewById(R.id.etLocation)
        etDate                      = findViewById(R.id.etDate)
        val etDescription: EditText = findViewById(R.id.etDescription)
        val btnSend:       Button   = findViewById(R.id.btnSend)
        val btnSelectMap:  Button   = findViewById(R.id.btnSelectMap)

        // ── Pre-fill name ─────────────────────────────────────────────────────
        etName.setText(AppPreferences.getSessionName(this))

        // ── Province / District / City spinners (copied from PostServiceActivity) ─
        setupLocationSpinners()

        // ── Auto-detect GPS → pre-select spinners ─────────────────────────────
        autoDetectCurrentLocation()

        // ── Phone from Firestore ──────────────────────────────────────────────
        autoFillPhoneFromFirestore()

        // ── DatePickerDialog ──────────────────────────────────────────────────
        etDate.setOnClickListener      { showDatePicker() }
        etDate.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDatePicker() }

        // ── Map button → MapPickerActivity ────────────────────────────────────
        btnSelectMap.setOnClickListener { MapPickerActivity.launch(this) }

        // ── Send (location now assembled from spinners) ────────────────────────
        btnSend.setOnClickListener {
            val name        = etName.text.toString().trim()
            val description = etDescription.text.toString().trim()

            // Assemble location from spinners (same as PostServiceActivity's Send logic)
            val province = spinnerProvince.selectedItem?.toString() ?: ""
            val district = spinnerDistrict.selectedItem?.toString() ?: ""
            val city     = spinnerCity.selectedItem?.toString()     ?: ""

            // If the user picked via map button, etLocation has an override
            val mapOverride = etLocation.text.toString().trim()

            val location = when {
                mapOverride.isNotBlank() -> mapOverride
                province.isBlank() || province.startsWith("-") -> ""
                district.isBlank() || district.startsWith("-") -> province
                city.isBlank()     || city.startsWith("-")     -> "$district, $province"
                else                                            -> "$city, $district, $province"
            }

            // Validation
            if (name.isBlank()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (description.isBlank()) {
                Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (location.isBlank()) {
                Toast.makeText(this, "Please select a province, district, and city", Toast.LENGTH_SHORT).show()
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
                location             = location,          // "City, District, Province"
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
    // Province → District → City cascading spinners (exact logic from PostServiceActivity)
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupLocationSpinners() {
        val provinces = listOf("-Select Province-") + locationData.keys.toList()

        val provinceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, provinces)
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProvince.adapter = provinceAdapter

        spinnerProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = provinces[position]
                if (selected == "-Select Province-") {
                    updateDistrictSpinner(emptyList())
                } else {
                    val districts = locationData[selected]?.keys?.toList() ?: emptyList()
                    updateDistrictSpinner(listOf("-Select District-") + districts)
                }
                // Clear map override hint when user manually changes province
                if (etLocation.text.isNullOrBlank().not()) etLocation.text = null
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        updateDistrictSpinner(emptyList())
    }

    private fun updateDistrictSpinner(districts: List<String>) {
        val list = districts.ifEmpty { listOf("-Select District-") }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, list)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDistrict.adapter = adapter

        spinnerDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (list.isEmpty() || list[position].startsWith("-")) {
                    updateCitySpinner(emptyList())
                    return
                }
                val selectedProvince = spinnerProvince.selectedItem.toString()
                val selectedDistrict = list[position]
                val cities = locationData[selectedProvince]?.get(selectedDistrict) ?: emptyList()
                updateCitySpinner(listOf("-Select City-") + cities)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        updateCitySpinner(emptyList())
    }

    private fun updateCitySpinner(cities: List<String>) {
        val list = cities.ifEmpty { listOf("-Select City-") }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, list)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCity.adapter = adapter
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GPS auto-detect via FusedLocationProviderClient
    // ─────────────────────────────────────────────────────────────────────────

    private fun autoDetectCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }
        fetchFusedLocation()
    }

    private fun fetchFusedLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val cts = CancellationTokenSource()
        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    // Try to auto-select the nearest city in the spinners
                    val address = reverseGeocode(location.latitude, location.longitude)
                    if (!address.isNullOrBlank()) {
                        // Show the resolved address in etLocation as a fallback label
                        etLocation.setText(address)
                        Log.d("LocationDebug", "GPS resolved: $address")
                    }
                } else {
                    LocationServices.getFusedLocationProviderClient(this).lastLocation
                        .addOnSuccessListener { last ->
                            if (last != null) {
                                val address = reverseGeocode(last.latitude, last.longitude)
                                if (!address.isNullOrBlank()) etLocation.setText(address)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("LocationDebug", "GPS error: ${e.message}")
            }
    }

    private fun reverseGeocode(lat: Double, lng: Double): String? = try {
        Geocoder(this, Locale.getDefault()).getFromLocation(lat, lng, 1)
            ?.firstOrNull()
            ?.let { addr ->
                buildString {
                    addr.subLocality?.let { append("$it, ") }
                    addr.locality?.let    { append(it) }
                    if (isEmpty()) addr.adminArea?.let { append(it) }
                }.trimEnd(',', ' ').ifBlank { null }
            }
    } catch (e: Exception) { null }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) fetchFusedLocation()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DatePickerDialog
    // ─────────────────────────────────────────────────────────────────────────

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            etDate.setText("%02d/%02d/%04d".format(d, m + 1, y))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            .apply { datePicker.minDate = cal.timeInMillis; show() }
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
                _autoFilledPhone = doc.getString("phoneNumber") ?: doc.getString("phone") ?: ""
            }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Map picker result
    // ─────────────────────────────────────────────────────────────────────────

    @Deprecated("Using legacy startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MapPickerActivity.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val address = data?.getStringExtra(MapPickerActivity.EXTRA_ADDRESS) ?: ""
            val lat     = data?.getDoubleExtra(MapPickerActivity.EXTRA_LAT, 0.0) ?: 0.0
            val lng     = data?.getDoubleExtra(MapPickerActivity.EXTRA_LNG, 0.0) ?: 0.0
            etLocation.setText(address.ifBlank { "%.5f, %.5f".format(lat, lng) })
            Log.d("MapPicker", "Address set: '$address'")
        }
    }
}
