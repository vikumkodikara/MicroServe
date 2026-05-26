package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class PersonalInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fragment_personal_info)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<MaterialCardView>(R.id.btn_camera).setOnClickListener {
            Toast.makeText(this, "Camera Clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
    }
}
