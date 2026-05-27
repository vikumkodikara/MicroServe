package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivitySignUpBinding
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupWindowInsets()
        setupActions()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.signUpScroll) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }
    }

    private fun setupActions() {
        binding.alreadyHaveAccountLink.setOnClickListener {
            goToLogin(prefillEmail = null)
        }

        binding.createAccountButton.setOnClickListener {
            val username = binding.signUpUsernameInput.text?.toString()?.trim().orEmpty()
            val password = binding.signUpPasswordInput.text?.toString()?.trim().orEmpty()
            val email = binding.signUpEmailInput.text?.toString()?.trim().orEmpty()
            val mobile = binding.signUpMobileInput.text?.toString()?.trim().orEmpty()

            when {
                username.isEmpty() -> toast(getString(R.string.sign_up_error_name))
                password.isEmpty() -> toast(getString(R.string.login_error_password))
                password.length < 6 -> toast(getString(R.string.sign_up_error_password_length))
                email.isEmpty() -> toast(getString(R.string.sign_up_error_email))
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast(getString(R.string.login_error_invalid_email))
                mobile.isEmpty() -> toast(getString(R.string.sign_up_error_mobile))
                else -> createAccount(
                    name = username,
                    email = email.lowercase(),
                    phone = mobile,
                    password = password
                )
            }
        }
    }

    private fun createAccount(name: String, email: String, phone: String, password: String) {
        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    setLoading(false)
                    toast(getString(R.string.sign_up_error_generic))
                    return@addOnSuccessListener
                }

                val profile = UserProfile(
                    uid = user.uid,
                    name = name,
                    email = email,
                    phone = phone,
                    role = UserProfile.ROLE_USER
                )

                UserRepository.saveProfile(
                    context = this,
                    profile = profile,
                    persistSession = false,
                    onSuccess = { completeSignUpAndReturnToLogin(email) },
                    onFailure = {
                        // Auth account exists; still send user to log in manually.
                        completeSignUpAndReturnToLogin(email)
                    }
                )
            }
            .addOnFailureListener { error ->
                setLoading(false)
                val message = when (error) {
                    is FirebaseAuthUserCollisionException -> getString(R.string.sign_up_error_email_in_use)
                    is FirebaseNetworkException -> getString(R.string.sign_up_error_network)
                    else -> AuthErrorHelper.loginMessage(this, error)
                        .takeUnless { it == getString(R.string.login_error_wrong_credentials) }
                        ?: getString(R.string.sign_up_error_generic)
                }
                toast(message)
            }
    }

    private fun completeSignUpAndReturnToLogin(email: String) {
        auth.signOut()
        SessionNavigator.clearAuth(this)
        setLoading(false)
        showAccountCreatedDialog(email)
    }

    private fun showAccountCreatedDialog(email: String) {
        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle(R.string.sign_up_success_title)
            .setMessage(R.string.sign_up_success)
            .setCancelable(false)
            .setPositiveButton(R.string.action_ok) { dialog, _ ->
                dialog.dismiss()
                goToLogin(prefillEmail = email)
            }
            .show()
    }

    private fun goToLogin(prefillEmail: String?) {
        val intent = Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (!prefillEmail.isNullOrBlank()) {
                putExtra(LoginActivity.EXTRA_PREFILL_EMAIL, prefillEmail)
            }
        }
        startActivity(intent)
        finish()
        overridePendingTransition(R.anim.nav_slide_in_left, R.anim.nav_slide_out_right)
    }

    private fun setLoading(loading: Boolean) {
        binding.createAccountButton.isEnabled = !loading
        binding.alreadyHaveAccountLink.isEnabled = !loading
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
