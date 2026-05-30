package com.example.microserve

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Seeds demo transactions for the admin dashboard when Firestore has none yet.
 * Runs once per install so real data is never overwritten.
 */
object AdminDashboardSeeder {

    private const val TAG = "AdminDashboardSeeder"
    private const val PREF_NAME = "admin_dashboard_seeder"
    private const val KEY_SEEDED = "demo_seeded_v1"

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    fun seedIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) return

        firestore.collection(ServiceTransaction.COLLECTION)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    prefs.edit().putBoolean(KEY_SEEDED, true).apply()
                    return@addOnSuccessListener
                }
                writeDemoTransactions {
                    prefs.edit().putBoolean(KEY_SEEDED, true).apply()
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Unable to check transactions collection", error)
            }
    }

    private fun writeDemoTransactions(onComplete: () -> Unit) {
        val now = System.currentTimeMillis()
        val day = 86_400_000L

        val demoTransactions = listOf(
            ServiceTransaction(
                transactionCode = "TX-301",
                requestId = "demo-req-1",
                requestTitle = "Home plumbing repair",
                requesterUid = "demo-requester-1",
                requesterName = "Nimali Peris",
                providerUid = "demo-provider-1",
                providerName = "Kamal Gunarathne",
                providerCode = "P1045821",
                amount = 2500,
                status = ServiceTransactionStatus.SUCCESS,
                paidAt = now - day * 5,
                providerDoneAt = now - day * 4,
                requesterConfirmedAt = now - day * 4,
                adminApprovedAt = now - day * 3,
                createdAt = now - day * 5
            ),
            ServiceTransaction(
                transactionCode = "TX-302",
                requestId = "demo-req-2",
                requestTitle = "AC installation",
                requesterUid = "demo-requester-2",
                requesterName = "Sarah Silva",
                providerUid = "demo-provider-2",
                providerName = "Sampath Dahanayake",
                providerCode = "P2093847",
                amount = 4200,
                status = ServiceTransactionStatus.SUCCESS,
                paidAt = now - day * 3,
                providerDoneAt = now - day * 2,
                requesterConfirmedAt = now - day * 2,
                adminApprovedAt = now - day * 1,
                createdAt = now - day * 3
            ),
            ServiceTransaction(
                transactionCode = "TX-303",
                requestId = "demo-req-3",
                requestTitle = "Garden landscaping",
                requesterUid = "demo-requester-3",
                requesterName = "Thakshila Jayaweera",
                providerUid = "demo-provider-1",
                providerName = "Kamal Gunarathne",
                providerCode = "P1045821",
                amount = 1800,
                status = ServiceTransactionStatus.SUCCESS,
                paidAt = now - day * 2,
                providerDoneAt = now - day * 1,
                requesterConfirmedAt = now - day * 1,
                adminApprovedAt = now,
                createdAt = now - day * 2
            ),
            ServiceTransaction(
                transactionCode = "TX-304",
                requestId = "demo-req-4",
                requestTitle = "Electrical wiring",
                requesterUid = "demo-requester-1",
                requesterName = "Nimali Peris",
                providerUid = "demo-provider-2",
                providerName = "Sampath Dahanayake",
                providerCode = "P2093847",
                amount = 3200,
                status = ServiceTransactionStatus.AWAITING_ADMIN,
                paidAt = now - day,
                providerDoneAt = now - day / 2,
                requesterConfirmedAt = now - day / 4,
                createdAt = now - day
            ),
            ServiceTransaction(
                transactionCode = "TX-305",
                requestId = "demo-req-5",
                requestTitle = "House cleaning",
                requesterUid = "demo-requester-2",
                requesterName = "Sarah Silva",
                providerUid = "demo-provider-1",
                providerName = "Kamal Gunarathne",
                providerCode = "P1045821",
                amount = 950,
                status = ServiceTransactionStatus.ESCROW,
                paidAt = now - day / 2,
                createdAt = now - day / 2
            )
        )

        val batch = firestore.batch()
        demoTransactions.forEach { transaction ->
            val docRef = firestore.collection(ServiceTransaction.COLLECTION).document()
            batch.set(docRef, transaction.toMap())
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Demo transactions seeded")
                onComplete()
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Failed to seed demo transactions", error)
            }
    }
}
