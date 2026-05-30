package com.example.microserve

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object TransactionRepository {

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private fun collection() = firestore.collection(ServiceTransaction.COLLECTION)

    fun createEscrowTransaction(
        transaction: ServiceTransaction,
        onSuccess: (ServiceTransaction) -> Unit,
        onFailure: (String) -> Unit
    ) {
        collection().get()
            .addOnSuccessListener { snapshot ->
                val docRef = collection().document()
                val code = ServiceTransaction.generateTransactionCode(snapshot.size())
                val payload = transaction.copy(
                    id = docRef.id,
                    transactionCode = code,
                    status = ServiceTransactionStatus.ESCROW,
                    paidAt = System.currentTimeMillis()
                )
                docRef.set(payload.toMap())
                    .addOnSuccessListener { onSuccess(payload) }
                    .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to create transaction") }
            }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to create transaction") }
    }

    fun getById(
        transactionId: String,
        onSuccess: (ServiceTransaction) -> Unit,
        onFailure: (String) -> Unit
    ) {
        collection().document(transactionId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onFailure("Transaction not found")
                    return@addOnSuccessListener
                }
                onSuccess(ServiceTransaction.fromMap(doc.id, doc.data.orEmpty()))
            }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to load transaction") }
    }

    fun listenPending(
        onUpdate: (List<ServiceTransaction>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return collection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Failed to load transactions")
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.map { doc ->
                    ServiceTransaction.fromMap(doc.id, doc.data.orEmpty())
                }.orEmpty()
                    .filter {
                        it.status == ServiceTransactionStatus.ESCROW ||
                            it.status == ServiceTransactionStatus.AWAITING_ADMIN
                    }
                    .sortedByDescending { it.createdAt }
                onUpdate(items)
            }
    }

    fun listenSuccess(
        onUpdate: (List<ServiceTransaction>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return collection()
            .whereEqualTo(ServiceTransaction.FIELD_STATUS, ServiceTransactionStatus.SUCCESS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Failed to load transactions")
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.map { doc ->
                    ServiceTransaction.fromMap(doc.id, doc.data.orEmpty())
                }.orEmpty().sortedByDescending { it.createdAt }
                onUpdate(items)
            }
    }

    fun markProviderDone(
        transactionId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        collection().document(transactionId).update(
            mapOf(
                ServiceTransaction.FIELD_PROVIDER_DONE_AT to System.currentTimeMillis()
            )
        ).addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Update failed") }
    }

    fun markRequesterConfirmed(
        transactionId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        collection().document(transactionId).update(
            mapOf(
                ServiceTransaction.FIELD_REQUESTER_CONFIRMED_AT to System.currentTimeMillis(),
                ServiceTransaction.FIELD_STATUS to ServiceTransactionStatus.AWAITING_ADMIN
            )
        ).addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Update failed") }
    }

    fun approveTransaction(
        transaction: ServiceTransaction,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        PointsRepository.approveProviderPayout(
            providerUid = transaction.providerUid,
            amount = transaction.amount,
            onSuccess = {
                collection().document(transaction.id).update(
                    mapOf(
                        ServiceTransaction.FIELD_STATUS to ServiceTransactionStatus.SUCCESS,
                        ServiceTransaction.FIELD_ADMIN_APPROVED_AT to System.currentTimeMillis()
                    )
                ).addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to approve") }
            },
            onFailure = onFailure
        )
    }
}
