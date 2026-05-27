package com.example.microserve

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.microserve.databinding.ActivityPostAdsBinding

class PostAdsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostAdsBinding
    private lateinit var previousPostAdapter: PreviousPostAdapter
    private lateinit var imageAdjuster: PostImageAdjuster
    private var selectedImagePath: String? = null
    private var selectedLocation: SelectedLocation? = null

    private var selectedProvince: String = ""
    private var selectedDistrict: String = ""
    private var selectedCity: String = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageAdjuster.startAdjust(uri)
        }
    }

    private val mapPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        selectedLocation = SelectedLocation.fromIntent(data)
        updateLocationSummary()
    }

    private val editPostLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        loadPreviousPosts()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPostAdsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupImageAdjuster()
        setupSpinner()
        setupLocationPickers()
        setupPreviousPostsList()
        setupClickListeners()
        prefillProviderFields()
        HomeBottomNavHelper.setup(this, HomeBottomNavHelper.TAB_POST)
    }

    override fun onResume() {
        super.onResume()
        loadPreviousPosts()
    }

    private fun setupWindowInsets() {
        SystemUiHelper.setupPurpleHeaderScreen(
            activity = this,
            root = binding.root,
            headerView = binding.headerContainer
        )
    }

    private fun prefillProviderFields() {
        val profile = AppPreferences.getSessionProfile(this)
        if (binding.providerNameET.text.isNullOrBlank() && profile.name.isNotBlank()) {
            binding.providerNameET.setText(profile.name)
        }
        if (binding.contactET.text.isNullOrBlank() && profile.phone.isNotBlank()) {
            binding.contactET.setText(profile.phone)
        }
        if (binding.emailET.text.isNullOrBlank() && profile.email.isNotBlank()) {
            binding.emailET.setText(profile.email)
        }
    }

    private fun setupSpinner() {
        val categories = arrayOf("-Select-", "Plumbing", "Electrical", "House Painting", "Carpentry", "Cleaning")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter
    }

    private fun setupPreviousPostsList() {
        previousPostAdapter = PreviousPostAdapter { service ->
            val intent = Intent(this, EditPostActivity::class.java).apply {
                putExtra(ServiceStore.EXTRA_SERVICE_ID, service.id)
            }
            editPostLauncher.launch(intent)
        }
        binding.previousPostsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PostAdsActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = previousPostAdapter
            setHasFixedSize(false)
        }
    }

    private fun loadPreviousPosts() {
        val posts = ServiceStore.getMyPosts(this)
        previousPostAdapter.submitList(posts)
        val hasPosts = posts.isNotEmpty()
        binding.tvNoPreviousPosts.visibility = if (hasPosts) View.GONE else View.VISIBLE
        binding.previousPostsRecyclerView.visibility = if (hasPosts) View.VISIBLE else View.GONE
    }

    private fun setupLocationPickers() {
        val placeholder = getString(R.string.post_ads_select_option)
        val provinces = listOf(placeholder) + SriLankaLocations.provinces.map { it.name }
        binding.provinceSpinner.adapter = spinnerAdapter(provinces)

        binding.provinceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedProvince = if (position <= 0) "" else provinces[position]
                selectedDistrict = ""
                selectedCity = ""
                selectedLocation = null
                updateDistrictSpinner()
                updateCitySpinner()
                updateLocationSummary()
                binding.btnPickOnMap.isEnabled = false
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.districtSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val districts = listOf(placeholder) + SriLankaLocations.districtsForProvince(selectedProvince).map { it.name }
                selectedDistrict = if (position <= 0) "" else districts[position]
                selectedCity = ""
                selectedLocation = null
                updateCitySpinner()
                updateLocationSummary()
                binding.btnPickOnMap.isEnabled = false
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val cities = listOf(placeholder) + SriLankaLocations.citiesForDistrict(selectedProvince, selectedDistrict).map { it.name }
                selectedCity = if (position <= 0) "" else cities[position]
                selectedLocation = null
                updateLocationSummary()
                binding.btnPickOnMap.isEnabled = selectedCity.isNotBlank()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun updateDistrictSpinner() {
        val placeholder = getString(R.string.post_ads_select_option)
        val districts = if (selectedProvince.isBlank()) {
            listOf(placeholder)
        } else {
            listOf(placeholder) + SriLankaLocations.districtsForProvince(selectedProvince).map { it.name }
        }
        binding.districtSpinner.adapter = spinnerAdapter(districts)
        binding.districtSpinner.isEnabled = selectedProvince.isNotBlank()
        binding.districtSpinner.setSelection(0)
    }

    private fun updateCitySpinner() {
        val placeholder = getString(R.string.post_ads_select_option)
        val cities = if (selectedProvince.isBlank() || selectedDistrict.isBlank()) {
            listOf(placeholder)
        } else {
            listOf(placeholder) + SriLankaLocations.citiesForDistrict(selectedProvince, selectedDistrict).map { it.name }
        }
        binding.citySpinner.adapter = spinnerAdapter(cities)
        binding.citySpinner.isEnabled = selectedDistrict.isNotBlank()
        binding.citySpinner.setSelection(0)
    }

    private fun spinnerAdapter(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, android.R.layout.simple_spinner_item, items).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun updateLocationSummary() {
        binding.tvSelectedLocationSummary.text = when {
            selectedLocation != null -> selectedLocation!!.formattedLocation()
            selectedCity.isNotBlank() -> getString(
                R.string.post_ads_location_pending_map,
                selectedCity,
                selectedDistrict,
                selectedProvince
            )
            selectedDistrict.isNotBlank() -> "$selectedDistrict, $selectedProvince"
            selectedProvince.isNotBlank() -> selectedProvince
            else -> getString(R.string.post_ads_location_not_selected)
        }
    }

    private fun setupImageAdjuster() {
        imageAdjuster = PostImageAdjuster(
            activity = this,
            imageView = binding.selectedImagePreview,
            placeholder = binding.addImagePlaceholder,
            adjustControls = binding.imageAdjustControls,
            confirmButton = binding.btnConfirmImageAdjust,
            cancelButton = binding.btnCancelImageAdjust,
            changeHint = binding.tvImageChangeHint,
            adjustHint = binding.tvAdjustImageHint,
            onImageSaved = { savedPath -> applySavedImage(savedPath) }
        )
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener { finish() }

        binding.addImageBtn.setOnClickListener {
            if (binding.imageAdjustControls.visibility == View.VISIBLE) return@setOnClickListener
            pickImage.launch("image/*")
        }

        binding.btnPickOnMap.setOnClickListener {
            if (selectedProvince.isBlank() || selectedDistrict.isBlank() || selectedCity.isBlank()) {
                Toast.makeText(this, R.string.post_ads_select_location_first, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val coords = SriLankaLocations.cityCoordinates(selectedProvince, selectedDistrict, selectedCity)
                ?: (6.9271 to 79.8612)
            val intent = Intent(this, PickLocationActivity::class.java).apply {
                putExtra(PickLocationActivity.EXTRA_PROVINCE, selectedProvince)
                putExtra(PickLocationActivity.EXTRA_DISTRICT, selectedDistrict)
                putExtra(PickLocationActivity.EXTRA_CITY, selectedCity)
                putExtra(PickLocationActivity.EXTRA_LAT, coords.first)
                putExtra(PickLocationActivity.EXTRA_LNG, coords.second)
            }
            mapPicker.launch(intent)
        }

        binding.postBtn.setOnClickListener {
            postAd()
        }
    }

    private fun postAd() {
        val category = binding.categorySpinner.selectedItem?.toString().orEmpty()
        val providerName = binding.providerNameET.text.toString().trim()
        val contact = binding.contactET.text.toString().trim()
        val email = binding.emailET.text.toString().trim()

        when {
            category == "-Select-" -> toast(getString(R.string.post_ads_select_category))
            providerName.isBlank() -> toast(getString(R.string.post_ads_enter_provider))
            selectedLocation == null -> toast(getString(R.string.post_ads_pick_map_location))
            contact.isBlank() -> toast(getString(R.string.post_ads_enter_contact))
            email.isBlank() -> toast(getString(R.string.post_ads_enter_email))
            else -> {
                ServiceStore.addService(
                    context = this,
                    category = category,
                    providerName = providerName,
                    contact = contact,
                    location = selectedLocation!!.formattedLocation(),
                    email = email,
                    imageUri = selectedImagePath
                )
                resetFormAfterPost()
                loadPreviousPosts()
                scrollToPreviouslyDoneJobs()
                showPostedSuccessDialog()
            }
        }
    }

    private fun showPostedSuccessDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.post_ads_posted_title)
            .setMessage(R.string.post_ads_posted_success_stay)
            .setCancelable(true)
            .setPositiveButton(R.string.action_ok, null)
            .show()
    }

    private fun scrollToPreviouslyDoneJobs() {
        binding.postAdsScrollView.post {
            val target = binding.previousPostsRecyclerView.top
            binding.postAdsScrollView.smoothScrollTo(0, target.coerceAtLeast(0))
            binding.previousPostsRecyclerView.post {
                binding.previousPostsRecyclerView.smoothScrollToPosition(0)
            }
        }
    }

    private fun applySavedImage(savedPath: String) {
        val previous = selectedImagePath
        if (!previous.isNullOrBlank() && previous != savedPath) {
            PostImageHelper.deletePostImage(this, previous)
        }
        selectedImagePath = savedPath
    }

    private fun resetFormAfterPost() {
        binding.categorySpinner.setSelection(0)
        selectedImagePath = null
        selectedLocation = null
        selectedProvince = ""
        selectedDistrict = ""
        selectedCity = ""
        imageAdjuster.reset()
        binding.imageActionText.text = getString(R.string.post_ads_add_image)
        binding.provinceSpinner.setSelection(0)
        updateDistrictSpinner()
        updateCitySpinner()
        updateLocationSummary()
        binding.btnPickOnMap.isEnabled = false
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
