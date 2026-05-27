package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class JobSuccessActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MESSAGE = "extra_message"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_success)

        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: getString(R.string.job_success_default)
        findViewById<TextView>(R.id.tv_success_message).text = message

        findViewById<android.view.View>(R.id.btn_back_home).setOnClickListener {
            startActivity(
                Intent(this, Homepage::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }
    }
}
