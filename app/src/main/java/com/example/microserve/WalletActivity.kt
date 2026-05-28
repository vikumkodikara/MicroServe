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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.util.Locale

class WalletActivity : AppCompatActivity() {

    private lateinit var tvPoints: TextView
    private var balanceListener: ListenerRegistration? = null

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
        refreshBalanceFromFirestore()
    }

    override fun onStart() {
        super.onStart()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        balanceListener?.remove()
        balanceListener = PointsRepository.listenBalance(
            uid = uid,
            onUpdate = { points ->
                tvPoints.text = "M ${formatNumber(points)}"
                AppPreferences.setMPoints(this, points)
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onStop() {
        balanceListener?.remove()
        balanceListener = null
        super.onStop()
    }

    private fun refreshBalanceFromFirestore() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        PointsRepository.getBalance(
            uid = uid,
            onSuccess = { balance ->
                tvPoints.text = "M ${formatNumber(balance)}"
                AppPreferences.setMPoints(this, balance)
            },
            onFailure = {
                val fallback = AppPreferences.getMPoints(this)
                tvPoints.text = "M ${formatNumber(fallback)}"
            }
        )
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

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid.isNullOrBlank()) {
                Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            PointsRepository.topUp(
                uid = uid,
                amount = amount,
                onSuccess = { newBalance ->
                    AppPreferences.setMPoints(this, newBalance)
                    dialog.dismiss()
                    tvPoints.text = "M ${formatNumber(newBalance)}"
                    tvCurrentBalance.text = "Current balance: M ${formatNumber(newBalance)}"

                    val cardName = cards.first().cardName
                    Toast.makeText(
                        this,
                        "Rs. ${formatNumber(amount)} debited from $cardName.\nM Points added successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onFailure = { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            )
        }

        dialog.show()
    }

    private fun formatNumber(number: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(number)
    }
}
