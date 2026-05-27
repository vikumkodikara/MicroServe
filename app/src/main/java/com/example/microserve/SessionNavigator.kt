package com.example.microserve

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

object SessionNavigator {

    /**
     * True only when the user completed login and we saved a session that matches Firebase.
     */
    fun isLoggedIn(context: Context): Boolean {
        if (!AppPreferences.isLoggedIn(context)) return false
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return false
        return firebaseUser.uid == AppPreferences.getSessionUid(context)
    }

    fun mainIntent(context: Context): Intent {
        val role = AppPreferences.getSessionRole(context)
        val target = if (role.equals(UserProfile.ROLE_ADMIN, ignoreCase = true)) {
            AdminDashboardActivity::class.java
        } else {
            Homepage::class.java
        }
        return Intent(context, target).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    fun loginIntent(context: Context): Intent {
        return Intent(context, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    /** Clears local session and Firebase/Google sign-in so Login is shown fresh. */
    fun clearAuth(context: Context) {
        AppPreferences.clearSession(context)
        FirebaseAuth.getInstance().signOut()
        try {
            GoogleSignIn.getClient(
                context,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut()
        } catch (_: Exception) {
            // Google Sign-In may not be configured
        }
    }
}
