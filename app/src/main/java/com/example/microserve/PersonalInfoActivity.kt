package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class PersonalInfoActivity : AppCompatActivity() {

    private lateinit var imgProfile: ImageView
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etLocation: EditText

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadFormFromSession()
            loadProfileImage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fragment_personal_info)

        imgProfile = findViewById(R.id.img_profile)
        etFullName = findViewById(R.id.et_full_name)
        etEmail = findViewById(R.id.et_email)
        etPhone = findViewById(R.id.et_phone)
        etLocation = findViewById(R.id.et_location)

        setFieldsReadOnly(etFullName, etEmail, etPhone, etLocation)

        loadFormFromSession()
        loadProfileImage()

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
            editProfileLauncher.launch(Intent(this, EditProfileActivity::class.java))
        }

        HomeBottomNavHelper.setup(this, HomeBottomNavHelper.TAB_PROFILE)
    }

    override fun onResume() {
        super.onResume()
        loadFormFromSession()
        loadProfileImage()
    }

    private fun setFieldsReadOnly(vararg fields: EditText) {
        fields.forEach { field ->
            field.isFocusable = false
            field.isFocusableInTouchMode = false
            field.isClickable = false
            field.isLongClickable = false
            field.keyListener = null
        }
    }

    private fun loadFormFromSession() {
        val profile = AppPreferences.getSessionProfile(this)
        etFullName.setText(profile.name.ifBlank { "" })
        etEmail.setText(profile.email.ifBlank { "" })
        etPhone.setText(profile.phone.ifBlank { "" })
        etLocation.setText(profile.location.ifBlank { "" })
    }

    private fun loadProfileImage() {
        ProfilePhotoHelper.loadAvatar(this, imgProfile, etFullName.text.toString())
    }
}
