package com.example.microserve

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BillActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.statusBarColor = Color.TRANSPARENT
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.statusBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_bill)

        val btnNext: Button = findViewById(R.id.btnNext)
        val txtBillProvider: TextView = findViewById(R.id.txtBillProvider)
        val txtBillService: TextView = findViewById(R.id.txtBillService)
        val txtBillTotal: TextView = findViewById(R.id.txtBillTotal)
        val txtBillClient: TextView = findViewById(R.id.txtBillClient)

        val providerName = intent.getStringExtra("PROVIDER_NAME") ?: "Unknown Provider"
        val category = intent.getStringExtra("CATEGORY") ?: "Service"
        val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: ""

        // If we have a transaction ID, load full details from Firebase
        if (transactionId.isNotBlank()) {
            TransactionRepository.getById(
                transactionId = transactionId,
                onSuccess = { transaction ->
                    txtBillProvider.text = "Service Provider: ${transaction.providerName}"
                    txtBillService.text = "Service: ${transaction.requestTitle}"
                    txtBillClient.text = "Client: ${transaction.requesterName}"
                    txtBillTotal.text = "Total: ${transaction.amount} M Points"
                    
                    btnNext.setOnClickListener {
                        val intent = Intent(this, RatingActivity::class.java)
                        intent.putExtra("PROVIDER_NAME", transaction.providerName)
                        intent.putExtra("PROVIDER_UID", transaction.providerUid)
                        intent.putExtra("REQUEST_ID", transaction.requestId)
                        startActivity(intent)
                    }
                },
                onFailure = { error ->
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            // Fallback to intent extras for backward compatibility
            txtBillProvider.text = "Service Provider: $providerName"
            txtBillService.text = "Service: $category"
            
            btnNext.setOnClickListener {
                val intent = Intent(this, RatingActivity::class.java)
                intent.putExtra("PROVIDER_NAME", providerName)
                startActivity(intent)
            }
        }
    }
}
