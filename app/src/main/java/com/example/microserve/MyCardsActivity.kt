package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MyCardsActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_cards)

        container = findViewById(R.id.cardsListContainer)
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadCards()
    }

    private fun loadCards() {
        container.removeAllViews()
        val cards = CardStore.getAllCards(this)

        if (cards.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No cards added yet"
                textSize = 15f
                setTextColor(0xFF777777.toInt())
                setPadding(0, 60, 0, 0)
                gravity = android.view.Gravity.CENTER
            }
            container.addView(empty)
            return
        }

        for (card in cards) {
            val item = LayoutInflater.from(this).inflate(R.layout.item_card, container, false)

            item.findViewById<TextView>(R.id.tv_card_name).text = card.cardName.ifBlank { "Card" }
            val masked = if (card.cardNumber.length >= 4) {
                "**** **** **** ${card.cardNumber.takeLast(4)}"
            } else {
                card.cardNumber
            }
            item.findViewById<TextView>(R.id.tv_card_number).text = masked

            item.setOnClickListener {
                startActivity(
                    Intent(this, EditCardActivity::class.java)
                        .putExtra("card_id", card.id)
                )
            }

            container.addView(item)
        }
    }
}
