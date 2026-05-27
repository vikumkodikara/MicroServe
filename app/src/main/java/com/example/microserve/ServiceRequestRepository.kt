package com.example.microserve

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object ServiceRequestRepository {

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private fun collection() = firestore.collection(ServiceRequest.COLLECTION)

    fun create(
        request: ServiceRequest,
        onSuccess: (ServiceRequest) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val docRef = if (request.id.isBlank()) collection().document() else collection().document(request.id)
        val payload = request.copy(id = docRef.id, updatedAt = System.currentTimeMillis())
        docRef.set(payload.toMap())
            .addOnSuccessListener { onSuccess(payload) }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to create request") }
    }

    fun update(
        requestId: String,
        fields: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updates = fields.toMutableMap()
        updates[ServiceRequest.FIELD_UPDATED_AT] = System.currentTimeMillis()
        collection().document(requestId).update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to update request") }
    }

    fun delete(
        requestId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        collection().document(requestId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to delete request") }
    }

    fun getById(
        requestId: String,
        onSuccess: (ServiceRequest) -> Unit,
        onFailure: (String) -> Unit
    ) {
        collection().document(requestId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onFailure("Request not found")
                    return@addOnSuccessListener
                }
                onSuccess(ServiceRequest.fromMap(doc.id, doc.data.orEmpty()))
            }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Failed to load request") }
    }

    fun listenById(
        requestId: String,
        onUpdate: (ServiceRequest) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return collection().document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Failed to listen")
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    onError("Request not found")
                    return@addSnapshotListener
                }
                onUpdate(ServiceRequest.fromMap(snapshot.id, snapshot.data.orEmpty()))
            }
    }

    fun listenByRequester(
        requesterUid: String,
        onUpdate: (List<ServiceRequest>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return collection()
            .whereEqualTo(ServiceRequest.FIELD_REQUESTER_UID, requesterUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Failed to load requests")
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.map { doc ->
                    ServiceRequest.fromMap(doc.id, doc.data.orEmpty())
                }.orEmpty().sortedByDescending { it.createdAt }
                onUpdate(items)
            }
    }

    fun listenOpenByCategories(
        storeKeys: List<String>,
        onUpdate: (List<ServiceRequest>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        val normalized = storeKeys.map { it.trim().lowercase() }
        return collection()
            .whereEqualTo(ServiceRequest.FIELD_STATUS, ServiceRequestStatus.OPEN)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Failed to load category requests")
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.map { doc ->
                    ServiceRequest.fromMap(doc.id, doc.data.orEmpty())
                }.orEmpty()
                    .filter { req -> req.category.trim().lowercase() in normalized }
                    .sortedByDescending { it.createdAt }
                onUpdate(items)
            }
    }

    fun listenByProvider(
        providerUid: String,
        onUpdate: (List<ServiceRequest>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return collection()
            .whereEqualTo(ServiceRequest.FIELD_ACCEPTED_PROVIDER_UID, providerUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Failed to load jobs")
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.map { doc ->
                    ServiceRequest.fromMap(doc.id, doc.data.orEmpty())
                }.orEmpty().sortedByDescending { it.updatedAt }
                onUpdate(items)
            }
    }
}
