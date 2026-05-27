package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
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
            finish()
            overridePendingTransition(R.anim.nav_slide_in_left, R.anim.nav_slide_out_right)
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
                else -> checkEmailThenCreateAccount(
                    name = username,
                    email = email.lowercase(),
                    phone = mobile,
                    password = password
                )
            }
        }
    }

    private fun checkEmailThenCreateAccount(name: String, email: String, phone: String, password: String) {
        auth.fetchSignInMethodsForEmail(email)
            .addOnSuccessListener { result ->
                val methods = result.signInMethods.orEmpty()
                if (methods.isNotEmpty()) {
                    toast(getString(R.string.sign_up_error_email_in_use))
                    return@addOnSuccessListener
                }
                createAccount(name, email, phone, password)
            }
            .addOnFailureListener { error ->
                toast(error.localizedMessage ?: getString(R.string.sign_up_error_check_email))
            }
    }

    private fun createAccount(name: String, email: String, phone: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    toast("Sign up failed")
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
                    onSuccess = {
                        toast(getString(R.string.sign_up_success))
                        startActivity(SessionNavigator.mainIntent(this))
                        finish()
                    },
                    onFailure = { message ->
                        toast(message)
                        AppPreferences.saveSession(this, profile)
                        startActivity(SessionNavigator.mainIntent(this))
                        finish()
                    }
                )
            }
            .addOnFailureListener { error ->
                val message = when (error) {
                    is FirebaseAuthUserCollisionException -> getString(R.string.sign_up_error_email_in_use)
                    is FirebaseNetworkException -> getString(R.string.sign_up_error_network)
                    else -> error.localizedMessage ?: getString(R.string.sign_up_error_generic)
                }
                toast(message)
            }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
