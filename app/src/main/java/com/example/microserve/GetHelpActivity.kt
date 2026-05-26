package com.example.microserve

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class GetHelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gethelp)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.btn_live_chat).setOnClickListener {
            Toast.makeText(this, "Live Chat coming soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btn_call_hotline).setOnClickListener {
            Toast.makeText(this, "Call Hotline coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}
