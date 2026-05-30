package com.example.microserve

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/**
 * Local persistence for provider payout transactions.
 * Pending -> Success after admin transfers payment.
 */
object TransactionStore {

    data class Transaction(
        val id: String,
        val transactionCode: String,
        val title: String,
        val providerUserId: String,
        val providerCode: String,
        val providerName: String,
        val amount: Double,
        val status: String,
        val createdAt: Long,
        val completedAt: Long? = null
    )

    private const val PREF_NAME = "transaction_store"
    private const val KEY_TRANSACTIONS = "transactions_json"

    const val STATUS_PENDING = "Pending"
    const val STATUS_SUCCESS = "Success"

    fun getAllTransactions(context: Context): List<Transaction> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()

        return try {
            val jsonArray = JSONArray(raw)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    add(item.toTransaction())
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getPendingTransactions(context: Context): List<Transaction> {
        return getAllTransactions(context)
            .filter { it.status.equals(STATUS_PENDING, ignoreCase = true) }
            .sortedByDescending { it.createdAt }
    }

    fun getSuccessTransactions(context: Context): List<Transaction> {
        return getAllTransactions(context)
            .filter { it.status.equals(STATUS_SUCCESS, ignoreCase = true) }
            .sortedByDescending { it.completedAt ?: it.createdAt }
    }

    fun getTransactionById(context: Context, transactionId: String): Transaction? {
        return getAllTransactions(context).firstOrNull { it.id == transactionId }
    }

    fun addPendingTransaction(
        context: Context,
        providerUserId: String,
        providerName: String,
        amount: Double,
        title: String = "Transfer to Provider"
    ): Transaction {
        val code = generateTransactionCode(context)
        val providerCode = generateProviderCode(providerUserId)

        val newItem = Transaction(
            id = UUID.randomUUID().toString(),
            transactionCode = code,
            title = title,
            providerUserId = providerUserId,
            providerCode = providerCode,
            providerName = providerName,
            amount = amount,
            status = STATUS_PENDING,
            createdAt = System.currentTimeMillis(),
            completedAt = null
        )

        val updated = getAllTransactions(context).toMutableList().apply { add(0, newItem) }
        saveAll(context, updated)
        return newItem
    }

    fun markTransactionSuccessAndCreditUser(context: Context, transactionId: String): Boolean {
        val current = getAllTransactions(context)
        val target = current.firstOrNull { it.id == transactionId } ?: return false
        if (target.status.equals(STATUS_SUCCESS, ignoreCase = true)) return false

        val credited = UserStore.addCashPoints(context, target.providerUserId, target.amount.toInt())
        if (!credited) return false

        val now = System.currentTimeMillis()
        val updated = current.map {
            if (it.id == transactionId) it.copy(status = STATUS_SUCCESS, completedAt = now) else it
        }
        saveAll(context, updated)
        return true
    }

    fun getTotalSuccessAmount(context: Context): Double {
        return getSuccessTransactions(context).sumOf { it.amount }
    }

    fun getSuccessCount(context: Context): Int {
        return getSuccessTransactions(context).size
    }

    fun formatAmount(amount: Double): String {
        val formatter = DecimalFormat("#,##0.00")
        return "${formatter.format(amount)} M Points"
    }

    fun estimateAmountForService(category: String): Double {
        return when (category.lowercase(Locale.getDefault())) {
            "plumbing" -> 2800.0
            "electrical" -> 3200.0
            "cleaning" -> 2500.0
            "house painting" -> 4500.0
            "carpentry" -> 3800.0
            "graphic design" -> 3500.0
            "welding" -> 3000.0
            else -> 3000.0
        }
    }

    private fun generateProviderCode(userId: String): String {
        val number = abs(userId.hashCode()) % 9_000_000 + 1_000_000
        return "P$number"
    }

    private fun generateTransactionCode(context: Context): String {
        val next = getAllTransactions(context).size + 1
        val codeNumber = 300 + next
        return "TX-$codeNumber"
    }

    private fun saveAll(context: Context, items: List<Transaction>) {
        val jsonArray = JSONArray()
        items.forEach { jsonArray.put(it.toJson()) }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TRANSACTIONS, jsonArray.toString())
            .apply()
    }

    private fun JSONObject.toTransaction(): Transaction {
        val completedValue = if (isNull("completedAt")) null else optLong("completedAt")
        return Transaction(
            id = optString("id", UUID.randomUUID().toString()),
            transactionCode = optString("transactionCode", "TX-000"),
            title = optString("title", "Transfer to Provider"),
            providerUserId = optString("providerUserId", ""),
            providerCode = optString("providerCode", "P0000000"),
            providerName = optString("providerName", "Provider"),
            amount = optDouble("amount", 0.0),
            status = optString("status", STATUS_PENDING),
            createdAt = optLong("createdAt", System.currentTimeMillis()),
            completedAt = completedValue
        )
    }

    private fun Transaction.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("transactionCode", transactionCode)
            put("title", title)
            put("providerUserId", providerUserId)
            put("providerCode", providerCode)
            put("providerName", providerName)
            put("amount", amount)
            put("status", status)
            put("createdAt", createdAt)
            put("completedAt", completedAt)
        }
    }
}
