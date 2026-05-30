package com.example.microserve

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge to edge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.statusBarColor = Color.TRANSPARENT
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.statusBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_main)

        // Setup Categories Recycler View
        val rvCategories: RecyclerView = findViewById(R.id.rvCategories)
        
        // Vertical grid with 3 columns
        val categoryLayoutManager = GridLayoutManager(this, 3)
        rvCategories.layoutManager = categoryLayoutManager
        
        val categories = listOf(
            Category("Plumbing", R.drawable.plumber),
            Category("Gardening", R.drawable.gardening),
            Category("Cleaning", R.drawable.cleaning),
            Category("Painting", R.drawable.painting),
            Category("Electric", R.drawable.electrician),
            Category("Handyman", R.drawable.handyman),
            Category("Carpentry", R.drawable.carpentry),
            Category("Mechanic", R.drawable.mechanic),
            Category("HVAC", R.drawable.hvac)
        )
        
        // Debugging Log to confirm data is reaching the UI
        Log.d("MainActivity", "Category list size: ${categories.size}")
        
        val categoryAdapter = CategoryAdapter(categories) { selectedCategory ->
            val intent = Intent(this, CategoryActivity::class.java)
            intent.putExtra("CATEGORY_NAME", selectedCategory.title)
            startActivity(intent)
        }
        rvCategories.adapter = categoryAdapter

        // Setup My Services Recycler View
        val rvMyServices: RecyclerView = findViewById(R.id.rvMyServices)
        
        val myServicesLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvMyServices.layoutManager = myServicesLayoutManager
        
        // Using Firebase data
        loadMyServices(rvMyServices)

        // Apply Navigation Bar Inset so it perfectly aligns with Home
        applyNavBarSpacer(R.id.navSystemBarSpacer)

        // Initialize Bottom Navigation
        setupBottomNavigation()

        // Handle UI Back Button
        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            navigateHome()
        }

        // Handle Hardware Back Button
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateHome()
            }
        })
    }

    private fun navigateHome() {
        startActivity(Intent(this, Homepage::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }

    private fun loadMyServices(rvMyServices: RecyclerView) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

        ServiceStore.loadFromFirestore(this) { allServices ->
            val myPosts = allServices.filter { it.ownerUid == uid }

            val myServiceItems = myPosts.map { service ->
                val catalog = CategoryCatalog.findByStoreKey(service.category)
                val iconRes = catalog?.imageRes ?: R.drawable.img_plumber
                MyService(
                    title = service.category,
                    imageResId = iconRes,
                    isActive = service.isActive,
                    serviceId = service.id
                )
            }.toMutableList()

            // Always add the "Add" card at the end
            myServiceItems.add(MyService("Add", 0, false))

            val adapter = MyServiceAdapter(myServiceItems) { selectedService ->
                if (selectedService.title == "Add") {
                    startActivity(Intent(this, PostServiceActivity::class.java))
                } else {
                    val intent = Intent(this, EditServiceActivity::class.java)
                    intent.putExtra("SERVICE_ID", selectedService.serviceId)
                    startActivity(intent)
                }
            }
            rvMyServices.adapter = adapter
        }
    }

    override fun onResume() {
        super.onResume()
        val rvMyServices: RecyclerView = findViewById(R.id.rvMyServices)
        loadMyServices(rvMyServices)
    }

    private fun setupBottomNavigation() {
        val navRequest = findViewById<View>(R.id.navTabRequest)
        val navService = findViewById<View>(R.id.navTabService)
        val navHome = findViewById<View>(R.id.navTabHome)
        val navPost = findViewById<View>(R.id.navTabPost)
        val navProfile = findViewById<View>(R.id.navTabProfile)

        navRequest?.setOnClickListener {
            startActivity(Intent(this, RequestMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        navService?.setOnClickListener {
            // Already in Service Activity (MainActivity)
        }

        navHome?.setOnClickListener {
            navigateHome()
        }

        navPost?.setOnClickListener {
            startActivity(Intent(this, PostAdsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        navProfile?.setOnClickListener {
            startActivity(Intent(this, PersonalInfoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
