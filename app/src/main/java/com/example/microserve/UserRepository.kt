package com.example.microserve

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object UserRepository {

    private const val TAG = "UserRepository"

    /** Default admin credentials — used to seed Firebase Auth + Firestore. */
    const val ADMIN_EMAIL = "admin@gmail.com"
    const val ADMIN_PASSWORD = "Admin123"
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
     * Queries Firestore for the admin profile by email and ensures the role
     * field is set to "admin". Does NOT sign in as admin — avoids changing
     * the current Firebase Auth user.
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
            .addOnFailureListener { Log.w(TAG, "ensureAdminRole query failed", it) }
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
                        cashPoints = doc.getLong(UserProfile.FIELD_CASH_POINTS)?.toInt() ?: 0,
                        createdAt = doc.getLong(UserProfile.FIELD_CREATED_AT) ?: System.currentTimeMillis()
                    )
                    syncProfileToUserStore(context, profile)
                    AppPreferences.saveSession(context, profile)
                    // Download profile photo from Firebase Storage to local cache
                    ProfilePhotoHelper.downloadFromFirebaseStorage(context, profile.uid)
                    if (profile.role.equals(UserProfile.ROLE_ADMIN, ignoreCase = true)) {
                        onAdminRoute()
                    } else {
                        onUserRoute()
                    }
                } else {
                    // Doc doesn't exist yet — build one
                    val isAdmin = user.email.equals(ADMIN_EMAIL, ignoreCase = true)
                    val profile = buildProfile(user, fallbackName).let {
                        if (isAdmin) it.copy(role = UserProfile.ROLE_ADMIN) else it
                    }
                    saveProfile(
                        context = context,
                        profile = profile,
                        onSuccess = { if (isAdmin) onAdminRoute() else onUserRoute() },
                        onFailure = { message ->
                            onError(message)
                            if (isAdmin) onAdminRoute() else onUserRoute()
                        }
                    )
                }
            }
            .addOnFailureListener { error ->
                val isAdmin = user.email.equals(ADMIN_EMAIL, ignoreCase = true)
                val profile = buildProfile(user, fallbackName).let {
                    if (isAdmin) it.copy(role = UserProfile.ROLE_ADMIN) else it
                }
                syncProfileToUserStore(context, profile)
                AppPreferences.saveSession(context, profile)
                onError(error.localizedMessage ?: "Unable to load profile")
                if (isAdmin) onAdminRoute() else onUserRoute()
            }
    }

    fun listenAllUsers(
        onUpdate: (List<UserProfile>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return firestore.collection(UserProfile.COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Failed to load users")
                    return@addSnapshotListener
                }
                val profiles = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data.orEmpty()
                    UserProfile(
                        uid = doc.id,
                        name = data[UserProfile.FIELD_NAME] as? String ?: "",
                        email = data[UserProfile.FIELD_EMAIL] as? String ?: "",
                        phone = data[UserProfile.FIELD_PHONE] as? String ?: "",
                        location = data[UserProfile.FIELD_LOCATION] as? String ?: "",
                        photoUrl = data[UserProfile.FIELD_PHOTO_URL] as? String ?: "",
                        role = data[UserProfile.FIELD_ROLE] as? String ?: UserProfile.ROLE_USER,
                        cashPoints = (data[UserProfile.FIELD_CASH_POINTS] as? Number)?.toInt() ?: 0,
                        createdAt = (data[UserProfile.FIELD_CREATED_AT] as? Number)?.toLong()
                            ?: System.currentTimeMillis()
                    )
                }.orEmpty().sortedByDescending { it.createdAt }
                onUpdate(profiles)
            }
    }

    fun getProfileById(
        uid: String,
        onSuccess: (UserProfile) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(UserProfile.COLLECTION)
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onFailure("User not found")
                    return@addOnSuccessListener
                }
                val data = doc.data.orEmpty()
                onSuccess(
                    UserProfile(
                        uid = doc.id,
                        name = data[UserProfile.FIELD_NAME] as? String ?: "",
                        email = data[UserProfile.FIELD_EMAIL] as? String ?: "",
                        phone = data[UserProfile.FIELD_PHONE] as? String ?: "",
                        location = data[UserProfile.FIELD_LOCATION] as? String ?: "",
                        photoUrl = data[UserProfile.FIELD_PHOTO_URL] as? String ?: "",
                        role = data[UserProfile.FIELD_ROLE] as? String ?: UserProfile.ROLE_USER,
                        cashPoints = (data[UserProfile.FIELD_CASH_POINTS] as? Number)?.toInt() ?: 0,
                        createdAt = (data[UserProfile.FIELD_CREATED_AT] as? Number)?.toLong()
                            ?: System.currentTimeMillis()
                    )
                )
            }
            .addOnFailureListener { error ->
                onFailure(error.localizedMessage ?: "Failed to load user")
            }
    }

    fun loadProviderUids(
        onSuccess: (Set<String>) -> Unit,
        onFailure: () -> Unit = {}
    ) {
        firestore.collection("services")
            .get()
            .addOnSuccessListener { snapshot ->
                val uids = snapshot.documents
                    .mapNotNull { it.getString("ownerUid")?.trim()?.takeIf { uid -> uid.isNotEmpty() } }
                    .toSet()
                onSuccess(uids)
            }
            .addOnFailureListener {
                onFailure()
            }
    }

    fun syncProfileToUserStore(context: Context, profile: UserProfile) {
        if (profile.uid.isBlank()) return

        UserStore.upsertFromProfile(
            context = context,
            profile = profile,
            type = UserStore.TYPE_REQUESTER
        )
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

