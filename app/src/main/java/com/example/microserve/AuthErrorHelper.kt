package com.example.microserve

import android.content.Context
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

object AuthErrorHelper {

    fun normalizeEmail(raw: String): String {
        return raw.trim().lowercase()
    }

    fun loginMessage(context: Context, error: Exception): String {
        return when (error) {
            is FirebaseNetworkException -> context.getString(R.string.sign_up_error_network)
            is FirebaseAuthUserCollisionException -> context.getString(R.string.sign_up_error_email_in_use)
            is FirebaseAuthInvalidUserException -> context.getString(R.string.login_error_no_account)
            is FirebaseAuthInvalidCredentialsException -> context.getString(R.string.login_error_wrong_credentials)
            is FirebaseAuthException -> messageForAuthErrorCode(context, error)
            else -> {
                val raw = error.localizedMessage.orEmpty()
                if (raw.contains("credential", ignoreCase = true) ||
                    raw.contains("malformed", ignoreCase = true) ||
                    raw.contains("expired", ignoreCase = true)
                ) {
                    context.getString(R.string.login_error_wrong_credentials)
                } else {
                    raw.ifBlank { context.getString(R.string.login_error_generic) }
                }
            }
        }
    }

    private fun messageForAuthCode(context: Context, error: FirebaseAuthException): String {
        return when (error.errorCode) {
            "ERROR_INVALID_EMAIL" -> context.getString(R.string.login_error_invalid_email)
            "ERROR_USER_DISABLED" -> context.getString(R.string.login_error_user_disabled)
            "ERROR_USER_NOT_FOUND",
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_LOGIN_CREDENTIALS",
            "ERROR_INVALID_CREDENTIAL" -> context.getString(R.string.login_error_wrong_credentials)
            "ERROR_TOO_MANY_REQUESTS" -> context.getString(R.string.login_error_too_many_requests)
            "ERROR_NETWORK_REQUEST_FAILED" -> context.getString(R.string.sign_up_error_network)
            else -> {
                val raw = error.localizedMessage.orEmpty()
                if (raw.contains("credential", ignoreCase = true) ||
                    raw.contains("malformed", ignoreCase = true)
                ) {
                    context.getString(R.string.login_error_wrong_credentials)
                } else {
                    raw.ifBlank { context.getString(R.string.login_error_generic) }
                }
            }
        }
    }

    private fun messageForAuthErrorCode(context: Context, error: FirebaseAuthException): String {
        return messageForAuthCode(context, error)
    }
}
