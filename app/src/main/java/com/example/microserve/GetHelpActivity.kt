package com.example.microserve

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class GetHelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fragment_get_help)

        // Set dynamic personalized subtitle using dynamic name resource
        val userName = AppPreferences.getSessionName(this).ifBlank { "User" }
        findViewById<TextView>(R.id.tv_subtitle)?.text = getString(R.string.help_subtitle, userName)

        // Bind back button
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        // Bind grid cards to simple helpful guidance toasts
        findViewById<View>(R.id.grid_layout)?.let { grid ->
            // In layout, bookings, payments, account, safety card views are defined.
            // We can attach click listeners to the views inside the grid layout.
            // For now, let's attach to the main grid children or standard mock actions
        }

        // Bind bottom support contact buttons
        findViewById<View>(R.id.btn_live_chat).setOnClickListener {
            Toast.makeText(this, "Live Chat is coming soon!", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btn_call_hotline).setOnClickListener {
            Toast.makeText(this, "Calling hotline: +94 11 234 5678", Toast.LENGTH_SHORT).show()
        }
    }
}
