package com.example.microserve

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object UserRepository {

    private const val TAG = "UserRepository"

    /** Default admin credentials — used to seed Firebase Auth + Firestore. */
    const val ADMIN_EMAIL = "admin@microserve.local"
    const val ADMIN_PASSWORD = "admin123"
    private const val ADMIN_NAME = "Admin"
    private const val ADMIN_PHONE = "+94 70 000 0000"

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    /**
     * Ensures the admin account exists in Firebase Auth and has a Firestore
     * profile with role = "admin".  Safe to call on every app launch — it
     * silently succeeds if the account already exists.
     */
    fun ensureAdminExists() {
        val auth = FirebaseAuth.getInstance()

        auth.createUserWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD)
            .addOnSuccessListener { result ->
                // First-time creation — write the Firestore profile
                val user = result.user ?: return@addOnSuccessListener
                val profile = UserProfile(
                    uid = user.uid,
                    name = ADMIN_NAME,
                    email = ADMIN_EMAIL,
                    phone = ADMIN_PHONE,
                    role = UserProfile.ROLE_ADMIN
                )
                firestore.collection(UserProfile.COLLECTION)
                    .document(user.uid)
                    .set(profile.toMap())
                    .addOnSuccessListener { Log.d(TAG, "Admin profile seeded in Firestore") }
                    .addOnFailureListener { Log.w(TAG, "Failed to seed admin profile", it) }

                // Sign out so the splash/login screen doesn't auto-skip
                auth.signOut()
            }
            .addOnFailureListener { error ->
                if (error is FirebaseAuthUserCollisionException) {
                    // Account already exists — make sure Firestore doc has admin role
                    ensureAdminRole()
                } else {
                    Log.w(TAG, "ensureAdminExists failed", error)
                }
            }
    }

    /**
     * Signs in as admin temporarily to verify / fix the Firestore role,
     * then signs out so the current session is not affected.
     */
    private fun ensureAdminRole() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            Log.d(TAG, "Skipping admin role sync — a user is already signed in")
            return
        }

        val previousUser = auth.currentUser

        auth.signInWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                firestore.collection(UserProfile.COLLECTION)
                    .document(user.uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val currentRole = doc.getString(UserProfile.FIELD_ROLE)
                        if (currentRole == null || !currentRole.equals(UserProfile.ROLE_ADMIN, ignoreCase = true)) {
                            // Fix the role
                            val profile = UserProfile(
                                uid = user.uid,
                                name = doc.getString(UserProfile.FIELD_NAME) ?: ADMIN_NAME,
                                email = ADMIN_EMAIL,
                                phone = doc.getString(UserProfile.FIELD_PHONE) ?: ADMIN_PHONE,
                                role = UserProfile.ROLE_ADMIN
                            )
                            firestore.collection(UserProfile.COLLECTION)
                                .document(user.uid)
                                .set(profile.toMap())
                        }
                        // Restore previous auth state
                        if (previousUser == null) auth.signOut()
                    }
            }
            .addOnFailureListener { Log.w(TAG, "ensureAdminRole sign-in failed", it) }
    }

    fun saveProfile(
        context: Context,
        profile: UserProfile,
        persistSession: Boolean = true,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(UserProfile.COLLECTION)
            .document(profile.uid)
            .set(profile.toMap())
            .addOnSuccessListener {
                syncProfileToUserStore(context, profile)
                if (persistSession) {
                    AppPreferences.saveSession(context, profile)
                }
                onSuccess()
            }
            .addOnFailureListener { error ->
                syncProfileToUserStore(context, profile)
                if (persistSession) {
                    AppPreferences.saveSession(context, profile)
                }
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
        // Always check Firestore for the user's role — no local shortcuts
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
                        location = doc.getString(UserProfile.FIELD_LOCATION).orEmpty(),
                        photoUrl = doc.getString(UserProfile.FIELD_PHOTO_URL)
                            ?: user.photoUrl?.toString().orEmpty(),
                        role = doc.getString(UserProfile.FIELD_ROLE) ?: UserProfile.ROLE_USER,
                        createdAt = doc.getLong(UserProfile.FIELD_CREATED_AT) ?: System.currentTimeMillis()
                    )
                    syncProfileToUserStore(context, profile)
                    AppPreferences.saveSession(context, profile)
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
                AppPreferences.saveSession(context, profile)
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
            photoUrl = user.photoUrl?.toString().orEmpty(),
            role = UserProfile.ROLE_USER
        )
    }
}

