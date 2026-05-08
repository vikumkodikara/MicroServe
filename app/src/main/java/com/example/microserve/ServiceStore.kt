package com.example.microserve

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Lightweight local persistence for service provider listings.
 * Services transition: Active (Current) → Pending → Completed
 * Replace with Room/Firebase when backend integration is ready.
 */
object ServiceStore {

    data class Service(
        val id: String,
        val title: String,
        val category: String,
        val providerName: String,
        val contact: String,
        val location: String,
        val status: String = STATUS_ACTIVE
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

    fun addService(
        context: Context,
        category: String,
        providerName: String,
        contact: String,
        location: String
    ) {
        val newService = Service(
            id = UUID.randomUUID().toString(),
            title = "New $category Service",
            category = category.trim(),
            providerName = providerName.trim(),
            contact = contact.trim(),
            location = location.trim(),
            status = STATUS_ACTIVE
        )

        val updated = getAllServices(context).toMutableList().apply { add(0, newService) }
        saveAll(context, updated)
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
        val updated = getAllServices(context).filterNot { it.id == serviceId }
        val deleted = updated.size != getAllServices(context).size
        if (deleted) saveAll(context, updated)
        return deleted
    }

    private fun saveAll(context: Context, services: List<Service>) {
        val jsonArray = JSONArray()
        services.forEach { service -> jsonArray.put(service.toJson()) }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVICES, jsonArray.toString())
            .apply()
    }

    private fun JSONObject.toService(): Service {
        return Service(
            id = optString("id", UUID.randomUUID().toString()),
            title = optString("title", "Untitled Service"),
            category = optString("category", "General"),
            providerName = optString("providerName", "Unknown"),
            contact = optString("contact", "N/A"),
            location = optString("location", "N/A"),
            status = optString("status", STATUS_ACTIVE)
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
        }
    }
}
