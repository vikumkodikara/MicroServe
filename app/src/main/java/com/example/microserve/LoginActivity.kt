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
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: run {
            toast("Google sign-in canceled")
            return@registerForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            signInWithGoogle(account)
        } catch (_: ApiException) {
            toast("Google sign-in failed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        googleSignInClient = buildGoogleSignInClient()

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
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast(getString(R.string.login_error_invalid_email))
                password.isEmpty() -> toast(getString(R.string.login_error_password))
                else -> {
                    signInWithEmail(email, password)
                }
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
            val intent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(intent)
        }
    }

    private fun buildGoogleSignInClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(this, options)
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

    private fun signInWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    toast("Google sign-in failed")
                    return@addOnSuccessListener
                }
                loadProfileAndRoute(user, fallbackName = account.displayName)
            }
            .addOnFailureListener { error ->
                toast(error.localizedMessage ?: "Google sign-in failed")
            }
    }

    private fun loadProfileAndRoute(user: FirebaseUser, fallbackName: String? = null) {
        val docRef = firestore.collection(UserProfile.COLLECTION).document(user.uid)
        docRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val role = doc.getString(UserProfile.FIELD_ROLE) ?: UserProfile.ROLE_USER
                    routeByRole(role)
                } else {
                    val name = fallbackName?.takeIf { it.isNotBlank() }
                        ?: user.displayName
                        ?: user.email?.substringBefore("@")
                        ?: ""
                    val profile = UserProfile(
                        uid = user.uid,
                        name = name,
                        email = user.email.orEmpty(),
                        phone = "",
                        role = UserProfile.ROLE_USER
                    )
                    docRef.set(profile.toMap())
                        .addOnSuccessListener { routeByRole(profile.role) }
                        .addOnFailureListener { error ->
                            toast(error.localizedMessage ?: "Unable to save profile")
                        }
                }
            }
            .addOnFailureListener { error ->
                toast(error.localizedMessage ?: "Unable to load profile")
            }
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
