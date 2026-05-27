package com.example.microserve

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object BidRepository {

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private fun bidsCollection(requestId: String) =
        firestore.collection(ServiceRequest.COLLECTION)
            .document(requestId)
            .collection(Bid.SUBCOLLECTION)

    fun placeBid(
        requestId: String,
        bid: Bid,
        onSuccess: (Bid) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val docRef = bidsCollection(requestId).document()
        val payload = bid.copy(id = docRef.id, requestId = requestId)
        docRef.set(payload.toMap())
            .addOnSuccessListener { onSuccess(payload) }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to place bid") }
    }

    fun listenBids(
        requestId: String,
        onUpdate: (List<Bid>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return bidsCollection(requestId)
            .orderBy(Bid.FIELD_CREATED_AT, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Failed to load bids")
                    return@addSnapshotListener
                }
                val bids = snapshot?.documents?.map { doc ->
                    Bid.fromMap(doc.id, doc.data.orEmpty())
                }.orEmpty()
                onUpdate(bids)
            }
    }

    fun acceptBid(
        requestId: String,
        bid: Bid,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        bidsCollection(requestId).get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                val requestRef = firestore.collection(ServiceRequest.COLLECTION).document(requestId)

                batch.update(
                    requestRef,
                    mapOf(
                        ServiceRequest.FIELD_STATUS to ServiceRequestStatus.BID_SELECTED,
                        ServiceRequest.FIELD_ACCEPTED_BID_ID to bid.id,
                        ServiceRequest.FIELD_ACCEPTED_PROVIDER_UID to bid.providerUid,
                        ServiceRequest.FIELD_ACCEPTED_PROVIDER_NAME to bid.providerName,
                        ServiceRequest.FIELD_ACCEPTED_POINTS to bid.points,
                        ServiceRequest.FIELD_UPDATED_AT to System.currentTimeMillis()
                    )
                )

                snapshot.documents.forEach { doc ->
                    val status = if (doc.id == bid.id) BidStatus.ACCEPTED else BidStatus.REJECTED
                    batch.update(doc.reference, Bid.FIELD_STATUS, status)
                }

                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to accept bid") }
            }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to load bids") }
    }
}
