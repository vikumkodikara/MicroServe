package com.example.microserve

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class CreateRequestActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REQUEST_ID = "request_id"
        const val EXTRA_CATEGORY = "extra_category"
    }

    private var editingId: String? = null
    private lateinit var spinnerProvince: Spinner
    private lateinit var spinnerDistrict: Spinner
    private lateinit var spinnerCity: Spinner
    private var suppressSpinnerCallbacks = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_request)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val etName = findViewById<EditText>(R.id.et_name)
        val etTitle = findViewById<EditText>(R.id.et_title)
        val spinner = findViewById<Spinner>(R.id.spinner_category)
        val etContact = findViewById<EditText>(R.id.et_contact)
        val etLocation = findViewById<EditText>(R.id.et_location)
        val etDescription = findViewById<EditText>(R.id.et_description)
        spinnerProvince = findViewById(R.id.spinner_province)
        spinnerDistrict = findViewById(R.id.spinner_district)
        spinnerCity = findViewById(R.id.spinner_city)

        val categories = arrayOf("-Select-", "Plumbing", "Gardening", "Cleaning", "Painting", "Electric", "Handyman", "Carpentry", "Mechanic", "HVAC")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        setupLocationSpinners()

        val preselectedCategory = intent.getStringExtra(EXTRA_CATEGORY)
        if (!preselectedCategory.isNullOrBlank()) {
            val index = categories.indexOfFirst { it.equals(preselectedCategory, ignoreCase = true) }
            if (index >= 0) spinner.setSelection(index)
        }

        editingId = intent.getStringExtra(EXTRA_REQUEST_ID)
        if (editingId != null) {
            ServiceRequestRepository.getById(
                requestId = editingId!!,
                onSuccess = { req ->
                    etName.setText(req.requesterName)
                    etTitle.setText(req.title)
                    etContact.setText(req.contact)
                    etLocation.setText(req.location)
                    etDescription.setText(req.description)
                    val catIndex = categories.indexOf(req.category)
                    if (catIndex >= 0) spinner.setSelection(catIndex)
                    prefillLocation(req.province, req.district, req.city)
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

        findViewById<View>(R.id.btn_post).setOnClickListener {
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name = etName.text.toString().trim()
            val title = etTitle.text.toString().trim()
            val category = spinner.selectedItem?.toString()?.trim().orEmpty()
            val contact = etContact.text.toString().trim()
            val province = spinnerProvince.selectedItem?.toString()?.trim().orEmpty()
            val district = spinnerDistrict.selectedItem?.toString()?.trim().orEmpty()
            val city = spinnerCity.selectedItem?.toString()?.trim().orEmpty()
            val location = etLocation.text.toString().trim()
            val description = etDescription.text.toString().trim()

            when {
                name.isBlank() || title.isBlank() -> {
                    Toast.makeText(this, "Please fill in name and title", Toast.LENGTH_SHORT).show()
                }
                category.isBlank() || category == "-Select-" -> {
                    Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                }
                province.isBlank() || province == "-Select-" -> {
                    Toast.makeText(this, "Please select a province", Toast.LENGTH_SHORT).show()
                }
                district.isBlank() || district == "-Select-" -> {
                    Toast.makeText(this, "Please select a district", Toast.LENGTH_SHORT).show()
                }
                city.isBlank() || city == "-Select-" -> {
                    Toast.makeText(this, "Please select a city", Toast.LENGTH_SHORT).show()
                }
                location.isBlank() -> {
                    Toast.makeText(this, "Please enter street or address", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val request = ServiceRequest(
                        id = editingId.orEmpty(),
                        requesterUid = uid,
                        requesterName = name,
                        title = title,
                        category = category,
                        contact = contact,
                        province = province,
                        district = district,
                        city = city,
                        location = location,
                        description = description,
                        status = ServiceRequestStatus.OPEN
                    )

                    if (editingId != null) {
                        ServiceRequestRepository.update(
                            requestId = editingId!!,
                            fields = mapOf(
                                ServiceRequest.FIELD_REQUESTER_NAME to name,
                                ServiceRequest.FIELD_TITLE to title,
                                ServiceRequest.FIELD_CATEGORY to category,
                                ServiceRequest.FIELD_CONTACT to contact,
                                ServiceRequest.FIELD_PROVINCE to province,
                                ServiceRequest.FIELD_DISTRICT to district,
                                ServiceRequest.FIELD_CITY to city,
                                ServiceRequest.FIELD_LOCATION to location,
                                ServiceRequest.FIELD_DESCRIPTION to description
                            ),
                            onSuccess = {
                                showSuccessDialog(getString(R.string.request_success_updated))
                            },
                            onFailure = { message ->
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        ServiceRequestRepository.create(
                            request = request,
                            onSuccess = {
                                showSuccessDialog(getString(R.string.request_success_added))
                            },
                            onFailure = { message ->
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun setupLocationSpinners() {
        bindSpinner(spinnerProvince, listOf("-Select-") + SriLankaLocations.provinces.map { it.name })

        spinnerProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressSpinnerCallbacks) return
                val province = spinnerProvince.selectedItem?.toString().orEmpty()
                val districts = if (province == "-Select-") emptyList()
                else SriLankaLocations.districtsForProvince(province).map { it.name }
                bindSpinner(spinnerDistrict, listOf("-Select-") + districts)
                bindSpinner(spinnerCity, listOf("-Select-"))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        spinnerDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressSpinnerCallbacks) return
                val province = spinnerProvince.selectedItem?.toString().orEmpty()
                val district = spinnerDistrict.selectedItem?.toString().orEmpty()
                val cities = if (district == "-Select-") emptyList()
                else SriLankaLocations.citiesForDistrict(province, district).map { it.name }
                bindSpinner(spinnerCity, listOf("-Select-") + cities)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun prefillLocation(province: String, district: String, city: String) {
        suppressSpinnerCallbacks = true
        selectSpinnerValue(spinnerProvince, province)
        bindSpinner(spinnerDistrict, listOf("-Select-") + SriLankaLocations.districtsForProvince(province).map { it.name })
        selectSpinnerValue(spinnerDistrict, district)
        bindSpinner(spinnerCity, listOf("-Select-") + SriLankaLocations.citiesForDistrict(province, district).map { it.name })
        selectSpinnerValue(spinnerCity, city)
        suppressSpinnerCallbacks = false
    }

    private fun bindSpinner(spinner: Spinner, items: List<String>) {
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
    }

    private fun selectSpinnerValue(spinner: Spinner, value: String) {
        val adapter = spinner.adapter as? ArrayAdapter<*> ?: return
        for (index in 0 until adapter.count) {
            if (adapter.getItem(index)?.toString().equals(value, ignoreCase = true)) {
                spinner.setSelection(index)
                return
            }
        }
    }

    private fun showSuccessDialog(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }
}
