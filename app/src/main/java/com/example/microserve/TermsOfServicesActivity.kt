package com.example.microserve

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class TermsOfServicesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fragment_terms_of_services)

        // Bind back button
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        // Bind I Agree button
        findViewById<View>(R.id.btn_agree).setOnClickListener {
            Toast.makeText(this, "Terms of Services accepted", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
