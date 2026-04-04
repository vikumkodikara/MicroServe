package com.example.microserve

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Local persistence for user feedback submitted by logged-in users.
 * Replace with Room/Firebase when backend integration is ready.
 */
object FeedbackStore {

    data class Feedback(
        val id: String,
        val userId: String,
        val userName: String,
        val message: String,
        val rating: Int,
        val createdAt: Long
    )

    private const val PREF_NAME = "feedback_store"
    private const val KEY_FEEDBACKS = "feedbacks_json"

    fun getAllFeedbacks(context: Context): List<Feedback> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_FEEDBACKS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()

        return try {
            val jsonArray = JSONArray(raw)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    add(item.toFeedback())
                }
            }.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getFeedbackCount(context: Context): Int {
        return getAllFeedbacks(context).size
    }

    fun addFeedback(
        context: Context,
        userId: String,
        userName: String,
        message: String,
        rating: Int,
        createdAt: Long = System.currentTimeMillis()
    ): Feedback {
        val safeRating = rating.coerceIn(1, 5)
        val feedback = Feedback(
            id = UUID.randomUUID().toString(),
            userId = userId.trim(),
            userName = userName.trim().ifBlank { "Anonymous User" },
            message = message.trim().ifBlank { "No feedback message provided." },
            rating = safeRating,
            createdAt = createdAt
        )

        val updated = getAllFeedbacks(context).toMutableList().apply { add(0, feedback) }
        saveAll(context, updated)
        return feedback
    }

    fun deleteFeedback(context: Context, feedbackId: String): Boolean {
        val current = getAllFeedbacks(context)
        val updated = current.filterNot { it.id == feedbackId }
        val deleted = updated.size != current.size
        if (deleted) saveAll(context, updated)
        return deleted
    }

    private fun saveAll(context: Context, items: List<Feedback>) {
        val jsonArray = JSONArray()
        items.forEach { jsonArray.put(it.toJson()) }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FEEDBACKS, jsonArray.toString())
            .apply()
    }

    private fun JSONObject.toFeedback(): Feedback {
        return Feedback(
            id = optString("id", UUID.randomUUID().toString()),
            userId = optString("userId", ""),
            userName = optString("userName", "Anonymous User"),
            message = optString("message", "No feedback message provided."),
            rating = optInt("rating", 5).coerceIn(1, 5),
            createdAt = optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun Feedback.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("userId", userId)
            put("userName", userName)
            put("message", message)
            put("rating", rating)
            put("createdAt", createdAt)
        }
    }
}
