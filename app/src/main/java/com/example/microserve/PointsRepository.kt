package com.example.microserve

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object PointsRepository {

    private const val PLATFORM_DOC = "platform"
    private const val ESCROW_DOC = "escrow"
    private const val FIELD_POINTS = "points"
    private const val FIELD_ESCROW_POINTS = "escrowPoints"

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private fun userRef(uid: String) =
        firestore.collection(UserProfile.COLLECTION).document(uid)

    private fun escrowRef() =
        firestore.collection(PLATFORM_DOC).document(ESCROW_DOC)

    fun getBalance(
        uid: String,
        onSuccess: (Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        userRef(uid).get()
            .addOnSuccessListener { doc ->
                val points = doc.getLong(UserProfile.FIELD_CASH_POINTS)?.toInt() ?: 0
                onSuccess(points)
            }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to load balance") }
    }

    fun listenBalance(
        uid: String,
        onUpdate: (Int) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return userRef(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.localizedMessage ?: "Failed to listen balance")
                return@addSnapshotListener
            }
            val points = snapshot?.getLong(UserProfile.FIELD_CASH_POINTS)?.toInt() ?: 0
            onUpdate(points)
        }
    }

    fun topUp(
        uid: String,
        amount: Int,
        onSuccess: (Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (amount <= 0) {
            onFailure("Amount must be greater than zero")
            return
        }

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef(uid))
            val current = snapshot.getLong(UserProfile.FIELD_CASH_POINTS)?.toInt() ?: 0
            val updated = current + amount
            transaction.update(userRef(uid), UserProfile.FIELD_CASH_POINTS, updated)
            updated
        }.addOnSuccessListener { newBalance -> onSuccess(newBalance) }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Top-up failed") }
    }

    fun processEscrowPayment(
        requesterUid: String,
        amount: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (amount <= 0) {
            onFailure("Invalid payment amount")
            return
        }

        firestore.runTransaction { transaction ->
            val userSnap = transaction.get(userRef(requesterUid))
            val current = userSnap.getLong(UserProfile.FIELD_CASH_POINTS)?.toInt() ?: 0
            if (current < amount) {
                throw IllegalStateException("Insufficient M Points balance")
            }
            transaction.update(userRef(requesterUid), UserProfile.FIELD_CASH_POINTS, current - amount)

            val escrowSnap = transaction.get(escrowRef())
            val escrow = if (escrowSnap.exists()) {
                escrowSnap.getLong(FIELD_ESCROW_POINTS)?.toInt() ?: 0
            } else {
                0
            }
            transaction.set(
                escrowRef(),
                mapOf(FIELD_ESCROW_POINTS to escrow + amount),
                com.google.firebase.firestore.SetOptions.merge()
            )
            null
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                onFailure(error.localizedMessage ?: "Payment failed")
            }
    }

    fun approveProviderPayout(
        providerUid: String,
        amount: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (amount <= 0) {
            onFailure("Invalid payout amount")
            return
        }

        firestore.runTransaction { transaction ->
            val escrowSnap = transaction.get(escrowRef())
            val escrow = escrowSnap.getLong(FIELD_ESCROW_POINTS)?.toInt() ?: 0
            if (escrow < amount) {
                throw IllegalStateException("Insufficient escrow balance")
            }
            transaction.update(escrowRef(), FIELD_ESCROW_POINTS, escrow - amount)

            val providerSnap = transaction.get(userRef(providerUid))
            val providerPoints = providerSnap.getLong(UserProfile.FIELD_CASH_POINTS)?.toInt() ?: 0
            transaction.update(userRef(providerUid), UserProfile.FIELD_CASH_POINTS, providerPoints + amount)
            null
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Payout failed") }
    }
}
