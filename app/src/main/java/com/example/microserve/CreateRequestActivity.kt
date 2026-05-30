package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Create / edit a service request. Location spinners follow the same pattern as [PostAdsActivity].
 */
class CreateRequestActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REQUEST_ID = "request_id"
        const val EXTRA_CATEGORY = "extra_category"
    }

    private var editingId: String? = null
    private lateinit var spinnerProvince: Spinner
    private lateinit var spinnerDistrict: Spinner
    private lateinit var spinnerCity: Spinner
    private lateinit var etLocationField: EditText
    private lateinit var btnPickLocation: MaterialButton
    private lateinit var postButton: MaterialButton

    private var suppressSpinnerCallbacks = false
    private var selectedLocation: SelectedLocation? = null
    private var isSubmitting = false

    private var selectedProvince = ""
    private var selectedDistrict = ""
    private var selectedCity = ""

    private lateinit var mapPicker: androidx.activity.result.ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_request)
        setupWindowInsets()

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val etName = findViewById<EditText>(R.id.et_name)
        val etTitle = findViewById<EditText>(R.id.et_title)
        val categorySpinner = findViewById<Spinner>(R.id.spinner_category)
        val etContact = findViewById<EditText>(R.id.et_contact)
        etLocationField = findViewById(R.id.et_location)
        val etDescription = findViewById<EditText>(R.id.et_description)
        postButton = findViewById(R.id.btn_post)
        btnPickLocation = findViewById(R.id.btn_pick_location)
        spinnerProvince = findViewById(R.id.spinner_province)
        spinnerDistrict = findViewById(R.id.spinner_district)
        spinnerCity = findViewById(R.id.spinner_city)

        val categories = arrayOf(
            "-Select-", "Plumbing", "Gardening", "Cleaning", "Painting",
            "Electric", "Handyman", "Carpentry", "Mechanic", "HVAC"
        )
        categorySpinner.adapter = spinnerAdapter(categories.toList())

        setupLocationSpinners()
        setupMapPicker()

        val preselectedCategory = intent.getStringExtra(EXTRA_CATEGORY)
        if (!preselectedCategory.isNullOrBlank()) {
            val index = categories.indexOfFirst { it.equals(preselectedCategory, ignoreCase = true) }
            if (index >= 0) categorySpinner.setSelection(index)
        }

        editingId = intent.getStringExtra(EXTRA_REQUEST_ID)?.takeIf { it.isNotBlank() }
        if (editingId != null) {
            ServiceRequestRepository.getById(
                requestId = editingId!!,
                onSuccess = { req ->
                    etName.setText(req.requesterName)
                    etTitle.setText(req.title)
                    etContact.setText(req.contact)
                    etDescription.setText(req.description)
                    val catIndex = categories.indexOf(req.category)
                    if (catIndex >= 0) categorySpinner.setSelection(catIndex)
                    selectedProvince = req.province
                    selectedDistrict = req.district
                    selectedCity = req.city
                    applyLocationToSpinners()
                    etLocationField.setText(req.location)
                    selectedLocation = SelectedLocation(
                        province = req.province,
                        district = req.district,
                        city = req.city,
                        address = req.location,
                        latitude = 6.9271,
                        longitude = 79.8612
                    )
                    updatePickMapButtonState()
                },
                onFailure = { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    finish()
                }
            )
        } else {
            val session = AppPreferences.getSessionProfile(this)
            if (etName.text.isNullOrBlank()) etName.setText(session.name)
            if (etContact.text.isNullOrBlank()) etContact.setText(session.phone)
        }

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        btnPickLocation.setOnClickListener { launchMapPicker() }

        postButton.setOnClickListener {
            if (isSubmitting) return@setOnClickListener
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            refreshLocationFromSpinners()

            val name = etName.text.toString().trim()
            val title = etTitle.text.toString().trim()
            val category = categorySpinner.selectedItem?.toString()?.trim().orEmpty()
            val contact = etContact.text.toString().trim()
            val streetOrPin = etLocationField.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val locationForDb = buildSavedLocationText(streetOrPin)

            when {
                name.isBlank() || title.isBlank() ->
                    Toast.makeText(this, "Please fill in name and title", Toast.LENGTH_SHORT).show()
                category.isBlank() || category == "-Select-" ->
                    Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                selectedProvince.isBlank() ->
                    Toast.makeText(this, "Please select a province", Toast.LENGTH_SHORT).show()
                selectedDistrict.isBlank() ->
                    Toast.makeText(this, "Please select a district", Toast.LENGTH_SHORT).show()
                selectedCity.isBlank() ->
                    Toast.makeText(this, "Please select a city", Toast.LENGTH_SHORT).show()
                streetOrPin.isBlank() ->
                    Toast.makeText(this, "Enter address or pick location on map", Toast.LENGTH_SHORT).show()
                else -> submitRequest(
                    uid = uid,
                    name = name,
                    title = title,
                    category = category,
                    contact = contact,
                    locationForDb = locationForDb,
                    description = description
                )
            }
        }
    }

    private fun setupMapPicker() {
        mapPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult
            val pin = SelectedLocation.fromIntent(data) ?: return@registerForActivityResult

            refreshLocationFromSpinners()

            selectedLocation = SelectedLocation(
                province = selectedProvince,
                district = selectedDistrict,
                city = selectedCity,
                address = pin.address,
                latitude = pin.latitude,
                longitude = pin.longitude
            )
            etLocationField.setText(pin.address)
            Toast.makeText(this, R.string.create_request_location_pinned, Toast.LENGTH_SHORT).show()
            updatePickMapButtonState()
        }
    }

    private fun submitRequest(
        uid: String,
        name: String,
        title: String,
        category: String,
        contact: String,
        locationForDb: String,
        description: String
    ) {
        setSubmittingState(true)
        val request = ServiceRequest(
            id = editingId.orEmpty(),
            requesterUid = uid,
            requesterName = name,
            title = title,
            category = category,
            contact = contact,
            province = selectedProvince,
            district = selectedDistrict,
            city = selectedCity,
            location = locationForDb,
            description = description,
            status = ServiceRequestStatus.OPEN
        )

        val isUpdate = editingId != null
        val updateFields = mapOf(
            ServiceRequest.FIELD_REQUESTER_NAME to name,
            ServiceRequest.FIELD_TITLE to title,
            ServiceRequest.FIELD_CATEGORY to category,
            ServiceRequest.FIELD_CONTACT to contact,
            ServiceRequest.FIELD_PROVINCE to selectedProvince,
            ServiceRequest.FIELD_DISTRICT to selectedDistrict,
            ServiceRequest.FIELD_CITY to selectedCity,
            ServiceRequest.FIELD_LOCATION to locationForDb,
            ServiceRequest.FIELD_DESCRIPTION to description
        )

        lifecycleScope.launch {
            try {
                if (isUpdate) {
                    ServiceRequestRepository.updateAwait(editingId!!, updateFields)
                } else {
                    ServiceRequestRepository.createAwait(request)
                }
                setSubmittingState(false)
                showPostSuccessDialog(isUpdate)
            } catch (e: Exception) {
                setSubmittingState(false)
                val msg = e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.create_request_post_failed)
                showFailureDialog(msg)
            }
        }
    }

    private fun buildSavedLocationText(streetOrPin: String): String {
        return listOf(streetOrPin, selectedCity, selectedDistrict, selectedProvince)
            .filter { it.isNotBlank() }
            .joinToString(", ")
    }

    private fun refreshLocationFromSpinners() {
        selectedProvince = readSpinnerValue(spinnerProvince)
        selectedDistrict = readSpinnerValue(spinnerDistrict)
        selectedCity = readSpinnerValue(spinnerCity)
    }

    private fun readSpinnerValue(spinner: Spinner): String {
        val position = spinner.selectedItemPosition
        if (position <= 0) return ""
        return spinner.selectedItem?.toString()?.trim().orEmpty()
            .takeIf { it.isNotBlank() && it != "-Select-" }
            .orEmpty()
    }

    private fun setSubmittingState(submitting: Boolean) {
        isSubmitting = submitting
        postButton.isEnabled = !submitting
        postButton.text = if (submitting) {
            getString(R.string.create_request_posting)
        } else {
            getString(R.string.create_request_post)
        }
    }

    private fun setupWindowInsets() {
        SystemUiHelper.setupPurpleHeaderScreen(
            activity = this,
            root = findViewById(R.id.createRequestRoot),
            headerView = findViewById(R.id.headerContainer),
            footerBar = findViewById(R.id.footerBar)
        )
    }

    private fun launchMapPicker() {
        refreshLocationFromSpinners()
        when {
            selectedProvince.isBlank() ->
                Toast.makeText(this, "Please select a province first", Toast.LENGTH_SHORT).show()
            selectedDistrict.isBlank() ->
                Toast.makeText(this, "Please select a district first", Toast.LENGTH_SHORT).show()
            selectedCity.isBlank() ->
                Toast.makeText(this, "Please select a city first", Toast.LENGTH_SHORT).show()
            else -> {
                val coords = selectedLocation?.let { it.latitude to it.longitude }
                    ?: SriLankaLocations.cityCoordinates(selectedProvince, selectedDistrict, selectedCity)
                    ?: (6.9271 to 79.8612)
                mapPicker.launch(
                    Intent(this, PickLocationActivity::class.java).apply {
                        putExtra(PickLocationActivity.EXTRA_PROVINCE, selectedProvince)
                        putExtra(PickLocationActivity.EXTRA_DISTRICT, selectedDistrict)
                        putExtra(PickLocationActivity.EXTRA_CITY, selectedCity)
                        putExtra(PickLocationActivity.EXTRA_LAT, coords.first)
                        putExtra(PickLocationActivity.EXTRA_LNG, coords.second)
                    }
                )
            }
        }
    }

    private fun setupLocationSpinners() {
        val provinces = listOf("-Select-") + SriLankaLocations.provinces.map { it.name }
        spinnerProvince.adapter = spinnerAdapter(provinces)

        spinnerProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressSpinnerCallbacks) return
                selectedProvince = if (position <= 0) "" else provinces[position]
                selectedDistrict = ""
                selectedCity = ""
                selectedLocation = null
                etLocationField.text?.clear()
                updateDistrictSpinner()
                updateCitySpinner()
                updatePickMapButtonState()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        spinnerDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressSpinnerCallbacks) return
                val districts = listOf("-Select-") +
                    SriLankaLocations.districtsForProvince(selectedProvince).map { it.name }
                selectedDistrict = if (position <= 0) "" else districts[position]
                selectedCity = ""
                selectedLocation = null
                etLocationField.text?.clear()
                updateCitySpinner()
                updatePickMapButtonState()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        spinnerCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressSpinnerCallbacks) return
                val cities = listOf("-Select-") +
                    SriLankaLocations.citiesForDistrict(selectedProvince, selectedDistrict).map { it.name }
                selectedCity = if (position <= 0) "" else cities[position]
                selectedLocation = null
                updatePickMapButtonState()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        updateDistrictSpinner()
        updateCitySpinner()
        updatePickMapButtonState()
    }

    private fun updateDistrictSpinner() {
        val districts = if (selectedProvince.isBlank()) {
            listOf("-Select-")
        } else {
            listOf("-Select-") + SriLankaLocations.districtsForProvince(selectedProvince).map { it.name }
        }
        spinnerDistrict.adapter = spinnerAdapter(districts)
        spinnerDistrict.isEnabled = selectedProvince.isNotBlank()
        if (!suppressSpinnerCallbacks) {
            spinnerDistrict.setSelection(0, false)
        }
    }

    private fun updateCitySpinner() {
        val cities = if (selectedProvince.isBlank() || selectedDistrict.isBlank()) {
            listOf("-Select-")
        } else {
            listOf("-Select-") + SriLankaLocations.citiesForDistrict(selectedProvince, selectedDistrict).map { it.name }
        }
        spinnerCity.adapter = spinnerAdapter(cities)
        spinnerCity.isEnabled = selectedDistrict.isNotBlank()
        if (!suppressSpinnerCallbacks) {
            spinnerCity.setSelection(0, false)
        }
    }

    private fun applyLocationToSpinners() {
        if (selectedProvince.isBlank()) return
        suppressSpinnerCallbacks = true
        setSpinnerSelection(spinnerProvince, selectedProvince)
        updateDistrictSpinner()
        if (selectedDistrict.isNotBlank()) {
            setSpinnerSelection(spinnerDistrict, selectedDistrict)
        }
        updateCitySpinner()
        if (selectedCity.isNotBlank()) {
            setSpinnerSelection(spinnerCity, selectedCity)
        }
        suppressSpinnerCallbacks = false
        updatePickMapButtonState()
    }

    private fun setSpinnerSelection(spinner: Spinner, value: String) {
        val adapter = spinner.adapter as? ArrayAdapter<*> ?: return
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i)?.toString().equals(value, ignoreCase = true)) {
                spinner.setSelection(i, false)
                return
            }
        }
    }

    private fun updatePickMapButtonState() {
        btnPickLocation.isEnabled = selectedCity.isNotBlank()
    }

    private fun spinnerAdapter(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, android.R.layout.simple_spinner_item, items).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun showPostSuccessDialog(isUpdate: Boolean) {
        if (isFinishing) return
        val message = if (isUpdate) {
            getString(R.string.request_success_updated)
        } else {
            getString(R.string.request_success_added)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.create_request_success_title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ ->
                navigateToRequestMain()
            }
            .show()
    }

    private fun showFailureDialog(message: String) {
        if (isFinishing) return
        AlertDialog.Builder(this)
            .setTitle(R.string.create_request_failed_title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun navigateToRequestMain() {
        startActivity(
            Intent(this, RequestMainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }
}
