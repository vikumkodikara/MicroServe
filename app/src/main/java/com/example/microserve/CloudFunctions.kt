package com.example.microserve

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

/**
 * Thin wrapper around Firebase Callable Functions for M-Points escrow operations.
 *
 * Two functions are exposed:
 *  - [processEscrowPayment]    → deducts M-Points from customer, deposits into escrow
 *  - [releaseEscrowToProvider] → releases escrow to provider after customer confirmation
 *
 * Both run with Admin SDK privileges server-side, so Firestore security rules
 * never block the write operations.
 */
object CloudFunctions {

    private val functions: FirebaseFunctions
        get() = FirebaseFunctions.getInstance()

    // ── Pay Now ───────────────────────────────────────────────────────────────

    /**
     * Asks the Cloud Function to:
     *  1. Verify the caller is the request's requester
     *  2. Deduct [cost] M-Points from the customer
     *  3. Credit escrow
     *  4. Set request status → "in_progress"
     *  5. Create a ServiceTransaction document
     *
     * @param requestId  Firestore document ID of the ServiceRequest
     * @param onSuccess  Called with the newly-created transactionId
     * @param onFailure  Called with a human-readable error message
     */
    fun processEscrowPayment(
        requestId: String,
        onSuccess: (transactionId: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val data = hashMapOf("requestId" to requestId)

        functions
            .getHttpsCallable("processEscrowPayment")
            .call(data)
            .addOnSuccessListener { result ->
                @Suppress("UNCHECKED_CAST")
                val map = result.data as? Map<String, Any>
                val txnId = map?.get("transactionId") as? String ?: ""
                onSuccess(txnId)
            }
            .addOnFailureListener { e ->
                onFailure(friendlyMessage(e))
            }
    }

    // ── Confirm & Release ─────────────────────────────────────────────────────

    /**
     * Asks the Cloud Function to:
     *  1. Verify the caller is the request's requester
     *  2. Transfer M-Points from escrow to provider
     *  3. Set request status → "admin_approved"
     *  4. Mark the ServiceTransaction as "completed"
     *
     * @param requestId     Firestore document ID of the ServiceRequest
     * @param transactionId Firestore document ID of the linked ServiceTransaction
     * @param onSuccess     Called on success
     * @param onFailure     Called with a human-readable error message
     */
    fun releaseEscrowToProvider(
        requestId: String,
        transactionId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val data = hashMapOf(
            "requestId"     to requestId,
            "transactionId" to transactionId
        )

        functions
            .getHttpsCallable("releaseEscrowToProvider")
            .call(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(friendlyMessage(e)) }
    }

    // ── Error helper ──────────────────────────────────────────────────────────

    private fun friendlyMessage(e: Exception): String {
        return if (e is FirebaseFunctionsException) {
            when (e.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED   -> "Please log in first."
                FirebaseFunctionsException.Code.PERMISSION_DENIED -> "You are not authorised for this action."
                FirebaseFunctionsException.Code.FAILED_PRECONDITION -> e.message ?: "Action not allowed at this stage."
                FirebaseFunctionsException.Code.NOT_FOUND          -> "Request or transaction not found."
                else -> e.message ?: "An unexpected error occurred."
            }
        } else {
            e.localizedMessage ?: "Unknown error."
        }
    }
}
