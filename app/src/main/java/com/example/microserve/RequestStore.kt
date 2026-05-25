package com.example.microserve

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Lightweight local persistence for user service requests.
 * Replace this with Room/Firebase when backend integration is ready.
 */
object RequestStore {

    data class UserRequest(
        val id: String,
        val title: String,
        val category: String,
        val requesterName: String,
        val contact: String,
        val location: String,
        val description: String,
        val status: String = STATUS_PENDING
    )

    private const val PREF_NAME = "request_store"
    private const val KEY_REQUESTS = "requests_json"

    const val STATUS_PENDING = "Pending"
    const val STATUS_COMPLETED = "Completed"

    fun getPendingRequests(context: Context): List<UserRequest> {
        return getAllRequests(context).filter { it.status.equals(STATUS_PENDING, ignoreCase = true) }
    }

    fun getPendingRequestsByCategory(context: Context, categoryKeys: List<String>): List<UserRequest> {
        if (categoryKeys.isEmpty()) return emptyList()
        return getPendingRequests(context).filter { request ->
            categoryKeys.any { key -> request.category.equals(key, ignoreCase = true) }
        }
    }

    fun hasPendingRequestsForCategory(context: Context, categoryKeys: List<String>): Boolean {
        return getPendingRequestsByCategory(context, categoryKeys).isNotEmpty()
    }

    fun getAllRequests(context: Context): List<UserRequest> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_REQUESTS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()

        return try {
            val jsonArray = JSONArray(raw)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    add(item.toUserRequest())
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addRequest(
        context: Context,
        category: String,
        requesterName: String,
        contact: String,
        location: String,
        title: String? = null,
        description: String? = null
    ) {
        val safeCategory = category.trim()
        val requestTitle = title?.trim().takeUnless { it.isNullOrBlank() }
            ?: "Need $safeCategory Service"
        val requestDescription = description?.trim().takeUnless { it.isNullOrBlank() }
            ?: "New request submitted by $requesterName."

        val newRequest = UserRequest(
            id = UUID.randomUUID().toString(),
            title = requestTitle,
            category = safeCategory,
            requesterName = requesterName.trim(),
            contact = contact.trim(),
            location = location.trim(),
            description = requestDescription,
            status = STATUS_PENDING
        )

        val updated = getAllRequests(context).toMutableList().apply { add(0, newRequest) }
        saveAll(context, updated)
    }

    fun deleteRequest(context: Context, requestId: String): Boolean {
        val updated = getAllRequests(context).filterNot { it.id == requestId }
        val deleted = updated.size != getAllRequests(context).size
        if (deleted) saveAll(context, updated)
        return deleted
    }

    fun markRequestCompleted(context: Context, requestId: String): Boolean {
        val current = getAllRequests(context)
        var changed = false
        val updated = current.map {
            if (it.id == requestId && !it.status.equals(STATUS_COMPLETED, ignoreCase = true)) {
                changed = true
                it.copy(status = STATUS_COMPLETED)
            } else {
                it
            }
        }

        if (changed) saveAll(context, updated)
        return changed
    }

    private fun saveAll(context: Context, requests: List<UserRequest>) {
        val jsonArray = JSONArray()
        requests.forEach { request -> jsonArray.put(request.toJson()) }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REQUESTS, jsonArray.toString())
            .apply()
    }

    private fun JSONObject.toUserRequest(): UserRequest {
        return UserRequest(
            id = optString("id", UUID.randomUUID().toString()),
            title = optString("title", "Untitled Request"),
            category = optString("category", "General"),
            requesterName = optString("requesterName", "Unknown"),
            contact = optString("contact", "N/A"),
            location = optString("location", "N/A"),
            description = optString("description", "No description provided."),
            status = optString("status", STATUS_PENDING)
        )
    }

    private fun UserRequest.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("category", category)
            put("requesterName", requesterName)
            put("contact", contact)
            put("location", location)
            put("description", description)
            put("status", status)
        }
    }
}
