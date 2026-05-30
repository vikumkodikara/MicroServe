package com.example.microserve

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Service provider listings backed by Firebase Firestore with a local
 * SharedPreferences cache for offline access.
 */
object ServiceStore {

    private const val TAG = "ServiceStore"
    const val EXTRA_SERVICE_ID = "service_id"
    private const val COLLECTION = "services"

    data class Service(
        val id: String,
        val title: String,
        val category: String,
        val providerName: String,
        val contact: String,
        val location: String,
        val status: String = STATUS_ACTIVE,
        val email: String = "",
        val imageUri: String? = null,
        val ownerUid: String = "",
        val isActive: Boolean = true
    )

    private const val PREF_NAME = "service_store"
    private const val KEY_SERVICES = "services_json"

    const val STATUS_ACTIVE = "Active"
    const val STATUS_PENDING = "Pending Approval"
    const val STATUS_COMPLETED = "Completed"

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private var snapshotListener: ListenerRegistration? = null

    // ── Firestore Real-time Sync ────────────────────────────────

    fun startListening(context: Context) {
        if (snapshotListener != null) return

        snapshotListener = firestore.collection(COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore listen failed", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val services = snapshot.documents.mapNotNull { doc ->
                    try {
                        Service(
                            id = doc.id,
                            title = doc.getString("title") ?: "Untitled Service",
                            category = doc.getString("category") ?: "General",
                            providerName = doc.getString("providerName") ?: "Unknown",
                            contact = doc.getString("contact") ?: "N/A",
                            location = doc.getString("location") ?: "N/A",
                            status = doc.getString("status") ?: STATUS_ACTIVE,
                            email = doc.getString("email") ?: "",
                            imageUri = doc.getString("imageUri")?.takeIf { it.isNotBlank() },
                            ownerUid = doc.getString("ownerUid") ?: "",
                            isActive = doc.getBoolean("isActive") ?: true
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing service doc", e)
                        null
                    }
                }
                saveAllLocally(context, services)
            }
    }

    fun stopListening() {
        snapshotListener?.remove()
        snapshotListener = null
    }

