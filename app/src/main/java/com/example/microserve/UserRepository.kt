package com.example.microserve

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object UserRepository {

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    fun saveProfile(
        context: Context,
        profile: UserProfile,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(UserProfile.COLLECTION)
            .document(profile.uid)
            .set(profile.toMap())
            .addOnSuccessListener {
                syncProfileToUserStore(context, profile)
                onSuccess()
            }
            .addOnFailureListener { error ->
                syncProfileToUserStore(context, profile)
                onFailure(error.localizedMessage ?: "Unable to save profile")
            }
    }

    fun loadProfileAndRoute(
        context: Context,
        user: FirebaseUser,
        fallbackName: String? = null,
        onAdminRoute: () -> Unit,
        onUserRoute: () -> Unit,
        onError: (String) -> Unit
    ) {
        val adminEmail = UserStore.getOrCreateAdminUser(context).email
        if (user.email.equals(adminEmail, ignoreCase = true)) {
            onAdminRoute()
            return
        }

        firestore.collection(UserProfile.COLLECTION)
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val profile = UserProfile(
                        uid = doc.getString(UserProfile.FIELD_UID) ?: user.uid,
                        name = doc.getString(UserProfile.FIELD_NAME).orEmpty(),
                        email = doc.getString(UserProfile.FIELD_EMAIL).orEmpty(),
                        phone = doc.getString(UserProfile.FIELD_PHONE).orEmpty(),
                        role = doc.getString(UserProfile.FIELD_ROLE) ?: UserProfile.ROLE_USER,
                        createdAt = doc.getLong(UserProfile.FIELD_CREATED_AT) ?: System.currentTimeMillis()
                    )
                    syncProfileToUserStore(context, profile)
                    if (profile.role.equals(UserProfile.ROLE_ADMIN, ignoreCase = true)) {
                        onAdminRoute()
                    } else {
                        onUserRoute()
                    }
                } else {
                    val profile = buildProfile(user, fallbackName)
                    saveProfile(
                        context = context,
                        profile = profile,
                        onSuccess = onUserRoute,
                        onFailure = { message ->
                            onError(message)
                            onUserRoute()
                        }
                    )
                }
            }
            .addOnFailureListener { error ->
                val profile = buildProfile(user, fallbackName)
                syncProfileToUserStore(context, profile)
                onError(error.localizedMessage ?: "Unable to load profile")
                onUserRoute()
            }
    }

    fun syncProfileToUserStore(context: Context, profile: UserProfile) {
        val existing = UserStore.getAllUsers(context)
            .firstOrNull { it.email.equals(profile.email, ignoreCase = true) }

        if (existing == null) {
            UserStore.addUser(
                context = context,
                name = profile.name,
                email = profile.email,
                phone = profile.phone,
                type = UserStore.TYPE_REQUESTER
            )
        }
    }

    private fun buildProfile(user: FirebaseUser, fallbackName: String?): UserProfile {
        val name = fallbackName?.takeIf { it.isNotBlank() }
            ?: user.displayName
            ?: user.email?.substringBefore("@")
            ?: ""

        return UserProfile(
            uid = user.uid,
            name = name,
            email = user.email.orEmpty(),
            phone = user.phoneNumber.orEmpty(),
            role = UserProfile.ROLE_USER
        )
    }
}
