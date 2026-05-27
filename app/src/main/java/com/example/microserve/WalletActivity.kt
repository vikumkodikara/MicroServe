package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class WalletActivity : AppCompatActivity() {

    private var balanceListener: ListenerRegistration? = null

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

        findViewById<View>(R.id.btn_add_points).setOnClickListener {
            showAddPointsDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            findViewById<android.widget.TextView>(R.id.tv_points).text = "0"
            return
        }
        balanceListener?.remove()
        balanceListener = PointsRepository.listenBalance(
            uid = uid,
            onUpdate = { balance ->
                findViewById<android.widget.TextView>(R.id.tv_points).text = balance.toString()
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

    private fun showAddPointsDialog() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show()
            return
        }

        val cards = CardStore.getAllCards(this)
        if (cards.isEmpty()) {
            Toast.makeText(this, R.string.add_card_first, Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AddNewCardActivity::class.java))
            return
        }

        val cardLabels = cards.map { "${it.cardName} •••• ${it.cardNumber.takeLast(4)}" }.toTypedArray()
        var selectedIndex = 0

        val amountInput = EditText(this).apply {
            hint = getString(R.string.top_up_amount_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(48, 32, 48, 16)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.add_points_title)
            .setSingleChoiceItems(cardLabels, 0) { _, which -> selectedIndex = which }
            .setView(amountInput)
            .setPositiveButton(R.string.add_points_confirm) { dialog, _ ->
                val amount = amountInput.text.toString().trim().toIntOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, R.string.invalid_top_up_amount, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                PointsRepository.topUp(
                    uid = uid,
                    amount = amount,
                    onSuccess = { newBalance ->
                        Toast.makeText(
                            this,
                            getString(R.string.top_up_success, newBalance),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = { message ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                )
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
