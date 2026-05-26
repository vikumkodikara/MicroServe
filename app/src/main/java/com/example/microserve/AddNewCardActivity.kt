package com.example.microserve

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class AddNewCardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_addnewcd)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.btn_save).setOnClickListener {
            Toast.makeText(this, "Card saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
