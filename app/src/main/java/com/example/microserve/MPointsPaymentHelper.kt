package com.example.microserve

import android.app.Activity
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import java.text.NumberFormat
import java.util.Locale

/**
 * Reusable helper that shows a payment confirmation dialog
 * and deducts M Points from the user's wallet.
 */
object MPointsPaymentHelper {

    fun showPaymentDialog(
        activity: Activity,
        amount: Int,
        providerName: String,
        onSuccess: () -> Unit
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

        val currentBalance = AppPreferences.getMPoints(activity)
        val balanceAfter = currentBalance - amount

        dialog.findViewById<TextView>(R.id.tv_balance).text = "M ${formatNumber(currentBalance)}"
        dialog.findViewById<TextView>(R.id.tv_amount).text = "M ${formatNumber(amount)}"

        val tvBalanceAfter = dialog.findViewById<TextView>(R.id.tv_balance_after)
        val tvInsufficient = dialog.findViewById<TextView>(R.id.tv_insufficient)
        val btnConfirm = dialog.findViewById<View>(R.id.btn_confirm_pay)

        if (balanceAfter >= 0) {
            tvBalanceAfter.text = "M ${formatNumber(balanceAfter)}"
            tvBalanceAfter.setTextColor(0xFF81C784.toInt())
            tvInsufficient.visibility = View.GONE
            btnConfirm.alpha = 1f
            btnConfirm.isEnabled = true
        } else {
            tvBalanceAfter.text = "M ${formatNumber(balanceAfter)}"
            tvBalanceAfter.setTextColor(0xFFFF6B6B.toInt())
            tvInsufficient.visibility = View.VISIBLE
            btnConfirm.alpha = 0.5f
            btnConfirm.isEnabled = false
        }

        btnConfirm.setOnClickListener {
            if (balanceAfter < 0) {
                Toast.makeText(activity, "Insufficient M Points! Please top up.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Deduct M Points
            AppPreferences.setMPoints(activity, balanceAfter)
            dialog.dismiss()

            Toast.makeText(
                activity,
                "M ${formatNumber(amount)} paid to $providerName.\nRemaining balance: M ${formatNumber(balanceAfter)}",
                Toast.LENGTH_LONG
            ).show()

            onSuccess()
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
