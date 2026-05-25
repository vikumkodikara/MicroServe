package com.example.microserve

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsFragmentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fragment_settings)

        val root = findViewById<View>(R.id.settingsFragRoot)
        val header = findViewById<View>(R.id.headerContainer)
        val footer = findViewById<View>(R.id.footerBar)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            header.setPadding(0, systemBars.top, 0, 0)
            val lp = footer.layoutParams
            lp.height = (48 * resources.displayMetrics.density).toInt() + systemBars.bottom
            footer.layoutParams = lp
            footer.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        try {
            val title = findViewById<TextView>(R.id.tv_title)
            title?.text = "Settings"
            title?.textSize = 35f
            title?.setTextColor(Color.WHITE)
            title?.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        } catch (_: Exception) { }

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.btn_logout).setOnClickListener {
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<View>(R.id.tv_edit_profile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<View>(R.id.btn_personal_info).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
    }
}
