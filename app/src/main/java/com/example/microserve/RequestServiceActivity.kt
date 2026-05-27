package com.example.microserve

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityRequestServiceBinding

/**
 * Form for customers to REQUEST services.
 * Data saves to RequestStore and appears in admin RequestersActivity.
 */
class RequestServiceActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
    }

    private lateinit var binding: ActivityRequestServiceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRequestServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupSpinner()
        setupClickListeners()
        HomeBottomNavHelper.setup(this, HomeBottomNavHelper.TAB_HOME)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.requestServiceRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            findViewById<View>(R.id.navSystemBarSpacer)?.let { spacer ->
                val lp = spacer.layoutParams
                if (lp.height != systemBars.bottom) {
                    lp.height = systemBars.bottom
                    spacer.layoutParams = lp
                }
            }
            insets
        }
    }

    private fun setupSpinner() {
        val categories = arrayOf(
            "-Select-",
            "Plumbing",
            "Electrical",
            "House Painting",
            "Carpentry",
            "Cleaning",
            "Gardening",
            "Graphic Design",
            "Welding"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter

        val preselected = intent.getStringExtra(EXTRA_CATEGORY)
        if (!preselected.isNullOrBlank()) {
            val index = categories.indexOfFirst { it.equals(preselected, ignoreCase = true) }
            if (index >= 0) {
                binding.categorySpinner.setSelection(index)
            }
        }
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.requestBtn.setOnClickListener {
            val title = binding.titleET.text.toString().trim()
            val name = binding.nameET.text.toString().trim()
            val contact = binding.contactET.text.toString().trim()
            val location = binding.locationET.text.toString().trim()
            val description = binding.descriptionET.text.toString().trim()
            val category = binding.categorySpinner.selectedItem.toString()

            if (category == "-Select-") {
                Toast.makeText(this, R.string.request_service_select_category, Toast.LENGTH_SHORT).show()
            } else if (title.isEmpty() || name.isEmpty() || contact.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, R.string.request_service_fill_required, Toast.LENGTH_SHORT).show()
            } else {
                val existingUser = UserStore.getAllUsers(this)
                    .firstOrNull { it.name.equals(name, ignoreCase = true) }

                if (existingUser == null) {
                    UserStore.addUser(
                        context = this,
                        name = name,
                        email = "requester_${System.currentTimeMillis()}@microserve.local",
                        phone = contact,
                        type = UserStore.TYPE_REQUESTER
                    )
                }

                RequestStore.addRequest(
                    context = this,
                    requesterName = name,
                    title = title,
                    category = category,
                    contact = contact,
                    location = location,
                    description = description
                )
                Toast.makeText(this, R.string.request_service_success, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
