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
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            toast("Google sign-in canceled")
            return@registerForActivityResult
        }

        val data = result.data ?: run {
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
                    toast(message)
                }
            )
        } catch (error: ApiException) {
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
    }

    override fun onStart() {
        super.onStart()
        auth.currentUser?.let { user ->
            loadProfileAndRoute(user)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.loginScroll) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }
    }

    private fun setupActions() {
        binding.loginButton.setOnClickListener {
            val email = binding.usernameInput.text?.toString()?.trim().orEmpty()
            val password = binding.passwordInput.text?.toString()?.trim().orEmpty()

            when {
                email.isEmpty() -> toast(getString(R.string.login_error_username))
                password.isEmpty() -> toast(getString(R.string.login_error_password))
                isAdminCredentials(email, password) -> loginAsAdmin()
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast(getString(R.string.login_error_invalid_email))
                else -> signInWithEmail(email, password)
            }
        }

        binding.signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            overridePendingTransition(R.anim.nav_slide_in_right, R.anim.nav_slide_out_left)
        }

        binding.forgotPasswordLink.setOnClickListener {
            toast(getString(R.string.login_forgot_password_soon))
        }

        binding.googleSignInButton.setOnClickListener {
            if (GoogleAuthHelper.getWebClientId(this).isNullOrBlank()) {
                toast(getString(R.string.login_google_not_configured))
                return@setOnClickListener
            }
            googleSignInLauncher.launch(GoogleAuthHelper.buildSignInClient(this).signInIntent)
        }

        binding.googleSignInButton.setSize(SignInButton.SIZE_WIDE)
        binding.googleSignInButton.setColorScheme(SignInButton.COLOR_LIGHT)
    }

    private fun isAdminCredentials(email: String, password: String): Boolean {
        val admin = UserStore.getOrCreateAdminUser(this)
        return email.equals(admin.email, ignoreCase = true) && password == admin.password
    }

    private fun loginAsAdmin() {
        toast(getString(R.string.login_admin_success))
        startActivity(
            Intent(this, AdminDashboardActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    toast("Login failed")
                    return@addOnSuccessListener
                }
                loadProfileAndRoute(user)
            }
            .addOnFailureListener { error ->
                toast(error.localizedMessage ?: "Login failed")
            }
    }

    private fun loadProfileAndRoute(user: FirebaseUser, fallbackName: String? = null) {
        UserRepository.loadProfileAndRoute(
            context = this,
            user = user,
            fallbackName = fallbackName,
            onAdminRoute = { routeByRole(UserProfile.ROLE_ADMIN) },
            onUserRoute = { routeByRole(UserProfile.ROLE_USER) },
            onError = { message -> toast(message) }
        )
    }

    private fun routeByRole(role: String) {
        val target = if (role.equals(UserProfile.ROLE_ADMIN, ignoreCase = true)) {
            AdminDashboardActivity::class.java
        } else {
            Homepage::class.java
        }

        startActivity(
            Intent(this, target)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
