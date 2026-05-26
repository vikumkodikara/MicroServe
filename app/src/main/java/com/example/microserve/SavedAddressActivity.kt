package com.example.microserve

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SavedAddressActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fragment_saved_address)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btn_add_address).setOnClickListener {
            Toast.makeText(this, "Add New Address", Toast.LENGTH_SHORT).show()
        }
    }
}
