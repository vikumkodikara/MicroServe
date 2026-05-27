package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            showLoading(false)
            toast("Google sign-in canceled")
            return@registerForActivityResult
        }

        val data = result.data ?: run {
            showLoading(false)
            toast("Google sign-in canceled")
            return@registerForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            GoogleAuthHelper.signInWithGoogleAccount(
                account = account,
                onSuccess = { user ->
                    loadProfileAndRoute(user, fallbackName = account.displayName)
                },
                onError = { message ->
                    showLoading(false)
                    toast(message)
                }
            )
        } catch (error: ApiException) {
            showLoading(false)
            toast(error.localizedMessage ?: "Google sign-in failed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupWindowInsets()
        setupActions()
        prefillEmailFromSignUp()
    }

    private fun prefillEmailFromSignUp() {
        intent.getStringExtra(EXTRA_PREFILL_EMAIL)?.takeIf { it.isNotBlank() }?.let { email ->
            binding.usernameInput.setText(AuthErrorHelper.normalizeEmail(email))
        }
    }

    private fun setupWindowInsets() {
        SystemUiHelper.setupFullBleedPurpleScreen(this, binding.loginScroll)
    }

    private fun setupActions() {
        binding.loginButton.setOnClickListener {
            val email = AuthErrorHelper.normalizeEmail(
                binding.usernameInput.text?.toString().orEmpty()
            )
            val password = binding.passwordInput.text?.toString()?.trim().orEmpty()

            when {
                email.isEmpty() -> toast(getString(R.string.login_error_username))
                password.isEmpty() -> toast(getString(R.string.login_error_password))
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast(getString(R.string.login_error_invalid_email))
                else -> {
                    showLoading(true)
                    signInWithEmail(email, password)
                }
            }
        }

        binding.signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            overridePendingTransition(R.anim.nav_slide_in_right, R.anim.nav_slide_out_left)
        }

        binding.forgotPasswordLink.setOnClickListener {
            sendPasswordReset()
        }

        binding.googleSignInButton.setOnClickListener {
            if (GoogleAuthHelper.getWebClientId(this).isNullOrBlank()) {
                toast(getString(R.string.login_google_not_configured))
                return@setOnClickListener
            }
            googleSignInLauncher.launch(GoogleAuthHelper.buildSignInClient(this).signInIntent)
            showLoading(true)
        }
    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    showLoading(false)
                    toast(getString(R.string.login_error_generic))
                    return@addOnSuccessListener
                }
                loadProfileAndRoute(user)
            }
            .addOnFailureListener { error ->
                showLoading(false)
                toast(AuthErrorHelper.loginMessage(this, error))
            }
    }

    private fun sendPasswordReset() {
        val email = AuthErrorHelper.normalizeEmail(
            binding.usernameInput.text?.toString().orEmpty()
        )
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast(getString(R.string.login_error_username))
            return
        }
        showLoading(true)
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                showLoading(false)
                toast(getString(R.string.login_reset_email_sent))
            }
            .addOnFailureListener { error ->
                showLoading(false)
                val message = if (error is FirebaseAuthInvalidUserException) {
                    getString(R.string.login_error_no_account)
                } else {
                    AuthErrorHelper.loginMessage(this, error)
                }
                toast(message)
            }
    }

    private fun loadProfileAndRoute(user: FirebaseUser, fallbackName: String? = null) {
        UserRepository.loadProfileAndRoute(
            context = this,
            user = user,
            fallbackName = fallbackName,
            onAdminRoute = { routeByRole(UserProfile.ROLE_ADMIN) },
            onUserRoute = { routeByRole(UserProfile.ROLE_USER) },
            onError = { message ->
                showLoading(false)
                toast(message)
            }
        )
    }

    private fun routeByRole(role: String) {
        showLoading(false)
        val target = if (role.equals(UserProfile.ROLE_ADMIN, ignoreCase = true)) {
            AdminDashboardActivity::class.java
        } else {
            Homepage::class.java
        }
        val intent = Intent(this, target).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_PREFILL_EMAIL = "prefill_email"
    }
}
