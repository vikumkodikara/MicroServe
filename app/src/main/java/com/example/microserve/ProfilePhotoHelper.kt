package com.example.microserve

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

object ProfilePhotoHelper {

    private const val TAG = "ProfilePhotoHelper"
    private const val FILE_NAME = "profile_photo.jpg"
    private const val STORAGE_PATH = "profile_photos"

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

    /**
     * Uploads the local profile photo to Firebase Storage at
     * `profile_photos/{uid}.jpg`, then updates the Firestore user doc's
     * `photoUrl` field with the download URL.
     */
    fun uploadToFirebaseStorage(
        context: Context,
        uid: String,
        onComplete: ((success: Boolean, url: String?) -> Unit)? = null
    ) {
        val file = getPhotoFile(context)
        if (!file.exists() || uid.isBlank()) {
            onComplete?.invoke(false, null)
            return
        }

        val storageRef = FirebaseStorage.getInstance()
            .reference
            .child("$STORAGE_PATH/$uid.jpg")

        storageRef.putFile(Uri.fromFile(file))
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val url = downloadUrl.toString()
                    Log.d(TAG, "Profile photo uploaded: $url")

                    // Update Firestore user doc with the cloud URL
                    FirebaseFirestore.getInstance()
                        .collection(UserProfile.COLLECTION)
                        .document(uid)
                        .update(UserProfile.FIELD_PHOTO_URL, url)
                        .addOnSuccessListener {
                            Log.d(TAG, "Firestore photoUrl updated for $uid")
                            AppPreferences.setSessionPhotoUrl(context, url)
                            onComplete?.invoke(true, url)
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to update Firestore photoUrl", e)
                            // Still save the URL locally even if Firestore update fails
                            AppPreferences.setSessionPhotoUrl(context, url)
                            onComplete?.invoke(true, url)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to upload profile photo", e)
                onComplete?.invoke(false, null)
            }
    }

    /**
     * Downloads the user's profile photo from Firebase Storage to
     * the local file, so it's available offline.
     */
    fun downloadFromFirebaseStorage(
        context: Context,
        uid: String,
        onComplete: ((success: Boolean) -> Unit)? = null
    ) {
        if (uid.isBlank()) {
            onComplete?.invoke(false)
            return
        }

        val storageRef = FirebaseStorage.getInstance()
            .reference
            .child("$STORAGE_PATH/$uid.jpg")

        val localFile = getPhotoFile(context)

        storageRef.getFile(localFile)
            .addOnSuccessListener {
                Log.d(TAG, "Profile photo downloaded for $uid")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "No profile photo in Storage for $uid (or download failed): ${e.message}")
                onComplete?.invoke(false)
            }
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
