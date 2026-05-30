package com.example.microserve

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

/**
 * Android client wrapper for MicroServe Firebase Callable Functions.
 *
 * Functions bypass Firestore security rules by running with Admin SDK privileges.
 *
 *  [processPayment]  → deducts M-Points from customer, holds in escrow, marks request in_progress
 *  [releasePayment]  → releases escrow to provider, marks request admin_approved
 */
object CloudFunctions {

    private val functions: FirebaseFunctions
        get() = FirebaseFunctions.getInstance()

    // ── Pay Now ───────────────────────────────────────────────────────────────

    /**
     * Calls the 'processPayment' Cloud Function.
     *
     * @param requestId   Firestore document ID of the ServiceRequest
     * @param customerId  UID of the customer (requester) — must match caller's auth UID
     * @param providerId  UID of the service provider
     * @param cost        M-Points amount to transfer
     * @param onSuccess   Called with the created transactionId on success
     * @param onFailure   Called with a human-readable error string on failure
     */
    fun processPayment(
        requestId:  String,
        customerId: String,
        providerId: String,
        cost:       Int,
        onSuccess:  (transactionId: String) -> Unit,
        onFailure:  (String) -> Unit
    ) {
        val data = hashMapOf(
            "requestId"  to requestId,
            "customerId" to customerId,
            "providerId" to providerId,
            "cost"       to cost
        )

        Log.d("CloudFunctions", "processPayment → requestId=$requestId customerId=$customerId providerId=$providerId cost=$cost")

        functions
            .getHttpsCallable("processPayment")
            .call(data)
            .addOnSuccessListener { result ->
                @Suppress("UNCHECKED_CAST")
                val map   = result.data as? Map<String, Any>
                val txnId = map?.get("transactionId") as? String ?: ""
                Log.d("CloudFunctions", "processPayment SUCCESS → transactionId=$txnId")
                onSuccess(txnId)
            }
            .addOnFailureListener { e ->
                val msg = friendlyMessage(e)
                Log.e("CloudFunctions", "processPayment FAILED → $msg", e)
                onFailure(msg)
            }
    }

    // ── Confirm & Release ─────────────────────────────────────────────────────

    /**
     * Calls the 'releasePayment' Cloud Function.
     *
     * @param requestId     Firestore document ID of the ServiceRequest
     * @param transactionId Firestore document ID of the ServiceTransaction
     * @param onSuccess     Called on success
     * @param onFailure     Called with a human-readable error string on failure
     */
    fun releasePayment(
        requestId:     String,
        transactionId: String,
        onSuccess:     () -> Unit,
        onFailure:     (String) -> Unit
    ) {
        val data = hashMapOf(
            "requestId"     to requestId,
            "transactionId" to transactionId
        )

        Log.d("CloudFunctions", "releasePayment → requestId=$requestId transactionId=$transactionId")

        functions
            .getHttpsCallable("releasePayment")
            .call(data)
            .addOnSuccessListener {
                Log.d("CloudFunctions", "releasePayment SUCCESS")
                onSuccess()
            }
            .addOnFailureListener { e ->
                val msg = friendlyMessage(e)
                Log.e("CloudFunctions", "releasePayment FAILED → $msg", e)
                onFailure(msg)
            }
    }

    // ── Error mapper ──────────────────────────────────────────────────────────

    private fun friendlyMessage(e: Exception): String {
        return if (e is FirebaseFunctionsException) {
            val detail = e.message ?: ""
            when (e.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED    ->
                    "Session expired. Please log out and log in again."
                FirebaseFunctionsException.Code.PERMISSION_DENIED  ->
                    "You are not authorised to perform this action."
                FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                    detail.ifBlank { "Action not allowed at this stage." }
                FirebaseFunctionsException.Code.NOT_FOUND           ->
                    "Request or transaction not found in database."
                FirebaseFunctionsException.Code.INVALID_ARGUMENT    ->
                    "Invalid data sent to server: $detail"
                else ->
                    detail.ifBlank { "An unexpected error occurred. Please try again." }
            }
        } else {
            e.localizedMessage ?: "Unknown error."
        }
    }
}
