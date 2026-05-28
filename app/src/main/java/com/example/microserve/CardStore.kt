package com.example.microserve

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Card persistence backed by Firebase Firestore (per-user subcollection)
 * with a local SharedPreferences cache for offline access.
 */
object CardStore {

    private const val TAG = "CardStore"
    private const val SUBCOLLECTION = "cards"

    data class Card(
        val id: String,
        val cardNumber: String,
        val cardName: String,
        val date: String,
        val cvv: String
    )

    private const val PREF_NAME = "card_store"
    private const val KEY_CARDS = "cards_json"

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private var snapshotListener: ListenerRegistration? = null

    private fun cardsCollection() =
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).collection(SUBCOLLECTION)
        }

    // ── Firestore Real-time Sync ────────────────────────────────

    fun startListening(context: Context) {
        if (snapshotListener != null) return
        val collection = cardsCollection() ?: return

        snapshotListener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Firestore listen failed", error)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            val cards = snapshot.documents.mapNotNull { doc ->
                try {
                    Card(
                        id = doc.id,
                        cardNumber = doc.getString("cardNumber") ?: "",
                        cardName = doc.getString("cardName") ?: "",
                        date = doc.getString("date") ?: "",
                        cvv = doc.getString("cvv") ?: ""
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing card doc", e)
                    null
                }
            }
            saveAllLocally(context, cards)
        }
    }

    fun stopListening() {
        snapshotListener?.remove()
        snapshotListener = null
    }

    /**
     * Pulls all cards from Firestore for the current user and
     * populates the local cache. Call on login.
     */
    fun syncOnLogin(context: Context, onComplete: (() -> Unit)? = null) {
        val collection = cardsCollection()
        if (collection == null) {
            onComplete?.invoke()
            return
        }

        collection.get()
            .addOnSuccessListener { snapshot ->
                val cards = snapshot.documents.mapNotNull { doc ->
                    try {
                        Card(
                            id = doc.id,
                            cardNumber = doc.getString("cardNumber") ?: "",
                            cardName = doc.getString("cardName") ?: "",
                            date = doc.getString("date") ?: "",
                            cvv = doc.getString("cvv") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing card doc", e)
                        null
                    }
                }
                saveAllLocally(context, cards)
                onComplete?.invoke()
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Failed to sync cards from Firestore", error)
                onComplete?.invoke()
            }
    }

    /** Clear local card cache on logout. */
    fun clearOnLogout(context: Context) {
        stopListening()
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_CARDS).apply()
    }

    // ── Read ────────────────────────────────────────────────────

    fun getAllCards(context: Context): List<Card> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CARDS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        Card(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            cardNumber = obj.optString("cardNumber", ""),
                            cardName = obj.optString("cardName", ""),
                            date = obj.optString("date", ""),
                            cvv = obj.optString("cvv", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Create ──────────────────────────────────────────────────

    fun addCard(context: Context, cardNumber: String, cardName: String, date: String, cvv: String): Card {
        val card = Card(UUID.randomUUID().toString(), cardNumber.trim(), cardName.trim(), date.trim(), cvv.trim())
        val updated = getAllCards(context).toMutableList().apply { add(card) }
        saveAllLocally(context, updated)

        // Sync to Firestore
        cardsCollection()?.document(card.id)
            ?.set(card.toMap())
            ?.addOnSuccessListener { Log.d(TAG, "Card created in Firestore: ${card.id}") }
            ?.addOnFailureListener { Log.w(TAG, "Failed to create card in Firestore", it) }

        return card
    }

    // ── Update ──────────────────────────────────────────────────

    fun updateCard(context: Context, id: String, cardNumber: String, cardName: String, date: String, cvv: String): Boolean {
        val cards = getAllCards(context).toMutableList()
        val index = cards.indexOfFirst { it.id == id }
        if (index == -1) return false
        cards[index] = cards[index].copy(
            cardNumber = cardNumber.trim(),
            cardName = cardName.trim(),
            date = date.trim(),
            cvv = cvv.trim()
        )
        saveAllLocally(context, cards)

        // Sync to Firestore
        cardsCollection()?.document(id)
            ?.set(cards[index].toMap())
            ?.addOnSuccessListener { Log.d(TAG, "Card updated in Firestore: $id") }
            ?.addOnFailureListener { Log.w(TAG, "Failed to update card in Firestore", it) }

        return true
    }

    // ── Delete ──────────────────────────────────────────────────

    fun deleteCard(context: Context, id: String): Boolean {
        val cards = getAllCards(context)
        val updated = cards.filterNot { it.id == id }
        val deleted = updated.size != cards.size
        if (deleted) {
            saveAllLocally(context, updated)

            // Sync to Firestore
            cardsCollection()?.document(id)
                ?.delete()
                ?.addOnSuccessListener { Log.d(TAG, "Card deleted from Firestore: $id") }
                ?.addOnFailureListener { Log.w(TAG, "Failed to delete card from Firestore", it) }
        }
        return deleted
    }

    // ── Local cache ─────────────────────────────────────────────

    private fun saveAllLocally(context: Context, items: List<Card>) {
        val arr = JSONArray()
        items.forEach { card ->
            arr.put(JSONObject().apply {
                put("id", card.id)
                put("cardNumber", card.cardNumber)
                put("cardName", card.cardName)
                put("date", card.date)
                put("cvv", card.cvv)
            })
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CARDS, arr.toString()).apply()
    }

    private fun Card.toMap(): Map<String, Any> {
        return mapOf(
            "cardNumber" to cardNumber,
            "cardName" to cardName,
            "date" to date,
            "cvv" to cvv
        )
    }
}
