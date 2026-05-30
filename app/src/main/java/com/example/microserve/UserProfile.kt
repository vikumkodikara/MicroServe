package com.example.microserve

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val photoUrl: String = "",
    val role: String = ROLE_USER,
    val cashPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            FIELD_UID to uid,
            FIELD_NAME to name,
            FIELD_EMAIL to email,
            FIELD_PHONE to phone,
            FIELD_LOCATION to location,
            FIELD_PHOTO_URL to photoUrl,
            FIELD_ROLE to role,
            FIELD_CASH_POINTS to cashPoints,
            FIELD_CREATED_AT to createdAt
        )
    }

    fun toAdminListUser(providerUids: Set<String> = emptySet()): UserStore.User {
        val type = when {
            role.equals(ROLE_ADMIN, ignoreCase = true) -> UserStore.TYPE_ADMIN
            uid in providerUids -> UserStore.TYPE_PROVIDER
            else -> UserStore.TYPE_REQUESTER
        }
        return UserStore.User(
            id = uid,
            name = name.ifBlank { "Unknown User" },
            email = email,
            phone = phone,
            type = type,
            status = UserStore.STATUS_ACTIVE,
            createdAt = createdAt,
            cashPoints = cashPoints
        )
    }

    companion object {
        const val COLLECTION = "users"

        const val ROLE_ADMIN = "admin"
        const val ROLE_USER = "user"

        const val FIELD_UID = "uid"
        const val FIELD_NAME = "name"
        const val FIELD_EMAIL = "email"
        const val FIELD_PHONE = "phone"
        const val FIELD_LOCATION = "location"
        const val FIELD_PHOTO_URL = "photoUrl"
        const val FIELD_ROLE = "role"
        const val FIELD_CASH_POINTS = "cashPoints"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
