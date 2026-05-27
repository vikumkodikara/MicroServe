package com.example.microserve

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Lightweight local persistence for registered users.
 * Replace with Firebase Auth + Firestore when backend integration is ready.
 */
object UserStore {

    data class User(
        val id: String,
        val name: String,
        val email: String,
        val phone: String,
        val password: String = DEFAULT_PASSWORD,
        val type: String = TYPE_REQUESTER, // PROVIDER, REQUESTER, ADMIN
        val status: String = STATUS_ACTIVE, // ACTIVE, BANNED, INACTIVE
        val createdAt: Long = System.currentTimeMillis(),
        val isProvider: Boolean = false,
        val cashPoints: Int = 0
    )

    private const val PREF_NAME = "user_store"
    private const val KEY_USERS = "users_json"

    const val TYPE_PROVIDER = "Provider"
    const val TYPE_REQUESTER = "Requester"
    const val TYPE_ADMIN = "Admin"

    const val STATUS_ACTIVE = "Active"
    const val STATUS_BANNED = "Banned"
    const val STATUS_INACTIVE = "Inactive"

    const val DEFAULT_PASSWORD = "admin123"

    data class UpdateAdminResult(
        val success: Boolean,
        val message: String
    )

    fun getAllUsers(context: Context): List<User> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_USERS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()

        return try {
            val jsonArray = JSONArray(raw)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    add(item.toUser())
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getProviders(context: Context): List<User> {
        return getAllUsers(context).filter { it.type.equals(TYPE_PROVIDER, ignoreCase = true) }
    }

    fun getRequesters(context: Context): List<User> {
        return getAllUsers(context).filter { it.type.equals(TYPE_REQUESTER, ignoreCase = true) }
    }

    fun getActiveUsers(context: Context): List<User> {
        return getAllUsers(context).filter { it.status.equals(STATUS_ACTIVE, ignoreCase = true) }
    }

    fun getInactiveUsers(context: Context): List<User> {
        return getAllUsers(context).filter { 
            it.status.equals(STATUS_BANNED, ignoreCase = true) || 
            it.status.equals(STATUS_INACTIVE, ignoreCase = true) 
        }
    }

    fun getUserById(context: Context, userId: String): User? {
        return getAllUsers(context).firstOrNull { it.id == userId }
    }

    fun getUserByName(context: Context, name: String): User? {
        return getAllUsers(context).firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun addUser(
        context: Context,
        name: String,
        email: String,
        phone: String,
        password: String = DEFAULT_PASSWORD,
        type: String = TYPE_REQUESTER
    ): User {
        val newUser = User(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            password = password,
            type = type,
            status = STATUS_ACTIVE
        )

        val updated = getAllUsers(context).toMutableList().apply { add(0, newUser) }
        saveAll(context, updated)
        return newUser
    }

    fun updateUserStatus(context: Context, userId: String, newStatus: String): Boolean {
        val current = getAllUsers(context)
        var changed = false
        val updated = current.map {
            if (it.id == userId && it.status != newStatus) {
                changed = true
                it.copy(status = newStatus)
            } else {
                it
            }
        }

        if (changed) saveAll(context, updated)
        return changed
    }

    fun deleteUser(context: Context, userId: String): Boolean {
        val updated = getAllUsers(context).filterNot { it.id == userId }
        val deleted = updated.size != getAllUsers(context).size
        if (deleted) saveAll(context, updated)
        return deleted
    }

    fun banUser(context: Context, userId: String): Boolean {
        return updateUserStatus(context, userId, STATUS_BANNED)
    }

    fun addCashPoints(context: Context, userId: String, amount: Int): Boolean {
        if (amount <= 0) return false

        val current = getAllUsers(context)
        var changed = false
        val updated = current.map {
            if (it.id == userId) {
                changed = true
                it.copy(cashPoints = it.cashPoints + amount)
            } else {
                it
            }
        }

        if (changed) saveAll(context, updated)
        return changed
    }

    fun getOrCreateAdminUser(context: Context): User {
        val existing = getAllUsers(context).firstOrNull { it.type.equals(TYPE_ADMIN, ignoreCase = true) }
        if (existing != null) return existing

        return addUser(
            context = context,
            name = "Admin",
            email = "admin@gmail.com",
            phone = "+94 70 000 0000",
            password = "Admin123",
            type = TYPE_ADMIN
        )
    }

    fun updateAdminProfile(
        context: Context,
        adminId: String,
        name: String,
        email: String,
        currentPassword: String,
        newPassword: String?
    ): UpdateAdminResult {
        val current = getAllUsers(context)
        val admin = current.firstOrNull { it.id == adminId && it.type.equals(TYPE_ADMIN, ignoreCase = true) }
            ?: return UpdateAdminResult(false, "Admin user not found")

        if (admin.password != currentPassword) {
            return UpdateAdminResult(false, "Current password is incorrect")
        }

        val nextPassword = newPassword?.trim().takeUnless { it.isNullOrBlank() } ?: admin.password
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        val updated = current.map {
            if (it.id == admin.id) {
                it.copy(
                    name = trimmedName,
                    email = trimmedEmail,
                    password = nextPassword
                )
            } else {
                it
            }
        }

        saveAll(context, updated)
        return UpdateAdminResult(true, "Profile updated successfully")
    }

    private fun saveAll(context: Context, users: List<User>) {
        val jsonArray = JSONArray()
        users.forEach { user -> jsonArray.put(user.toJson()) }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USERS, jsonArray.toString())
            .apply()
    }

    private fun JSONObject.toUser(): User {
        return User(
            id = optString("id", UUID.randomUUID().toString()),
            name = optString("name", "Unknown User"),
            email = optString("email", ""),
            phone = optString("phone", ""),
            password = optString("password", DEFAULT_PASSWORD),
            type = optString("type", TYPE_REQUESTER),
            status = optString("status", STATUS_ACTIVE),
            createdAt = optLong("createdAt", System.currentTimeMillis()),
            isProvider = optBoolean("isProvider", false),
            cashPoints = optInt("cashPoints", 0)
        )
    }

    private fun User.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("email", email)
            put("phone", phone)
            put("password", password)
            put("type", type)
            put("status", status)
            put("createdAt", createdAt)
            put("isProvider", isProvider)
            put("cashPoints", cashPoints)
        }
    }
}
