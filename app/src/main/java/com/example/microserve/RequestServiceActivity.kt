package com.example.microserve

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class RequestServiceActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val category = intent.getStringExtra(EXTRA_CATEGORY)
        startActivity(
            Intent(this, CreateRequestActivity::class.java).apply {
                if (!category.isNullOrBlank()) {
                    putExtra(CreateRequestActivity.EXTRA_CATEGORY, category)
                }
            }
        )
        finish()
    }
}
