package com.example.microserve

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.util.Locale

class WalletActivity : AppCompatActivity() {

    private lateinit var tvPoints: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        SystemUiHelper.setupPurpleHeaderScreen(
            activity = this,
            root = findViewById(R.id.walletRoot),
            headerView = findViewById(R.id.headerContainer),
            footerBar = findViewById(R.id.footerBar)
        )

        tvPoints = findViewById(R.id.tv_points)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<View>(R.id.btn_add_points).setOnClickListener {
            showAddPointsDialog()
        }

        findViewById<View>(R.id.btn_cards).setOnClickListener {
            startActivity(Intent(this, MyCardsActivity::class.java))
        }

        findViewById<View>(R.id.btn_add_card).setOnClickListener {
            startActivity(Intent(this, AddNewCardActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshBalance()
    }

    private fun refreshBalance() {
        val balance = AppPreferences.getMPoints(this)
        tvPoints.text = "M ${formatNumber(balance)}"
    }

    private fun showAddPointsDialog() {
        val dialog = Dialog(this, com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog)
        dialog.setContentView(R.layout.dialog_add_points)

        dialog.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawableResource(android.R.color.transparent)
            attributes = attributes.also {
                it.windowAnimations = com.google.android.material.R.style.Animation_Design_BottomSheetDialog
            }
        }

        val etAmount = dialog.findViewById<EditText>(R.id.et_amount)
        val tvCurrentBalance = dialog.findViewById<TextView>(R.id.tv_current_balance)
        val cardInfoRow = dialog.findViewById<View>(R.id.card_info_row)
        val tvCardInfo = dialog.findViewById<TextView>(R.id.tv_card_info)
        val tvNoCard = dialog.findViewById<TextView>(R.id.tv_no_card)
        val btnAddToWallet = dialog.findViewById<View>(R.id.btn_add_to_wallet)

        // Show current balance
        val currentBalance = AppPreferences.getMPoints(this)
        tvCurrentBalance.text = "Current balance: M ${formatNumber(currentBalance)}"

        // Show card info or warning
        val cards = CardStore.getAllCards(this)
        if (cards.isNotEmpty()) {
            val card = cards.first()
            val lastFour = card.cardNumber.takeLast(4)
            tvCardInfo.text = "•••• •••• •••• $lastFour  (${card.cardName})"
            cardInfoRow.visibility = View.VISIBLE
            tvNoCard.visibility = View.GONE
        } else {
            cardInfoRow.visibility = View.GONE
            tvNoCard.visibility = View.VISIBLE
        }

        // Quick amount chips
        dialog.findViewById<View>(R.id.chip_500).setOnClickListener {
            etAmount.setText("500")
            etAmount.setSelection(etAmount.text.length)
        }
        dialog.findViewById<View>(R.id.chip_1000).setOnClickListener {
            etAmount.setText("1000")
            etAmount.setSelection(etAmount.text.length)
        }
        dialog.findViewById<View>(R.id.chip_2000).setOnClickListener {
            etAmount.setText("2000")
            etAmount.setSelection(etAmount.text.length)
        }
        dialog.findViewById<View>(R.id.chip_5000).setOnClickListener {
            etAmount.setText("5000")
            etAmount.setSelection(etAmount.text.length)
        }

        // Add to wallet button
        btnAddToWallet.setOnClickListener {
            val amountStr = etAmount.text.toString().trim()
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toIntOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cards.isEmpty()) {
                Toast.makeText(this, "Please add a card first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add points
            val newBalance = AppPreferences.addMPoints(this, amount)
            dialog.dismiss()

            // Refresh balance on wallet
            refreshBalance()

            // Show success notification
            val cardName = cards.first().cardName
            Toast.makeText(
                this,
                "Rs. ${formatNumber(amount)} debited from $cardName.\nM Points added successfully!",
                Toast.LENGTH_LONG
            ).show()
        }

        dialog.show()
    }

    private fun formatNumber(number: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(number)
    }
}
