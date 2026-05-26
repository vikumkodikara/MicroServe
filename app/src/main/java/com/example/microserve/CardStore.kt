package com.example.microserve

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object CardStore {

    data class Card(
        val id: String,
        val cardNumber: String,
        val cardName: String,
        val date: String,
        val cvv: String
    )

    private const val PREF_NAME = "card_store"
    private const val KEY_CARDS = "cards_json"

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

    fun addCard(context: Context, cardNumber: String, cardName: String, date: String, cvv: String): Card {
        val card = Card(UUID.randomUUID().toString(), cardNumber.trim(), cardName.trim(), date.trim(), cvv.trim())
        val updated = getAllCards(context).toMutableList().apply { add(card) }
        saveAll(context, updated)
        return card
    }

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
        saveAll(context, cards)
        return true
    }

    fun deleteCard(context: Context, id: String): Boolean {
        val cards = getAllCards(context)
        val updated = cards.filterNot { it.id == id }
        val deleted = updated.size != cards.size
        if (deleted) saveAll(context, updated)
        return deleted
    }

    private fun saveAll(context: Context, items: List<Card>) {
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
}
