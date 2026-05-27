package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class WalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_wallet)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.btn_add_card).setOnClickListener {
            startActivity(Intent(this, AddNewCardActivity::class.java))
        }

        findViewById<View>(R.id.btn_cards).setOnClickListener {
            startActivity(Intent(this, MyCardsActivity::class.java))
        }
    }
}
