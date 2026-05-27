package com.example.microserve

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.TextView

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
        findViewById<View>(R.id.btn_live_chat).setOnClickListener {
            Toast.makeText(this, "Live Chat coming soon", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btn_call_hotline).setOnClickListener {
            Toast.makeText(this, "Call Hotline coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}
