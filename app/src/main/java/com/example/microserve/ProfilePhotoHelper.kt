package com.example.microserve

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import java.io.File

object ProfilePhotoHelper {

    private const val FILE_NAME = "profile_photo.jpg"

    fun getPhotoFile(context: Context): File = File(context.filesDir, FILE_NAME)

    fun hasLocalPhoto(context: Context): Boolean = getPhotoFile(context).exists()

    fun savePhotoFromUri(context: Context, sourceUri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                getPhotoFile(context).outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return false
            AppPreferences.setSessionPhotoUrl(context, getPhotoFile(context).absolutePath)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearPhoto(context: Context) {
        getPhotoFile(context).delete()
    }

    fun loadAvatar(context: Context, imageView: ImageView, displayName: String) {
        val file = getPhotoFile(context)
        if (file.exists()) {
            Glide.with(context)
                .load(file)
                .circleCrop()
                .placeholder(R.drawable.navprofile)
                .into(imageView)
            return
        }

        val photoUrl = AppPreferences.getSessionPhotoUrl(context)
        if (photoUrl.startsWith("http://", ignoreCase = true) ||
            photoUrl.startsWith("https://", ignoreCase = true)
        ) {
            Glide.with(context)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.navprofile)
                .error(R.drawable.navprofile)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.navprofile)
        }
    }
}
