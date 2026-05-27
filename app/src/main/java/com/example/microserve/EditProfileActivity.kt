package com.example.microserve

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth

class EditProfileActivity : AppCompatActivity() {

    private lateinit var imgProfile: ImageView
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etLocation: EditText

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        if (ProfilePhotoHelper.savePhotoFromUri(this, uri)) {
            ProfilePhotoHelper.loadAvatar(this, imgProfile, etFullName.text.toString())
            Toast.makeText(this, R.string.profile_photo_saved, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.profile_photo_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)

        val root = findViewById<View>(R.id.edit_profile_root)
        val header = findViewById<View>(R.id.header_bg)
        SystemUiHelper.setupPurpleHeaderScreen(
            activity = this,
            root = root,
            headerView = header,
            footerBar = findViewById(R.id.footerBar)
        )

        imgProfile = findViewById(R.id.img_profile)
        etFullName = findViewById(R.id.et_full_name)
        etEmail = findViewById(R.id.et_email)
        etPhone = findViewById(R.id.et_phone)
        etLocation = findViewById(R.id.et_location)

        findViewById<View>(R.id.tv_title)?.let { title ->
            if (title is android.widget.TextView) {
                title.setText(R.string.edit_profile)
            }
        }

        loadForm()
        ProfilePhotoHelper.loadAvatar(this, imgProfile, etFullName.text.toString())

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val openPicker = View.OnClickListener { imagePicker.launch("image/*") }
        findViewById<MaterialCardView>(R.id.btn_camera).setOnClickListener(openPicker)
        findViewById<MaterialCardView>(R.id.profile_image_container).setOnClickListener(openPicker)

        findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
            saveProfile()
        }
    }

    private fun loadForm() {
        val profile = AppPreferences.getSessionProfile(this)
        etFullName.setText(profile.name)
        etEmail.setText(profile.email)
        etPhone.setText(profile.phone)
        etLocation.setText(profile.location)
    }

    private fun saveProfile() {
        val name = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val location = etLocation.text.toString().trim()

        when {
            name.isBlank() -> toast(getString(R.string.sign_up_error_name))
            email.isBlank() -> toast(getString(R.string.sign_up_error_email))
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast(getString(R.string.login_error_invalid_email))
            phone.isBlank() -> toast(getString(R.string.sign_up_error_mobile))
            else -> {
                val uid = AppPreferences.getSessionUid(this).ifBlank {
                    FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                }
                if (uid.isBlank()) {
                    toast(getString(R.string.profile_save_failed))
                    return
                }

                val existing = AppPreferences.getSessionProfile(this)
                val photoUrl = if (ProfilePhotoHelper.hasLocalPhoto(this)) {
                    ProfilePhotoHelper.getPhotoFile(this).absolutePath
                } else {
                    existing.photoUrl
                }

                val updated = existing.copy(
                    uid = uid,
                    name = name,
                    email = email,
                    phone = phone,
                    location = location,
                    photoUrl = photoUrl
                )

                UserRepository.saveProfile(
                    context = this,
                    profile = updated,
                    onSuccess = {
                        AppPreferences.saveSession(this, updated)
                        setResult(RESULT_OK)
                        Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onFailure = { message ->
                        AppPreferences.saveSession(this, updated)
                        setResult(RESULT_OK)
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
