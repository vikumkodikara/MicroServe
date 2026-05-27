package com.example.microserve

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import java.io.File
import java.util.UUID

object PostImageHelper {

    private const val POST_IMAGES_DIR = "post_images"

    /** Copy a gallery pick immediately while read permission is still valid. */
    fun copyPickedImage(context: Context, sourceUri: Uri): String? {
        val tempId = UUID.randomUUID().toString()
        return savePostImageFromUri(context, sourceUri, tempId)
    }

    fun savePostImageFromUri(context: Context, sourceUri: Uri, serviceId: String): String? {
        return try {
            val dir = File(context.filesDir, POST_IMAGES_DIR).apply { mkdirs() }
            val target = File(dir, "$serviceId.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            if (!target.exists() || target.length() <= 0L) return null
            target.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun attachToServiceId(context: Context, imagePath: String?, serviceId: String): String? {
        val path = imagePath?.takeIf { it.isNotBlank() } ?: return null
        val source = File(path)
        if (!source.exists()) return null

        val target = File(File(context.filesDir, POST_IMAGES_DIR), "$serviceId.jpg")
        target.parentFile?.mkdirs()

        return if (source.absolutePath == target.absolutePath) {
            target.absolutePath
        } else {
            source.copyTo(target, overwrite = true)
            if (source.name.startsWith("pick_") || !source.name.removeSuffix(".jpg").equals(serviceId, ignoreCase = true)) {
                source.delete()
            }
            target.absolutePath
        }
    }

    fun deletePostImage(context: Context, imageUri: String?) {
        val file = resolveLocalFile(context, imageUri) ?: return
        runCatching { file.delete() }
    }

    fun loadPostImage(imageView: ImageView, imageUri: String?) {
        val context = imageView.context
        val path = imageUri?.takeIf { it.isNotBlank() }
        val localFile = resolveLocalFile(context, path)

        if (localFile != null && localFile.exists()) {
            Glide.with(context)
                .load(localFile)
                .signature(ObjectKey(localFile.lastModified()))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .placeholder(R.drawable.home_job_done_placeholder)
                .error(R.drawable.home_job_done_placeholder)
                .into(imageView)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            return
        }

        if (path != null && (path.startsWith("content://") || path.startsWith("http://") || path.startsWith("https://"))) {
            Glide.with(context)
                .load(Uri.parse(path))
                .centerCrop()
                .placeholder(R.drawable.home_job_done_placeholder)
                .error(R.drawable.home_job_done_placeholder)
                .into(imageView)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            return
        }

        imageView.setImageResource(R.drawable.home_job_done_placeholder)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
    }

    private fun resolveLocalFile(context: Context, path: String?): File? {
        if (path.isNullOrBlank()) return null
        return when {
            path.startsWith("file://") -> Uri.parse(path).path?.let { File(it) }
            path.startsWith("/") -> File(path)
            path.startsWith(context.filesDir.absolutePath) -> File(path)
            else -> null
        }
    }
}
