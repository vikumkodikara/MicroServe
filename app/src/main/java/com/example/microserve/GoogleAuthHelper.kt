package com.example.microserve

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

object GoogleAuthHelper {

    fun buildSignInClient(context: Context): GoogleSignInClient {
        val optionsBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()

        getWebClientId(context)?.let { webClientId ->
            optionsBuilder.requestIdToken(webClientId)
        }

        return com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
            context,
            optionsBuilder.build()
        )
    }

    fun getWebClientId(context: Context): String? {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resourceId == 0) return null

        val value = context.getString(resourceId)
        if (value.isBlank() || value.contains("YOUR_WEB_CLIENT", ignoreCase = true)) {
            return null
        }
        return value
    }

    fun signInWithGoogleAccount(
        account: GoogleSignInAccount,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val idToken = account.idToken
        if (!idToken.isNullOrBlank()) {
            signInWithGoogleIdToken(idToken, onSuccess, onError)
            return
        }

        signInWithGoogleEmailFallback(account, onSuccess, onError)
    }

    private fun signInWithGoogleIdToken(
        idToken: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance()
            .signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onError("Google sign-in failed")
                } else {
                    onSuccess(user)
                }
            }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: "Google sign-in failed")
            }
    }

    private fun signInWithGoogleEmailFallback(
        account: GoogleSignInAccount,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val email = account.email?.trim().orEmpty()
        if (email.isEmpty()) {
            onError("Google account has no email address")
            return
        }

        val googleId = account.id?.trim().orEmpty()
        if (googleId.isEmpty()) {
            onError("Google account is missing an ID")
            return
        }

        val auth = FirebaseAuth.getInstance()
        val password = derivePassword(googleId)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onError("Google sign-in failed")
                } else {
                    onSuccess(user)
                }
            }
            .addOnFailureListener {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val user = result.user
                        if (user == null) {
                            onError("Google sign-in failed")
                        } else {
                            onSuccess(user)
                        }
                    }
                    .addOnFailureListener { createError ->
                        when (createError) {
                            is FirebaseAuthUserCollisionException -> {
                                onError("This email is already registered. Log in with email and password instead.")
                            }
                            else -> onError(createError.localizedMessage ?: "Google sign-in failed")
                        }
                    }
            }
    }

    internal fun derivePassword(googleId: String): String {
        val hash = googleId.hashCode().toUInt().toString(16)
        return "MsG_$hash!"
    }
}
