package com.example.microserve

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Lightweight local persistence for service provider listings.
 * Services transition: Active (Current) → Pending → Completed
 * Replace with Room/Firebase when backend integration is ready.
 */
object ServiceStore {

    const val EXTRA_SERVICE_ID = "service_id"

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
        val ownerUid: String = ""
    )

    private const val PREF_NAME = "service_store"
    private const val KEY_SERVICES = "services_json"

    const val STATUS_ACTIVE = "Active"
    const val STATUS_PENDING = "Pending Approval"
    const val STATUS_COMPLETED = "Completed"

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
        saveAll(context, updated)
        return newService
    }

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
                )
            } else {
                service
            }
        }
        if (changed) saveAll(context, updated)
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

        if (changed) saveAll(context, updated)
        return changed
    }

    fun deleteService(context: Context, serviceId: String): Boolean {
        val current = getAllServices(context)
        val removed = current.firstOrNull { it.id == serviceId }
        val updated = current.filterNot { it.id == serviceId }
        val deleted = updated.size != current.size
        if (deleted) {
            PostImageHelper.deletePostImage(context, removed?.imageUri)
            saveAll(context, updated)
        }
        return deleted
    }

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

    private fun saveAll(context: Context, services: List<Service>) {
        val jsonArray = JSONArray()
        services.forEach { service -> jsonArray.put(service.toJson()) }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVICES, jsonArray.toString())
            .commit()
    }

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
            ownerUid = optString("ownerUid", "")
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
        }
    }
}
