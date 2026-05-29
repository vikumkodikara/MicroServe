package com.example.microserve

import android.os.Handler
import android.os.Looper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ServiceRequestRepository {

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun collection() = firestore.collection(ServiceRequest.COLLECTION)

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    /**
     * Saves a request and completes when the write is on device (offline cache) or server.
     * If the server is slow, we still succeed when the document is visible in local cache.
     */
    suspend fun createAwait(request: ServiceRequest): ServiceRequest {
        val docRef = if (request.id.isBlank()) {
            collection().document()
        } else {
            collection().document(request.id)
        }
        val now = System.currentTimeMillis()
        val payload = request.copy(
            id = docRef.id,
            createdAt = if (request.createdAt > 0L) request.createdAt else now,
            updatedAt = now
        )
        val data = payload.toMap()
        return suspendCancellableCoroutine { cont ->
            docRef.set(data)
                .addOnSuccessListener { cont.resume(payload) }
                .addOnFailureListener { error ->
                    if (cont.isCancelled) return@addOnFailureListener
                    docRef.get(Source.CACHE)
                        .addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                cont.resume(payload)
                            } else {
                                cont.resumeWithException(
                                    error ?: Exception("Failed to create request")
                                )
                            }
                        }
                        .addOnFailureListener {
                            cont.resumeWithException(
                                error ?: Exception("Failed to create request")
                            )
                        }
                }
        }
    }

    suspend fun updateAwait(requestId: String, fields: Map<String, Any>) {
        val updates = fields.toMutableMap()
        updates[ServiceRequest.FIELD_UPDATED_AT] = System.currentTimeMillis()
        val docRef = collection().document(requestId)
        suspendCancellableCoroutine { cont ->
            docRef.update(updates)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { error ->
                    if (cont.isCancelled) return@addOnFailureListener
                    docRef.get(Source.CACHE)
                        .addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                cont.resume(Unit)
                            } else {
                                cont.resumeWithException(
                                    error ?: Exception("Failed to update request")
                                )
                            }
                        }
                        .addOnFailureListener {
                            cont.resumeWithException(
                                error ?: Exception("Failed to update request")
                            )
                        }
                }
        }
    }

    fun create(
        request: ServiceRequest,
        onSuccess: (ServiceRequest) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val docRef = if (request.id.isBlank()) collection().document() else collection().document(request.id)
        val now = System.currentTimeMillis()
        val payload = request.copy(
            id = docRef.id,
            createdAt = if (request.createdAt > 0L) request.createdAt else now,
            updatedAt = now
        )
        docRef.set(payload.toMap())
            .addOnCompleteListener { task ->
                onMain {
                    if (task.isSuccessful) {
                        onSuccess(payload)
                    } else {
                        onFailure(task.exception?.localizedMessage ?: "Failed to create request")
                    }
                }
            }
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
            .addOnCompleteListener { task ->
                onMain {
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        onFailure(task.exception?.localizedMessage ?: "Failed to update request")
                    }
                }
            }
    }

    fun delete(
        requestId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        collection().document(requestId).delete()
            .addOnCompleteListener { task ->
                onMain {
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        onFailure(task.exception?.localizedMessage ?: "Failed to delete request")
                    }
                }
            }
    }

    fun getById(
        requestId: String,
        onSuccess: (ServiceRequest) -> Unit,
        onFailure: (String) -> Unit
    ) {
        collection().document(requestId).get()
            .addOnCompleteListener { task ->
                onMain {
                    if (!task.isSuccessful) {
                        onFailure(task.exception?.localizedMessage ?: "Failed to load request")
                        return@onMain
                    }
                    val doc = task.result
                    if (doc == null || !doc.exists()) {
                        onFailure("Request not found")
                    } else {
                        onSuccess(ServiceRequest.fromMap(doc.id, doc.data.orEmpty()))
                    }
                }
            }
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
