package com.example.microserve

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.microserve.databinding.ActivityEditServiceBinding
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Locale

class EditServiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditServiceBinding
    private var interiorCount = 0
    private var exteriorCount = 0
    private var serviceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityEditServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        serviceId = intent.getStringExtra("SERVICE_ID")

        applyWindowInsets()
        setupSpinner()
        setupLocationSpinners()
        setupCounters()
        setupTimePickers()
        setupDaySelection()
        setupClickListeners()

        populateData()
    }

    private fun applyWindowInsets() {
        SystemUiHelper.setupPurpleHeaderScreen(
            activity = this,
            root = binding.main,
            headerView = binding.headerContainer,
            footerBar = binding.footerBar.root
        )
    }

    // ── Location Data ──────────────────────────────────────────────

    private val locationData = mapOf(
        "Western" to mapOf(
            "Colombo" to listOf("Colombo 01", "Colombo 02", "Dehiwala", "Moratuwa", "Maharagama"),
            "Gampaha" to listOf("Gampaha", "Negombo", "Kelaniya", "Kadawatha"),
            "Kalutara" to listOf("Kalutara", "Panadura", "Horana", "Matugama")
        ),
        "Central" to mapOf(
            "Kandy" to listOf("Kandy", "Peradeniya", "Katugastota", "Gampola"),
            "Matale" to listOf("Matale", "Dambulla", "Sigiriya"),
            "Nuwara Eliya" to listOf("Nuwara Eliya", "Hatton", "Talawakelle")
        ),
        "Southern" to mapOf(
            "Galle" to listOf("Galle", "Hikkaduwa", "Ambalangoda", "Elpitiya"),
            "Matara" to listOf("Matara", "Weligama", "Dickwella", "Akuressa"),
            "Hambantota" to listOf("Hambantota", "Tangalle", "Beliatta", "Ambalantota")
        ),
        "Northern" to mapOf(
            "Jaffna" to listOf("Jaffna", "Chavakachcheri", "Point Pedro", "Nallur"),
            "Kilinochchi" to listOf("Kilinochchi", "Pallai", "Paranthan"),
            "Mannar" to listOf("Mannar", "Murunkan", "Pesalai"),
            "Mullaitivu" to listOf("Mullaitivu", "Puthukkudiyiruppu", "Oddusuddan"),
            "Vavuniya" to listOf("Vavuniya", "Cheddikulam", "Omanthai")
        ),
        "Eastern" to mapOf(
            "Trincomalee" to listOf("Trincomalee", "Kinniya", "Mutur"),
            "Batticaloa" to listOf("Batticaloa", "Kattankudy", "Eravur"),
            "Ampara" to listOf("Ampara", "Kalmunai", "Akkaraipattu")
        ),
        "North Western" to mapOf(
            "Kurunegala" to listOf("Kurunegala", "Kuliyapitiya", "Polgahawela", "Narammala"),
            "Puttalam" to listOf("Puttalam", "Chilaw", "Wennappuwa")
        ),
        "North Central" to mapOf(
            "Anuradhapura" to listOf("Anuradhapura", "Kekirawa", "Tambuttegama", "Eppawala"),
            "Polonnaruwa" to listOf("Polonnaruwa", "Kaduruwela", "Medirigiriya")
        ),
        "Uva" to mapOf(
            "Badulla" to listOf("Badulla", "Bandarawela", "Haputale", "Mahiyanganaya"),
            "Monaragala" to listOf("Monaragala", "Wellawaya", "Bibile", "Kataragama")
        ),
        "Sabaragamuwa" to mapOf(
            "Ratnapura" to listOf("Ratnapura", "Balangoda", "Pelmadulla", "Embilipitiya"),
            "Kegalle" to listOf("Kegalle", "Mawanella", "Warakapola", "Rambukkana")
        )
    )

    // ── Category Config ──────────────────────────────────────────

    private val categoryConfig = mapOf(
        "Painting" to listOf(
            Pair("Interior Walls", "sq. ft."),
            Pair("Exterior Walls", "sq. ft.")
        ),
        "Plumbing" to listOf(
            Pair("Main Pipe Installation", "foot"),
            Pair("Emergency Callout", "hour")
        ),
        "Gardening" to listOf(
            Pair("Lawn Mowing", "sq. ft."),
            Pair("Tree Trimming", "tree")
        ),
        "Cleaning" to listOf(
            Pair("Area Size", "sq. ft."),
            Pair("Deep Clean", "hours")
        ),
        "Electric Work" to listOf(
            Pair("Circuit Repair", "unit"),
            Pair("Wiring", "points")
        ),
        "Handyman" to listOf(
            Pair("Assembly", "item"),
            Pair("General Repair", "hour")
        ),
        "Carpentry" to listOf(
            Pair("Custom Furniture", "piece"),
            Pair("Wood Repair", "hour")
        ),
        "HVAC" to listOf(
            Pair("AC Service", "unit"),
            Pair("Duct Cleaning", "sq. ft.")
        )
    )

    // ── Setup Methods ────────────────────────────────────────────

    private fun setupSpinner() {
        val categories = arrayOf("-Select-", "Painting", "Plumbing", "Gardening", "Cleaning", "Electric Work", "Handyman", "Carpentry", "HVAC")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter

        binding.categorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateMeasurementLabels(categories[position])
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun updateMeasurementLabels(category: String) {
        if (category == "-Select-") return

        val fields = categoryConfig[category] ?: listOf(Pair("Measurement 1", "unit"), Pair("Measurement 2", "unit"))

        binding.txtInteriorLabel.text = "${fields[0].first}:\nM Points per ${fields[0].second}"
        binding.txtExteriorLabel.text = "${fields[1].first}:\nM Points per ${fields[1].second}"
    }

    private fun setupLocationSpinners() {
        val provinces = listOf("-Select Province-") + locationData.keys.toList()

        val provinceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, provinces)
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProvince.adapter = provinceAdapter

        binding.spinnerProvince.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedProvince = provinces[position]
                if (selectedProvince == "-Select Province-") {
                    updateDistrictSpinner(emptyList())
                } else {
                    val districts = locationData[selectedProvince]?.keys?.toList() ?: emptyList()
                    updateDistrictSpinner(listOf("-Select District-") + districts)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        updateDistrictSpinner(emptyList())
    }

    private fun updateDistrictSpinner(districts: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, districts.ifEmpty { listOf("-Select District-") })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDistrict.adapter = adapter

        binding.spinnerDistrict.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (districts.isEmpty() || districts[position] == "-Select District-") {
                    updateCitySpinner(emptyList())
                    return
                }
                val selectedProvince = binding.spinnerProvince.selectedItem.toString()
                val selectedDistrict = districts[position]

                val cities = locationData[selectedProvince]?.get(selectedDistrict) ?: emptyList()
                updateCitySpinner(listOf("-Select City-") + cities)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        updateCitySpinner(emptyList())
    }

    private fun updateCitySpinner(cities: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cities.ifEmpty { listOf("-Select City-") })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCity.adapter = adapter
    }

    private fun setupCounters() {
        binding.interiorPlus.setOnClickListener {
            interiorCount++
            binding.interiorCount.text = interiorCount.toString()
        }
        binding.interiorMinus.setOnClickListener {
            if (interiorCount > 0) {
                interiorCount--
                binding.interiorCount.text = interiorCount.toString()
            }
        }

        binding.exteriorPlus.setOnClickListener {
            exteriorCount++
            binding.exteriorCount.text = exteriorCount.toString()
        }
        binding.exteriorMinus.setOnClickListener {
            if (exteriorCount > 0) {
                exteriorCount--
                binding.exteriorCount.text = exteriorCount.toString()
            }
        }
    }

    private fun setupTimePickers() {
        binding.startTimeBtn.setOnClickListener {
            showTimePicker { time -> binding.startTimeBtn.text = time }
        }

        binding.endTimeBtn.setOnClickListener {
            showTimePicker { time -> binding.endTimeBtn.text = time }
        }
    }

    private fun setupDaySelection() {
        val days = listOf(
            binding.daySun, binding.dayMon, binding.dayTue,
            binding.dayWed, binding.dayThu, binding.dayFri, binding.daySat
        )

        val dayClickListener = View.OnClickListener { view ->
            view.isSelected = !view.isSelected
            if (view is TextView) {
                if (view.isSelected) {
                    view.setTextColor(getColor(R.color.white))
                } else {
                    view.setTextColor(getColor(R.color.black))
                }
            }
        }

        days.forEach { it.setOnClickListener(dayClickListener) }
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(12)
            .setMinute(0)
            .setTitleText("Select Time")
            .build()

        picker.addOnPositiveButtonClickListener {
            val hour = if (picker.hour > 12) picker.hour - 12 else if (picker.hour == 0) 12 else picker.hour
            val amPm = if (picker.hour >= 12) "PM" else "AM"
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hour, picker.minute, amPm)
            onTimeSelected(formattedTime)
        }

        picker.show(supportFragmentManager, "TIME_PICKER")
    }

    // ── Data Population ──────────────────────────────────────────

    private fun populateData() {
        val id = serviceId ?: return

        // Show loading state
        binding.btnEdit.isEnabled = false
        binding.btnDelete.isEnabled = false
        showToast("Loading service details...")

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("services")
            .document(id)
            .get()
            .addOnSuccessListener { document ->
                binding.btnEdit.isEnabled = true
                binding.btnDelete.isEnabled = true

                if (!document.exists()) {
                    showToast("Service not found")
                    return@addOnSuccessListener
                }

                // Populate Category
                val category = document.getString("category") ?: ""
                val categories = arrayOf("-Select-", "Painting", "Plumbing", "Gardening", "Cleaning", "Electric Work", "Handyman", "Carpentry", "HVAC")
                val categoryIndex = categories.indexOf(category)
                if (categoryIndex >= 0) {
                    binding.categorySpinner.setSelection(categoryIndex)
                }

                // Populate Location
                val location = document.getString("location") ?: ""
                val locationParts = location.split(", ")
                if (locationParts.size == 3) {
                    val city = locationParts[0].trim()
                    val district = locationParts[1].trim()
                    val province = locationParts[2].trim()

                    val provinceAdapter = binding.spinnerProvince.adapter as? ArrayAdapter<String>
                    val pIdx = provinceAdapter?.getPosition(province) ?: -1
                    if (pIdx >= 0) {
                        binding.spinnerProvince.setSelection(pIdx)
                        
                        binding.spinnerProvince.post {
                            val districtAdapter = binding.spinnerDistrict.adapter as? ArrayAdapter<String>
                            val dIdx = districtAdapter?.getPosition(district) ?: -1
                            if (dIdx >= 0) {
                                binding.spinnerDistrict.setSelection(dIdx)
                                
                                binding.spinnerDistrict.post {
                                    val cityAdapter = binding.spinnerCity.adapter as? ArrayAdapter<String>
                                    val cIdx = cityAdapter?.getPosition(city) ?: -1
                                    if (cIdx >= 0) {
                                        binding.spinnerCity.setSelection(cIdx)
                                    }
                                }
                            }
                        }
                    }
                }

                // Populate Measurements
                interiorCount = document.getLong("interiorCount")?.toInt() ?: 0
                exteriorCount = document.getLong("exteriorCount")?.toInt() ?: 0
                binding.interiorCount.text = interiorCount.toString()
                binding.exteriorCount.text = exteriorCount.toString()

                // Populate Time Scheduling
                val startTime = document.getString("startTime")
                if (!startTime.isNullOrBlank()) binding.startTimeBtn.text = startTime

                val endTime = document.getString("endTime")
                if (!endTime.isNullOrBlank()) binding.endTimeBtn.text = endTime

                val selectedDaysStr = document.getString("selectedDays") ?: ""
                val selectedDaysList = selectedDaysStr.split(",").map { it.trim() }
                
                val days = listOf(
                    binding.daySun, binding.dayMon, binding.dayTue,
                    binding.dayWed, binding.dayThu, binding.dayFri, binding.daySat
                )
                
                days.forEach { dayView ->
                    if (selectedDaysList.contains(dayView.text.toString())) {
                        dayView.isSelected = true
                        dayView.setTextColor(getColor(R.color.white))
                    } else {
                        dayView.isSelected = false
                        dayView.setTextColor(getColor(R.color.black))
                    }
                }
            }
            .addOnFailureListener {
                binding.btnEdit.isEnabled = true
                binding.btnDelete.isEnabled = true
                showToast("Failed to load service details")
            }
    }

    // ── Click Listeners ──────────────────────────────────────────

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.btnEdit.setOnClickListener {
            val id = serviceId
            if (id == null) {
                showToast("Error: Service ID missing")
                return@setOnClickListener
            }

            val category = binding.categorySpinner.selectedItem.toString()
            val province = binding.spinnerProvince.selectedItem?.toString() ?: ""
            val district = binding.spinnerDistrict.selectedItem?.toString() ?: ""
            val city = binding.spinnerCity.selectedItem?.toString() ?: ""

            when {
                category == "-Select-" -> showToast("Please select a category")
                province == "-Select Province-" || province.isBlank() -> showToast("Please select a province")
                district == "-Select District-" || district.isBlank() -> showToast("Please select a district")
                city == "-Select City-" || city.isBlank() -> showToast("Please select a city")
                interiorCount == 0 && exteriorCount == 0 -> showToast("Please set at least one measurement")
                else -> {
                    val location = "$city, $district, $province"
                    val service = ServiceStore.getServiceById(this, id)
                    
                    if (service != null) {
                        // Update cached schema using ServiceStore
                        ServiceStore.updateService(
                            context = this,
                            serviceId = id,
                            category = category,
                            providerName = service.providerName,
                            contact = service.contact,
                            location = location,
                            email = service.email,
                            imageUri = service.imageUri,
                            replaceImage = false
                        )
                        
                        // Update extra unmapped fields directly to Firestore
                        val selectedDays = listOf(
                            binding.daySun, binding.dayMon, binding.dayTue,
                            binding.dayWed, binding.dayThu, binding.dayFri, binding.daySat
                        ).filter { it.isSelected }.joinToString(",") { it.text.toString() }

                        val extraUpdates = mapOf(
                            "interiorCount" to interiorCount,
                            "exteriorCount" to exteriorCount,
                            "startTime" to binding.startTimeBtn.text.toString(),
                            "endTime" to binding.endTimeBtn.text.toString(),
                            "selectedDays" to selectedDays
                        )
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("services")
                            .document(id)
                            .update(extraUpdates)

                        showToast("Service Updated Successfully!")
                        finish()
                    } else {
                        showToast("Failed to update service")
                    }
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            val id = serviceId
            if (id == null) {
                showToast("Error: Service ID missing")
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Delete Service")
                .setMessage("Are you sure you want to delete this service?")
                .setPositiveButton("Yes") { dialog, _ ->
                    ServiceStore.deleteService(this, id)
                    showToast("Service Deleted")
                    dialog.dismiss()
                    finish()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
