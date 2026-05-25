package com.example.microserve

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                username.isEmpty() -> toast(getString(R.string.login_error_username))
                password.isEmpty() -> toast(getString(R.string.login_error_password))
                email.isEmpty() -> toast(getString(R.string.sign_up_error_email))
                mobile.isEmpty() -> toast(getString(R.string.sign_up_error_mobile))
                else -> {
                    toast(getString(R.string.sign_up_success))
                    finish()
                    overridePendingTransition(R.anim.nav_slide_in_left, R.anim.nav_slide_out_right)
                }
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
