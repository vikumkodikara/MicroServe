package com.example.microserve

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityPostServiceBinding
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Locale

class PostServiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostServiceBinding
    private var interiorCount = 0
    private var exteriorCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPostServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupSpinner()
        setupCounters()
        setupTimePickers()
        setupDaySelection()
        setupClickListeners()
    }

    private fun applyWindowInsets() {
        SystemUiHelper.setupPurpleHeaderScreen(
            activity = this,
            root = binding.main,
            headerView = binding.headerContainer,
            footerBar = binding.footerBar.root
        )
    }

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

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.postBtn.setOnClickListener {
            val category = binding.categorySpinner.selectedItem.toString()
            val startTime = binding.startTimeBtn.text.toString()
            val endTime = binding.endTimeBtn.text.toString()

            when {
                category == "-Select-" -> showToast("Please select a category")
                startTime == "Select Time" || endTime == "Select Time" -> showToast("Please select time scheduling")
                interiorCount == 0 && exteriorCount == 0 -> showToast("Please set at least one measurement")
                else -> {
                    showToast("Service Posted Successfully!")
                    finish()
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
