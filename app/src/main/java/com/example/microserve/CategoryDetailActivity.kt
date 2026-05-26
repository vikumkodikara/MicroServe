package com.example.microserve

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class CategoryDetailActivity : AppCompatActivity() {

    private lateinit var chipsContainer: LinearLayout
    private lateinit var providersContainer: LinearLayout
    private var currentCategory: String = ""

    private val allCategories = listOf(
        "Plumbing", "Cleaning", "Gardening", "Painting", "Electric", "Handyman",
        "Carpentry", "Mechanic", "HVAC"
    )

    private val sampleProviders = mapOf(
        "Plumbing" to listOf(
            "Sisira Kumara" to "Pipe leak repair",
            "Kulathunga Herath" to "Tap & faucet installation",
            "Kavindya Sathsarani" to "Bathroom fitting",
            "Yohan Silva" to "Blocked drain cleaning",
            "Mihiri Katunayaka" to "Pipe leak repair"
        ),
        "Cleaning" to listOf(
            "Nimal Perera" to "House deep cleaning",
            "Sanduni Fernando" to "Office cleaning service",
            "Kamal Jayasinghe" to "Window & glass cleaning",
            "Dilini Weerasinghe" to "Carpet & upholstery cleaning",
            "Ruwan Bandara" to "Post-construction cleaning"
        ),
        "Gardening" to listOf(
            "Sunil Rathnayake" to "Garden maintenance",
            "Amara Dissanayake" to "Lawn mowing & trimming",
            "Pradeep Kumara" to "Tree pruning service",
            "Nimali Jayawardena" to "Weed removal",
            "Kasun Wickramasinghe" to "Landscaping design"
        ),
        "Painting" to listOf(
            "Nimal Herath" to "House repainting",
            "Saman Kumara" to "Interior wall painting",
            "Lakshitha Fernando" to "Exterior painting",
            "Chaminda Rajapakse" to "Fence & gate painting",
            "Dinesh Gunawardena" to "Waterproofing & painting"
        ),
        "Electric" to listOf(
            "Rajitha Perera" to "Wiring & rewiring",
            "Amal Gunaratne" to "Electrical panel upgrade",
            "Chathura Bandara" to "Light fixture installation",
            "Sampath Jayasuriya" to "Generator installation",
            "Nuwan Liyanage" to "Ceiling fan installation"
        ),
        "Handyman" to listOf(
            "Asanka Kumara" to "Furniture assembly",
            "Roshan Perera" to "Door & lock repair",
            "Thilina Madushanka" to "Wall mounting service",
            "Janaka Wijesinghe" to "Shelf & cabinet installation",
            "Lasith Dissanayake" to "General home repairs"
        ),
        "Carpentry" to listOf(
            "Chamara Wimalasena" to "Custom furniture making",
            "Nishantha Perera" to "Door frame repair",
            "Ranjith Senanayake" to "Wooden deck building",
            "Mahinda Rajapakse" to "Cabinet & wardrobe work",
            "Sarath Kumara" to "Roof timber framing"
        ),
        "Mechanic" to listOf(
            "Dhananjaya Silva" to "Vehicle engine repair",
            "Prasanna Kumara" to "Brake & suspension service",
            "Gayan Wickrama" to "Motorbike servicing",
            "Lahiru Fernando" to "Oil change & tune-up",
            "Tharindu Jayasena" to "Battery & electrical fix"
        ),
        "HVAC" to listOf(
            "Samantha Perera" to "AC installation & repair",
            "Kumara Herath" to "Central air maintenance",
            "Dilshan Jayawardena" to "Duct cleaning service",
            "Naveen Bandara" to "Refrigerator repair",
            "Asela Gunawardena" to "Heating system installation"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category_detail)

        chipsContainer = findViewById(R.id.chipsContainer)
        providersContainer = findViewById(R.id.providersContainer)

        currentCategory = intent.getStringExtra("category") ?: allCategories.first()

        findViewById<TextView>(R.id.tv_category_title).text = currentCategory
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        buildChips()
        loadProviders()
    }

    private fun buildChips() {
        chipsContainer.removeAllViews()

        for (cat in allCategories) {
            val chip = TextView(this).apply {
                text = cat
                textSize = 13f
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                gravity = Gravity.CENTER

                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(dpToPx(4), 0, dpToPx(4), 0)
                layoutParams = lp

                if (cat == currentCategory) {
                    setBackgroundResource(R.drawable.chip_selected_bg)
                    setTextColor(Color.WHITE)
                } else {
                    setBackgroundResource(R.drawable.chip_unselected_bg)
                    setTextColor(Color.parseColor("#4a4458"))
                }

                setOnClickListener {
                    currentCategory = cat
                    findViewById<TextView>(R.id.tv_category_title).text = cat
                    buildChips()
                    loadProviders()
                }
            }
            chipsContainer.addView(chip)
        }
    }

    private fun loadProviders() {
        providersContainer.removeAllViews()

        val providers = sampleProviders[currentCategory] ?: emptyList()

        if (providers.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No providers available for this category"
                textSize = 14f
                setTextColor(0xFF777777.toInt())
                setPadding(0, dpToPx(30), 0, 0)
                gravity = Gravity.CENTER
            }
            providersContainer.addView(empty)
            return
        }

        for ((name, desc) in providers) {
            val item = LayoutInflater.from(this)
                .inflate(R.layout.item_service_provider, providersContainer, false)
            item.findViewById<TextView>(R.id.tv_provider_name).text = name
            item.findViewById<TextView>(R.id.tv_provider_desc).text = "Description :- $desc"
            providersContainer.addView(item)
        }
    }

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
}
