package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.microserve.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupActions()
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
            val username = binding.usernameInput.text?.toString()?.trim().orEmpty()
            val password = binding.passwordInput.text?.toString()?.trim().orEmpty()

            when {
                username.isEmpty() -> toast(getString(R.string.login_error_username))
                password.isEmpty() -> toast(getString(R.string.login_error_password))
                else -> {
                    startActivity(
                        Intent(this, PostAdsActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
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
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
