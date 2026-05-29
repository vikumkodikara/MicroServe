package com.example.microserve

import android.app.AlertDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class EditServiceActivity : AppCompatActivity() {

    // Dynamic Configuration Map (Kotlin equivalent to the requested TypeScript/Dart map)
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

    private lateinit var measurementContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.statusBarColor = Color.TRANSPARENT
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.statusBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_edit_service)

        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val btnEdit: Button = findViewById(R.id.btnEdit)
        btnEdit.setOnClickListener {
            Toast.makeText(this, "Service Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }

        val btnDelete: ImageView = findViewById(R.id.btnDelete)
        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Service")
                .setMessage("Are you sure want to delete this service?")
                .setPositiveButton("Yes") { dialog, _ ->
                    Toast.makeText(this, "Service Deleted", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    finish()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        measurementContainer = findViewById(R.id.measurementContainer)
        setupSpinner()
    }

    private fun setupSpinner() {
        val spinnerCategory: Spinner = findViewById(R.id.spinnerCategory)
        val categories = arrayOf("-Select-", "Painting", "Plumbing", "Gardening", "Cleaning", "Electric Work", "Handyman", "Carpentry", "HVAC")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                renderMeasurementFields(selectedCategory)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        
        spinnerCategory.setSelection(1) // Select Painting by default to match screenshot
    }

    private fun renderMeasurementFields(category: String) {
        measurementContainer.removeAllViews()

        val fields = categoryConfig[category] ?: return

        for ((label, unit) in fields) {
            val fieldLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(12)
                }
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvLabel = TextView(this).apply {
                // Formatting to use 'Points' instead of 'Rs.'
                text = "$label: Points per $unit"
                textSize = 13f
                setTextColor(Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            var count = 0
            val tvCount = TextView(this).apply {
                text = count.toString()
                textSize = 14f
                setPadding(dpToPx(16), 0, dpToPx(16), 0)
                setTextColor(Color.BLACK)
            }

            val btnMinus = TextView(this).apply {
                text = "✖"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(androidx.core.content.ContextCompat.getColor(this@EditServiceActivity, R.color.purple_nav))
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                setOnClickListener {
                    if (count > 0) {
                        count--
                        tvCount.text = count.toString()
                    }
                }
            }

            val btnPlus = TextView(this).apply {
                text = "➕"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(androidx.core.content.ContextCompat.getColor(this@EditServiceActivity, R.color.purple_nav))
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                setOnClickListener {
                    count++
                    tvCount.text = count.toString()
                }
            }

            fieldLayout.addView(tvLabel)
            fieldLayout.addView(btnMinus)
            fieldLayout.addView(tvCount)
            fieldLayout.addView(btnPlus)

            measurementContainer.addView(fieldLayout)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
