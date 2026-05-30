package com.example.microserve

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class EditCardActivity : AppCompatActivity() {

    private var cardId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_card)

        cardId = intent.getStringExtra("card_id") ?: ""

        val etCardNumber = findViewById<EditText>(R.id.et_card_number)
        val etCardName = findViewById<EditText>(R.id.et_card_name)
        val etDate = findViewById<EditText>(R.id.et_date)
        val etCvv = findViewById<EditText>(R.id.et_cvv)

        val card = CardStore.getAllCards(this).find { it.id == cardId }
        if (card != null) {
            etCardNumber.setText(card.cardNumber)
            etCardName.setText(card.cardName)
            etDate.setText(card.date)
            etCvv.setText(card.cvv)
        }

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.btn_update).setOnClickListener {
            val number = etCardNumber.text.toString().trim()
            val name = etCardName.text.toString().trim()
            val date = etDate.text.toString().trim()
            val cvv = etCvv.text.toString().trim()

            if (number.isBlank() || name.isBlank()) {
                Toast.makeText(this, "Please fill in card details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CardStore.updateCard(this, cardId, number, name, date, cvv)
            Toast.makeText(this, "Card updated", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<View>(R.id.btn_delete).setOnClickListener {
            showDeleteDialog()
        }
    }

    private fun showDeleteDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_delete_card)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<MaterialButton>(R.id.btn_yes_delete).setOnClickListener {
            CardStore.deleteCard(this, cardId)
            Toast.makeText(this, "Card deleted", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            finish()
        }

        dialog.findViewById<MaterialButton>(R.id.btn_no).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
