package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ServiceMainActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    private data class ServiceItem(val name: String, val iconRes: Int)

    private val services = listOf(
        ServiceItem("Plumbing", R.drawable.plumber),
        ServiceItem("Gardening", R.drawable.gardening),
        ServiceItem("Cleaning", R.drawable.cleaning),
        ServiceItem("Painting", R.drawable.painting),
        ServiceItem("Electric", R.drawable.electrician),
        ServiceItem("Handyman", R.drawable.handyman),
        ServiceItem("Carpentry", R.drawable.carpentry),
        ServiceItem("Mechanic", R.drawable.mechanic),
        ServiceItem("HVAC", R.drawable.hvac)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_service_main)

        container = findViewById(R.id.servicesListContainer)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        val categoryMap = mapOf(
            R.id.cat_plumbing to "Plumbing",
            R.id.cat_gardening to "Gardening",
            R.id.cat_cleaning to "Cleaning",
            R.id.cat_painting to "Painting",
            R.id.cat_electric to "Electric",
            R.id.cat_handyman to "Handyman",
            R.id.cat_carpentry to "Carpentry",
            R.id.cat_mechanic to "Mechanic",
            R.id.cat_hvac to "HVAC"
        )
        for ((viewId, catName) in categoryMap) {
            findViewById<View>(viewId).setOnClickListener {
                startActivity(
                    Intent(this, CategoryDetailActivity::class.java)
                        .putExtra("category", catName)
                )
            }
        }

        HomeBottomNavHelper.setup(this, HomeBottomNavHelper.TAB_SERVICE)
        loadServices()
    }

    private fun loadServices() {
        container.removeAllViews()

        for (service in services) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_my_service, container, false)

            item.findViewById<ImageView>(R.id.iv_service_icon).setImageResource(service.iconRes)
            item.findViewById<TextView>(R.id.tv_service_name).text = service.name

            item.findViewById<MaterialButton>(R.id.btn_activate).setOnClickListener {
                Toast.makeText(this, "${service.name} activated", Toast.LENGTH_SHORT).show()
            }

            container.addView(item)
        }
    }
}
