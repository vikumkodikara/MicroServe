package com.example.microserve

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.util.Locale

/**
 * Shows a payment confirmation bottom sheet using live Firestore M Points balance,
 * then invokes [onConfirm] so the caller can run the server-side escrow payment.
 */
object MPointsPaymentHelper {

    fun formatMPoints(amount: Int): String = "M ${formatNumber(amount)}"

    fun showPaymentDialog(
        activity: Activity,
        amount: Int,
        providerName: String,
        onConfirm: () -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            Toast.makeText(activity, R.string.login_required, Toast.LENGTH_SHORT).show()
            return
        }

        PointsRepository.getBalance(
            uid = uid,
            onSuccess = { balance ->
                activity.runOnUiThread {
                    showDialogWithBalance(activity, amount, providerName, balance, onConfirm)
                }
            },
            onFailure = { message ->
                activity.runOnUiThread {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showDialogWithBalance(
        activity: Activity,
        amount: Int,
        providerName: String,
        currentBalance: Int,
        onConfirm: () -> Unit
    ) {
        val dialog = Dialog(activity, com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog)
        dialog.setContentView(R.layout.dialog_confirm_payment)

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

        val balanceAfter = currentBalance - amount
        val hasSufficientBalance = balanceAfter >= 0

        dialog.findViewById<TextView>(R.id.tv_balance).text = formatMPoints(currentBalance)
        dialog.findViewById<TextView>(R.id.tv_amount).text = formatMPoints(amount)

        val tvBalanceAfter = dialog.findViewById<TextView>(R.id.tv_balance_after)
        val tvInsufficient = dialog.findViewById<TextView>(R.id.tv_insufficient)
        val btnTopUp = dialog.findViewById<View>(R.id.btn_top_up_wallet)
        val btnConfirm = dialog.findViewById<View>(R.id.btn_confirm_pay)

        tvBalanceAfter.text = formatMPoints(balanceAfter)
        if (hasSufficientBalance) {
            tvBalanceAfter.setTextColor(0xFF81C784.toInt())
            tvInsufficient.visibility = View.GONE
            btnTopUp.visibility = View.GONE
            btnConfirm.alpha = 1f
            btnConfirm.isEnabled = true
        } else {
            tvBalanceAfter.setTextColor(0xFFFF6B6B.toInt())
            tvInsufficient.visibility = View.VISIBLE
            btnTopUp.visibility = View.VISIBLE
            btnConfirm.alpha = 0.5f
            btnConfirm.isEnabled = false
        }

        btnTopUp.setOnClickListener {
            dialog.dismiss()
            activity.startActivity(Intent(activity, WalletActivity::class.java))
        }

        btnConfirm.setOnClickListener {
            if (!hasSufficientBalance) {
                Toast.makeText(activity, R.string.insufficient_points, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            onConfirm()
        }

        dialog.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun formatNumber(number: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(number)
    }
}