    /**
     * Loads all services from Firestore once (non-realtime).
     * Updates local cache and invokes the callback on the main thread.
     */
    fun loadFromFirestore(context: Context, onComplete: ((List<Service>) -> Unit)? = null) {
        firestore.collection(COLLECTION)
            .get()
            .addOnSuccessListener { snapshot ->
                val services = snapshot.documents.mapNotNull { doc ->
                    try {
                        Service(
                            id = doc.id,
                            title = doc.getString("title") ?: "Untitled Service",
                            category = doc.getString("category") ?: "General",
                            providerName = doc.getString("providerName") ?: "Unknown",
                            contact = doc.getString("contact") ?: "N/A",
                            location = doc.getString("location") ?: "N/A",
                            status = doc.getString("status") ?: STATUS_ACTIVE,
                            email = doc.getString("email") ?: "",
                            imageUri = doc.getString("imageUri")?.takeIf { it.isNotBlank() },
                            ownerUid = doc.getString("ownerUid") ?: "",
                            isActive = doc.getBoolean("isActive") ?: true
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing service doc", e)
                        null
                    }
                }
                saveAllLocally(context, services)
                onComplete?.invoke(services)
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Failed to load services from Firestore", error)
                onComplete?.invoke(getAllServices(context))
            }
    }

    // ── Read ────────────────────────────────────────────────────

    fun getActiveServices(context: Context): List<Service> {
        return getAllServices(context).filter { it.status.equals(STATUS_ACTIVE, ignoreCase = true) }
    }

    fun getPendingServices(context: Context): List<Service> {
        return getAllServices(context).filter { it.status.equals(STATUS_PENDING, ignoreCase = true) }
    }

    fun getCompletedServices(context: Context): List<Service> {
        return getAllServices(context).filter { it.status.equals(STATUS_COMPLETED, ignoreCase = true) }
    }

    fun getAllServices(context: Context): List<Service> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SERVICES, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()

        return try {
            val jsonArray = JSONArray(raw)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    add(item.toService())
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** All active advertisements shown on the home feed. */
    fun getHomeAdvertisements(context: Context): List<Service> {
        return getAllServices(context).filter {
            it.status.equals(STATUS_ACTIVE, ignoreCase = true)
        }
    }

    /** Posts created by the currently logged-in service provider. */
    fun getMyPosts(context: Context): List<Service> {
        val uid = AppPreferences.getSessionUid(context)
        val sessionName = AppPreferences.getSessionName(context).trim()
        val sessionPhone = normalizePhone(AppPreferences.getSessionPhone(context))
        val sessionEmail = AppPreferences.getSessionEmail(context).trim().lowercase()

        return getAllServices(context).filter { service ->
            val owner = service.ownerUid.trim()
            val postEmail = service.email.trim().lowercase()
            when {
                uid.isNotBlank() && (owner == uid || owner.equals(sessionEmail, ignoreCase = true)) -> true
                sessionEmail.isNotBlank() && (postEmail == sessionEmail || owner.equals(sessionEmail, ignoreCase = true)) -> true
                owner.isBlank() -> matchesLegacyOwner(service, sessionName, sessionPhone)
                else -> false
            }
        }
    }

    fun getServiceById(context: Context, serviceId: String): Service? {
        return getAllServices(context).firstOrNull { it.id == serviceId }
    }

    // ── Create ──────────────────────────────────────────────────

    fun addService(
        context: Context,
        category: String,
        providerName: String,
        contact: String,
        location: String,
        email: String = "",
        imageUri: String? = null,
        ownerUid: String = resolveOwnerKey(context, email)
    ): Service {
        val serviceId = UUID.randomUUID().toString()
        val persistedImage = imageUri?.let { path ->
            PostImageHelper.attachToServiceId(context, path, serviceId)
                ?: persistImageUri(context, serviceId, path)
        }

        val newService = Service(
            id = serviceId,
            title = "New $category Service",
            category = category.trim(),
            providerName = providerName.trim(),
            contact = contact.trim(),
            location = location.trim(),
            status = STATUS_ACTIVE,
            email = email.trim(),
            imageUri = persistedImage,
            ownerUid = ownerUid
        )

        val updated = getAllServices(context).toMutableList().apply { add(0, newService) }
        saveAllLocally(context, updated)

        // Sync to Firestore
        firestore.collection(COLLECTION)
            .document(serviceId)
            .set(newService.toMap())
            .addOnSuccessListener { Log.d(TAG, "Service created in Firestore: $serviceId") }
            .addOnFailureListener { Log.w(TAG, "Failed to create service in Firestore", it) }

        return newService
    }

    // ── Update ──────────────────────────────────────────────────

    fun updateService(
        context: Context,
        serviceId: String,
        category: String,
        providerName: String,
        contact: String,
        location: String,
        email: String = "",
        imageUri: String? = null,
        replaceImage: Boolean = false
    ): Boolean {
        val current = getAllServices(context)
        var changed = false
        var updatedService: Service? = null
        val updated = current.map { service ->
            if (service.id == serviceId) {
                changed = true
                val nextImage = when {
                    !replaceImage -> service.imageUri
                    imageUri.isNullOrBlank() -> service.imageUri
                    else -> {
                        PostImageHelper.deletePostImage(context, service.imageUri)
                        PostImageHelper.attachToServiceId(context, imageUri, serviceId)
                            ?: persistImageUri(context, serviceId, imageUri)
                            ?: service.imageUri
                    }
                }
                service.copy(
                    category = category.trim(),
                    providerName = providerName.trim(),
                    contact = contact.trim(),
                    location = location.trim(),
                    email = email.trim(),
                    imageUri = nextImage,
                    title = "New ${category.trim()} Service"
                ).also { updatedService = it }
            } else {
                service
            }
        }
        if (changed) {
            saveAllLocally(context, updated)

            // Sync to Firestore
            updatedService?.let { svc ->
                firestore.collection(COLLECTION)
                    .document(serviceId)
                    .set(svc.toMap())
                    .addOnSuccessListener { Log.d(TAG, "Service updated in Firestore: $serviceId") }
                    .addOnFailureListener { Log.w(TAG, "Failed to update service in Firestore", it) }
            }
        }
        return changed
    }

    fun updateServiceStatus(context: Context, serviceId: String, newStatus: String): Boolean {
        val current = getAllServices(context)
        var changed = false
        val updated = current.map {
            if (it.id == serviceId && it.status != newStatus) {
                changed = true
                it.copy(status = newStatus)
            } else {
                it
            }
        }

        if (changed) {
            saveAllLocally(context, updated)

            // Sync to Firestore
            firestore.collection(COLLECTION)
                .document(serviceId)
                .update("status", newStatus)
                .addOnSuccessListener { Log.d(TAG, "Service status updated in Firestore: $serviceId") }
                .addOnFailureListener { Log.w(TAG, "Failed to update service status in Firestore", it) }
        }
        return changed
    }

    // ── Toggle Active ────────────────────────────────────────────

    fun toggleServiceActive(
        context: Context,
        serviceId: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val current = getAllServices(context)
        val service = current.firstOrNull { it.id == serviceId }
        if (service == null) {
            onComplete?.invoke(false)
            return
        }

        val newActiveState = !service.isActive
        val updated = current.map {
            if (it.id == serviceId) it.copy(isActive = newActiveState) else it
        }
        saveAllLocally(context, updated)

        // Sync to Firestore
        firestore.collection(COLLECTION)
            .document(serviceId)
            .update("isActive", newActiveState)
            .addOnSuccessListener {
                Log.d(TAG, "Service isActive toggled to $newActiveState: $serviceId")
                onComplete?.invoke(true)
            }
            .addOnFailureListener {
                Log.w(TAG, "Failed to toggle service active state", it)
                onComplete?.invoke(false)
            }
    }

    // ── Delete ──────────────────────────────────────────────────

    fun deleteService(context: Context, serviceId: String): Boolean {
        val current = getAllServices(context)
        val removed = current.firstOrNull { it.id == serviceId }
        val updated = current.filterNot { it.id == serviceId }
        val deleted = updated.size != current.size
        if (deleted) {
            PostImageHelper.deletePostImage(context, removed?.imageUri)
            saveAllLocally(context, updated)

            // Sync to Firestore
            firestore.collection(COLLECTION)
                .document(serviceId)
                .delete()
                .addOnSuccessListener { Log.d(TAG, "Service deleted from Firestore: $serviceId") }
                .addOnFailureListener { Log.w(TAG, "Failed to delete service from Firestore", it) }
        }
        return deleted
    }

    // ── Helpers ─────────────────────────────────────────────────

    private fun resolveOwnerKey(context: Context, email: String): String {
        val uid = AppPreferences.getSessionUid(context)
        if (uid.isNotBlank()) return uid
        return email.trim().lowercase()
    }

    private fun persistImageUri(context: Context, serviceId: String, imageUri: String): String? {
        if (imageUri.startsWith("content://", ignoreCase = true)) {
            return PostImageHelper.savePostImageFromUri(context, Uri.parse(imageUri), serviceId)
        }
        if (imageUri.startsWith(context.filesDir.absolutePath)) {
            return imageUri
        }
        return imageUri.takeIf { it.isNotBlank() }
    }

    private fun matchesLegacyOwner(
        service: Service,
        sessionName: String,
        sessionPhone: String
    ): Boolean {
        val nameMatch = sessionName.isNotBlank() &&
            service.providerName.equals(sessionName, ignoreCase = true)
        val phoneMatch = sessionPhone.isNotBlank() &&
            normalizePhone(service.contact) == sessionPhone
        return nameMatch || phoneMatch
    }

    private fun normalizePhone(value: String): String {
        return value.filter { it.isDigit() }
    }

    // ── Local cache ─────────────────────────────────────────────

    private fun saveAllLocally(context: Context, services: List<Service>) {
        val jsonArray = JSONArray()
        services.forEach { service -> jsonArray.put(service.toJson()) }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVICES, jsonArray.toString())
            .commit()
    }

    // ── JSON conversion ─────────────────────────────────────────

    private fun JSONObject.toService(): Service {
        val image = optString("imageUri", "").takeIf { it.isNotBlank() }
        return Service(
            id = optString("id", UUID.randomUUID().toString()),
            title = optString("title", "Untitled Service"),
            category = optString("category", "General"),
            providerName = optString("providerName", "Unknown"),
            contact = optString("contact", "N/A"),
            location = optString("location", "N/A"),
            status = optString("status", STATUS_ACTIVE),
            email = optString("email", ""),
            imageUri = image,
            ownerUid = optString("ownerUid", ""),
            isActive = optBoolean("isActive", true)
        )
    }

    private fun Service.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("category", category)
            put("providerName", providerName)
            put("contact", contact)
            put("location", location)
            put("status", status)
            put("email", email)
            put("imageUri", imageUri.orEmpty())
            put("ownerUid", ownerUid)
            put("isActive", isActive)
        }
    }

    private fun Service.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "category" to category,
            "providerName" to providerName,
            "contact" to contact,
            "location" to location,
            "status" to status,
            "email" to email,
            "imageUri" to imageUri.orEmpty(),
            "ownerUid" to ownerUid,
            "isActive" to isActive
        )
    }
}
