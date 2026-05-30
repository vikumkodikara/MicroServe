package com.example.microserve

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Feedback persistence backed by Firebase Firestore with a local
 * SharedPreferences cache for offline access.
 */
object FeedbackStore {

    private const val TAG = "FeedbackStore"
    private const val COLLECTION = "feedbacks"

    // Local cache keys
    private const val PREF_NAME = "feedback_store"
    private const val KEY_FEEDBACKS = "feedbacks_json"

    data class Feedback(
        val id: String,
        val ownerUid: String,
        val userName: String,
        val message: String,
        val rating: Int,
        val createdAt: Long
    )

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private var snapshotListener: ListenerRegistration? = null

    // ── Firestore Real-time Sync ────────────────────────────────

    /**
     * Starts a Firestore snapshot listener that keeps the local cache
     * up-to-date. Call once from the Activity that first needs feedback data.
     */
    fun startListening(context: Context) {
        if (snapshotListener != null) return

        snapshotListener = firestore.collection(COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore listen failed", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val feedbacks = snapshot.documents.mapNotNull { doc ->
                    try {
                        Feedback(
                            id = doc.getString("id") ?: doc.id,
                            ownerUid = doc.getString("ownerUid") ?: "",
                            userName = doc.getString("userName") ?: "Anonymous",
                            message = doc.getString("message") ?: "",
                            rating = (doc.getLong("rating") ?: 5).toInt().coerceIn(1, 5),
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing feedback doc", e)
                        null
                    }
                }
                saveAllLocally(context, feedbacks)
            }
    }

    fun stopListening() {
        snapshotListener?.remove()
        snapshotListener = null
    }

    // ── Read ────────────────────────────────────────────────────

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

    /**
     * Loads all feedbacks from Firestore once (non-realtime).
     * Updates local cache and invokes the callback on the main thread.
     */
    fun loadFromFirestore(context: Context, onComplete: (List<Feedback>) -> Unit) {
        firestore.collection(COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val feedbacks = snapshot.documents.mapNotNull { doc ->
                    try {
                        Feedback(
                            id = doc.getString("id") ?: doc.id,
                            ownerUid = doc.getString("ownerUid") ?: "",
                            userName = doc.getString("userName") ?: "Anonymous",
                            message = doc.getString("message") ?: "",
                            rating = (doc.getLong("rating") ?: 5).toInt().coerceIn(1, 5),
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing feedback doc", e)
                        null
                    }
                }
                saveAllLocally(context, feedbacks)
                onComplete(feedbacks)
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Failed to load feedbacks from Firestore", error)
                onComplete(getAllFeedbacks(context)) // fallback to local cache
            }
    }

    // ── Create ──────────────────────────────────────────────────

    fun addFeedback(
        context: Context,
        ownerUid: String,
        userName: String,
        message: String,
        rating: Int,
        createdAt: Long = System.currentTimeMillis()
    ): Feedback {
        val safeRating = rating.coerceIn(1, 5)
        val id = UUID.randomUUID().toString()
        val feedback = Feedback(
            id = id,
            ownerUid = ownerUid.trim(),
            userName = userName.trim().ifBlank { "Anonymous User" },
            message = message.trim().ifBlank { "No feedback message provided." },
            rating = safeRating,
            createdAt = createdAt
        )

        // Save to local cache immediately
        val updated = getAllFeedbacks(context).toMutableList().apply { add(0, feedback) }
        saveAllLocally(context, updated)

        // Sync to Firestore
        firestore.collection(COLLECTION)
            .document(id)
            .set(feedback.toMap())
            .addOnSuccessListener { Log.d(TAG, "Feedback created in Firestore: $id") }
            .addOnFailureListener { Log.w(TAG, "Failed to create feedback in Firestore", it) }

        return feedback
    }

    // ── Update ──────────────────────────────────────────────────

    fun updateFeedback(context: Context, feedbackId: String, message: String, rating: Int): Boolean {
        val current = getAllFeedbacks(context).toMutableList()
        val index = current.indexOfFirst { it.id == feedbackId }
        if (index == -1) return false

        current[index] = current[index].copy(
            message = message.trim().ifBlank { "No feedback message provided." },
            rating = rating.coerceIn(1, 5)
        )
        saveAllLocally(context, current)

        // Sync to Firestore
        firestore.collection(COLLECTION)
            .document(feedbackId)
            .update(
                mapOf(
                    "message" to current[index].message,
                    "rating" to current[index].rating
                )
            )
            .addOnSuccessListener { Log.d(TAG, "Feedback updated in Firestore: $feedbackId") }
            .addOnFailureListener { Log.w(TAG, "Failed to update feedback in Firestore", it) }

        return true
    }

    // ── Delete ──────────────────────────────────────────────────

    fun deleteFeedback(context: Context, feedbackId: String): Boolean {
        val current = getAllFeedbacks(context)
        val updated = current.filterNot { it.id == feedbackId }
        val deleted = updated.size != current.size
        if (deleted) {
            saveAllLocally(context, updated)

            // Sync to Firestore
            firestore.collection(COLLECTION)
                .document(feedbackId)
                .delete()
                .addOnSuccessListener { Log.d(TAG, "Feedback deleted from Firestore: $feedbackId") }
                .addOnFailureListener { Log.w(TAG, "Failed to delete feedback from Firestore", it) }
        }
        return deleted
    }

    // ── Ownership helpers ───────────────────────────────────────

    /** True if the given feedback belongs to the currently logged-in user. */
    fun isOwner(context: Context, feedback: Feedback): Boolean {
        val currentUid = AppPreferences.getSessionUid(context)
        return currentUid.isNotEmpty() && currentUid == feedback.ownerUid
    }

    /** True if the current user is allowed to delete the feedback (owner or admin). */
    fun canDelete(context: Context, feedback: Feedback): Boolean {
        if (isOwner(context, feedback)) return true
        val role = AppPreferences.getSessionRole(context)
        return role.equals(UserProfile.ROLE_ADMIN, ignoreCase = true)
    }

    // ── Local cache ─────────────────────────────────────────────

    private fun saveAllLocally(context: Context, items: List<Feedback>) {
        val jsonArray = JSONArray()
        items.forEach { jsonArray.put(it.toJson()) }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FEEDBACKS, jsonArray.toString())
            .apply()
    }

    // ── JSON conversion ─────────────────────────────────────────

    private fun JSONObject.toFeedback(): Feedback {
        return Feedback(
            id = optString("id", UUID.randomUUID().toString()),
            ownerUid = optString("ownerUid", ""),
            userName = optString("userName", "Anonymous User"),
            message = optString("message", "No feedback message provided."),
            rating = optInt("rating", 5).coerceIn(1, 5),
            createdAt = optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun Feedback.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("ownerUid", ownerUid)
            put("userName", userName)
            put("message", message)
            put("rating", rating)
            put("createdAt", createdAt)
        }
    }

    private fun Feedback.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "ownerUid" to ownerUid,
            "userName" to userName,
            "message" to message,
            "rating" to rating,
            "createdAt" to createdAt
        )
    }
}
