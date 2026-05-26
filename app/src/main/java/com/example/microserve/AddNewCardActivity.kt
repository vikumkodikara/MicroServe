package com.example.microserve

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class AddNewCardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_addnewcd)

        val etNumber = findViewById<EditText>(R.id.et_card_number)
        val etName = findViewById<EditText>(R.id.et_card_name)
        val etDate = findViewById<EditText>(R.id.et_date)
        val etCvv = findViewById<EditText>(R.id.et_cvv)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.btn_save).setOnClickListener {
            val number = etNumber.text.toString().trim()
            val name = etName.text.toString().trim()
            val date = etDate.text.toString().trim()
            val cvv = etCvv.text.toString().trim()

            if (number.isBlank() || name.isBlank()) {
                Toast.makeText(this, "Please fill in card details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CardStore.addCard(this, number, name, date, cvv)
            Toast.makeText(this, "Card saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
