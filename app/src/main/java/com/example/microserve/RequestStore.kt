package com.example.microserve

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object RequestStore {

    const val STATUS_PENDING = "pending"
    const val STATUS_COMPLETED = "completed"

    data class UserRequest(
        val id: String,
        val requesterName: String,
        val title: String,
        val category: String,
        val contact: String,
        val location: String,
        val description: String,
        val status: String = STATUS_PENDING
    ) {
        val name: String get() = requesterName
    }

    private const val PREF_NAME = "request_store"
    private const val KEY_REQUESTS = "requests_json"

    fun getAllRequests(context: Context): List<UserRequest> = loadAll(context)

    fun getPendingRequests(context: Context): List<UserRequest> =
        loadAll(context).filter { it.status == STATUS_PENDING }

    fun getRequestById(context: Context, id: String): UserRequest? =
        loadAll(context).firstOrNull { it.id == id }

    fun hasPendingRequestsForCategory(context: Context, storeKeys: List<String>): Boolean {
        if (storeKeys.isEmpty()) return false
        val normalized = storeKeys.map { it.trim().lowercase() }
        return getPendingRequests(context).any { req ->
            req.category.trim().lowercase() in normalized
        }
    }

    fun addRequest(
        context: Context,
        requesterName: String,
        title: String,
        category: String,
        contact: String,
        location: String,
        description: String
    ): UserRequest = addRequestInternal(
        context,
        requesterName = requesterName,
        title = title,
        category = category,
        contact = contact,
        location = location,
        description = description
    )

    fun updateRequest(
        context: Context,
        id: String,
        name: String,
        title: String,
        category: String,
        contact: String,
        location: String,
        description: String
    ): Boolean {
        val list = loadAll(context).toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index == -1) return false
        val existing = list[index]
        list[index] = existing.copy(
            requesterName = name.trim(),
            title = title.trim(),
            category = category.trim(),
            contact = contact.trim(),
            location = location.trim(),
            description = description.trim()
        )
        saveAll(context, list)
        return true
    }

    fun markRequestCompleted(context: Context, id: String): Boolean {
        val list = loadAll(context).toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index == -1) return false
        list[index] = list[index].copy(status = STATUS_COMPLETED)
        saveAll(context, list)
        return true
    }

    fun deleteRequest(context: Context, id: String): Boolean {
        val list = loadAll(context)
        val updated = list.filterNot { it.id == id }
        val deleted = updated.size != list.size
        if (deleted) saveAll(context, updated)
        return deleted
    }

    private fun addRequestInternal(
        context: Context,
        requesterName: String,
        title: String,
        category: String,
        contact: String,
        location: String,
        description: String
    ): UserRequest {
        val req = UserRequest(
            id = UUID.randomUUID().toString(),
            requesterName = requesterName.trim(),
            title = title.trim(),
            category = category.trim(),
            contact = contact.trim(),
            location = location.trim(),
            description = description.trim(),
            status = STATUS_PENDING
        )
        val updated = loadAll(context).toMutableList().apply { add(req) }
        saveAll(context, updated)
        return req
    }

    private fun loadAll(context: Context): List<UserRequest> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_REQUESTS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val requesterName = obj.optString("requesterName")
                        .ifBlank { obj.optString("name", "") }
                    add(
                        UserRequest(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            requesterName = requesterName,
                            title = obj.optString("title", ""),
                            category = obj.optString("category", ""),
                            contact = obj.optString("contact", ""),
                            location = obj.optString("location", ""),
                            description = obj.optString("description", ""),
                            status = obj.optString("status", STATUS_PENDING)
                                .ifBlank { STATUS_PENDING }
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveAll(context: Context, items: List<UserRequest>) {
        val arr = JSONArray()
        items.forEach { req ->
            arr.put(
                JSONObject().apply {
                    put("id", req.id)
                    put("name", req.requesterName)
                    put("requesterName", req.requesterName)
                    put("title", req.title)
                    put("category", req.category)
                    put("contact", req.contact)
                    put("location", req.location)
                    put("description", req.description)
                    put("status", req.status)
                }
            )
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REQUESTS, arr.toString())
            .apply()
    }
}
