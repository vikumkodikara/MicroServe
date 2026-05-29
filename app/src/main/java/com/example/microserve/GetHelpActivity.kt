package com.example.microserve

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class GetHelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gethelp)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header_bg)) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.setPadding(view.paddingLeft, top, view.paddingRight, view.paddingBottom)
            insets
        }

        val userName = AppPreferences.getSessionName(this).ifBlank { "User" }
        findViewById<TextView>(R.id.tv_subtitle)?.text = "Hello $userName, how can we help ?"
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        // Setup dynamic email sending click listeners for the 4 Help category cards
        findViewById<View>(R.id.card_bookings).setOnClickListener {
            sendSupportEmail("Bookings", "[Get Help] Bookings Support Request")
        }

        findViewById<View>(R.id.card_payments).setOnClickListener {
            sendSupportEmail("Payments", "[Get Help] Payments & Billing Support Request")
        }

        findViewById<View>(R.id.card_account).setOnClickListener {
            sendSupportEmail("Account", "[Get Help] Account & Profile Support Request")
        }

        findViewById<View>(R.id.card_safety).setOnClickListener {
            sendSupportEmail("Safety & Legal", "[Get Help] Safety & Legal Support Request")
        }

        findViewById<View>(R.id.btn_live_chat).setOnClickListener {
            Toast.makeText(this, "Live Chat coming soon", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btn_call_hotline).setOnClickListener {
            Toast.makeText(this, "Call Hotline coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSupportEmail(category: String, subject: String) {
        val userName = AppPreferences.getSessionName(this).ifBlank { "User" }
        val emailBody = """
            Hi MicroServe Support,

            I need assistance with my $category. Here are the details of my issue:

            [Please describe your issue here]

            ---
            User Name: $userName
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:support@microserve.local")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, emailBody)
        }

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No email client app found on your device", Toast.LENGTH_SHORT).show()
        }
    }
}
