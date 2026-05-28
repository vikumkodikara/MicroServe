package com.example.microserve

import com.google.firebase.auth.FirebaseAuth
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

    fun updateBid(
        requestId: String,
        bidId: String,
        points: Int,
        completionHours: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            onFailure("Please log in first")
            return
        }

        val requestRef = firestore.collection(ServiceRequest.COLLECTION).document(requestId)
        val bidRef = bidsCollection(requestId).document(bidId)

        requestRef.get()
            .addOnSuccessListener { requestSnap ->
                if (!requestSnap.exists()) {
                    onFailure("Request not found")
                    return@addOnSuccessListener
                }

                val request = ServiceRequest.fromMap(requestSnap.id, requestSnap.data.orEmpty())
                if (request.status != ServiceRequestStatus.OPEN) {
                    onFailure("This request is no longer open for bids")
                    return@addOnSuccessListener
                }

                bidRef.get()
                    .addOnSuccessListener { bidSnap ->
                        if (!bidSnap.exists()) {
                            onFailure("Bid not found")
                            return@addOnSuccessListener
                        }

                        val bid = Bid.fromMap(bidSnap.id, bidSnap.data.orEmpty())
                        if (bid.providerUid != uid) {
                            onFailure("You can only edit your own bid")
                            return@addOnSuccessListener
                        }
                        if (bid.status != BidStatus.PENDING) {
                            onFailure("This bid can no longer be edited")
                            return@addOnSuccessListener
                        }

                        bidRef.update(
                            mapOf(
                                Bid.FIELD_POINTS to points,
                                Bid.FIELD_COMPLETION_HOURS to completionHours
                            )
                        )
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener {
                                onFailure(it.localizedMessage ?: "Failed to update bid")
                            }
                    }
                    .addOnFailureListener {
                        onFailure(it.localizedMessage ?: "Failed to load bid")
                    }
            }
            .addOnFailureListener {
                onFailure(it.localizedMessage ?: "Failed to load request")
            }
    }
}
