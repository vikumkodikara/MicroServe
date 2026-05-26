package com.example.microserve

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object RequestStore {

    data class ServiceRequest(
        val id: String,
        val name: String,
        val title: String,
        val category: String,
        val contact: String,
        val location: String,
        val description: String
    )

    private const val PREF_NAME = "request_store"
    private const val KEY_REQUESTS = "requests_json"

    fun getAllRequests(context: Context): List<ServiceRequest> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_REQUESTS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        ServiceRequest(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.optString("name", ""),
                            title = obj.optString("title", ""),
                            category = obj.optString("category", ""),
                            contact = obj.optString("contact", ""),
                            location = obj.optString("location", ""),
                            description = obj.optString("description", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addRequest(context: Context, name: String, title: String, category: String, contact: String, location: String, description: String): ServiceRequest {
        val req = ServiceRequest(UUID.randomUUID().toString(), name.trim(), title.trim(), category.trim(), contact.trim(), location.trim(), description.trim())
        val updated = getAllRequests(context).toMutableList().apply { add(req) }
        saveAll(context, updated)
        return req
    }

    fun updateRequest(context: Context, id: String, name: String, title: String, category: String, contact: String, location: String, description: String): Boolean {
        val list = getAllRequests(context).toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index == -1) return false
        list[index] = list[index].copy(
            name = name.trim(), title = title.trim(), category = category.trim(),
            contact = contact.trim(), location = location.trim(), description = description.trim()
        )
        saveAll(context, list)
        return true
    }

    fun deleteRequest(context: Context, id: String): Boolean {
        val list = getAllRequests(context)
        val updated = list.filterNot { it.id == id }
        val deleted = updated.size != list.size
        if (deleted) saveAll(context, updated)
        return deleted
    }

    private fun saveAll(context: Context, items: List<ServiceRequest>) {
        val arr = JSONArray()
        items.forEach { req ->
            arr.put(JSONObject().apply {
                put("id", req.id)
                put("name", req.name)
                put("title", req.title)
                put("category", req.category)
                put("contact", req.contact)
                put("location", req.location)
                put("description", req.description)
            })
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_REQUESTS, arr.toString()).apply()
    }
}
