package com.example.microserve

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class ContactUsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_contact_us)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.btn_phone).setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+94773456881")))
        }

        findViewById<View>(R.id.btn_email).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:Company.info@gmail.com"))
            startActivity(Intent.createChooser(intent, "Send Email"))
        }
    }
}
