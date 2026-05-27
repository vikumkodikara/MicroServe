package com.example.microserve

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
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

        val tvNumberPreview = findViewById<TextView>(R.id.tv_card_number_preview)
        val tvNamePreview = findViewById<TextView>(R.id.tv_card_name_preview)
        val tvDatePreview = findViewById<TextView>(R.id.tv_card_date_preview)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        // Live card preview
        etNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val raw = s?.toString()?.replace(" ", "") ?: ""
                if (raw.isEmpty()) {
                    tvNumberPreview.text = "•••• •••• •••• ••••"
                } else {
                    val formatted = raw.chunked(4).joinToString(" ")
                    val padded = formatted.padEnd(19, '•')
                    tvNumberPreview.text = padded
                }
            }
        })

        etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val name = s?.toString()?.trim()
                tvNamePreview.text = if (name.isNullOrBlank()) "YOUR NAME" else name.uppercase()
            }
        })

        etDate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val date = s?.toString()?.trim()
                tvDatePreview.text = if (date.isNullOrBlank()) "MM/YY" else date
            }
        })

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
            Toast.makeText(this, "Card saved successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
