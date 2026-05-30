package com.example.microserve

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import android.media.ExifInterface
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import kotlin.math.max
import kotlin.math.min

/**
 * Inline square image adjust (pinch + drag) on the post ads screen.
 */
class PostImageAdjuster(
    private val activity: AppCompatActivity,
    private val imageView: ImageView,
    private val placeholder: View,
    private val adjustControls: View,
    private val confirmButton: MaterialButton,
    private val cancelButton: MaterialButton,
    private val changeHint: View?,
    private val adjustHint: View?,
    private val imageContainer: View? = null,
    private val scrollParent: View? = null,
    private val onImageSaved: (savedPath: String) -> Unit
) {
    private var sourceBitmap: Bitmap? = null
    private val matrix = Matrix()
    private var isAdjusting = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val scaleDetector = ScaleGestureDetector(
        activity,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                matrix.postScale(
                    detector.scaleFactor,
                    detector.scaleFactor,
                    detector.focusX,
                    detector.focusY
                )
                imageView.imageMatrix = matrix
                return true
            }
        }
    )

    init {
        imageView.scaleType = ImageView.ScaleType.MATRIX
        listOf(changeHint, adjustHint).forEach { hint ->
            hint?.isClickable = false
            hint?.isFocusable = false
        }
        imageView.setOnTouchListener { _, event ->
            if (!isAdjusting) return@setOnTouchListener false

            var parent = imageView.parent
            while (parent is ViewGroup) {
                parent.requestDisallowInterceptTouchEvent(true)
                parent = parent.parent
            }
            scrollParent?.parent?.let { outer ->
                if (outer is ViewGroup) outer.requestDisallowInterceptTouchEvent(true)
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1 && !scaleDetector.isInProgress) {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        matrix.postTranslate(dx, dy)
                        imageView.imageMatrix = matrix
                        lastTouchX = event.x
                        lastTouchY = event.y
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    var p = imageView.parent
                    while (p is ViewGroup) {
                        p.requestDisallowInterceptTouchEvent(false)
                        p = p.parent
                    }
                    scrollParent?.parent?.let { outer ->
                        if (outer is ViewGroup) outer.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }

            scaleDetector.onTouchEvent(event)
            true
        }

        confirmButton.setOnClickListener { confirmCrop() }
        cancelButton.setOnClickListener { cancelAdjust() }
    }

    fun startAdjust(uri: Uri) {
        val bitmap = decodeBitmap(uri)
        if (bitmap == null) {
            Toast.makeText(
                activity,
                R.string.post_ads_image_load_failed,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        sourceBitmap?.recycle()
        sourceBitmap = bitmap
        isAdjusting = true

        blockContainerPick(true)
        placeholder.isVisible = false
        changeHint?.isVisible = false
        adjustHint?.isVisible = true
        adjustControls.isVisible = true
        imageView.isVisible = true
        imageView.isEnabled = true
        imageView.isClickable = true
        imageView.isFocusable = true
        imageView.bringToFront()
        adjustHint?.bringToFront()
        imageView.scaleType = ImageView.ScaleType.MATRIX
        imageView.setImageBitmap(bitmap)

        imageView.post {
            if (imageView.width > 0 && imageView.height > 0) {
                fitImageToSquare()
            } else {
                imageView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (imageView.width > 0 && imageView.height > 0) {
                            imageView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            fitImageToSquare()
                        }
                    }
                })
            }
        }
    }

    fun showSavedPreview(imagePath: String) {
        isAdjusting = false
        blockContainerPick(false)
        adjustControls.isVisible = false
        placeholder.isVisible = false
        imageView.isVisible = true
        imageView.isClickable = false
        imageView.isFocusable = false
        adjustHint?.isVisible = false
        changeHint?.isVisible = true
        PostImageHelper.loadPostImage(imageView, imagePath)
    }

    fun reset() {
        isAdjusting = false
        blockContainerPick(false)
        sourceBitmap?.recycle()
        sourceBitmap = null
        matrix.reset()
        imageView.setImageDrawable(null)
        imageView.isVisible = false
        imageView.isClickable = false
        imageView.isFocusable = false
        adjustControls.isVisible = false
        placeholder.isVisible = true
        adjustHint?.isVisible = false
        changeHint?.isVisible = false
    }

    private fun cancelAdjust() {
        sourceBitmap?.recycle()
        sourceBitmap = null
        isAdjusting = false
        matrix.reset()
        imageView.setImageDrawable(null)
        imageView.isVisible = false
        imageView.isClickable = false
        imageView.isFocusable = false
        adjustControls.isVisible = false
        placeholder.isVisible = true
        adjustHint?.isVisible = false
        changeHint?.isVisible = false
        blockContainerPick(false)
    }

    /** Block opening the gallery again without disabling touch on the preview (child views). */
    private fun blockContainerPick(block: Boolean) {
        imageContainer?.isClickable = !block
        imageContainer?.isFocusable = !block
        imageContainer?.isEnabled = true
        imageView.isEnabled = true
    }

    private fun fitImageToSquare() {
        val bitmap = sourceBitmap ?: return
        val viewW = imageView.width.toFloat()
        val viewH = imageView.height.toFloat()
        if (viewW <= 0f || viewH <= 0f) return

        val scale = max(viewW / bitmap.width, viewH / bitmap.height)
        matrix.reset()
        matrix.postScale(scale, scale)
        matrix.postTranslate(
            (viewW - bitmap.width * scale) / 2f,
            (viewH - bitmap.height * scale) / 2f
        )
        imageView.imageMatrix = matrix
    }

    private fun confirmCrop() {
        val cropped = cropVisibleSquare()
        if (cropped == null) {
            Toast.makeText(
                activity,
                R.string.post_ads_image_save_failed,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val savedPath = PostImageHelper.saveBitmap(activity, cropped)
        cropped.recycle()
        if (savedPath == null) {
            Toast.makeText(
                activity,
                R.string.post_ads_image_save_failed,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        isAdjusting = false
        sourceBitmap?.recycle()
        sourceBitmap = null
        onImageSaved(savedPath)
        showSavedPreview(savedPath)
    }

    private fun cropVisibleSquare(): Bitmap? {
        val bitmap = sourceBitmap ?: return null
        if (imageView.width <= 0 || imageView.height <= 0) return null

        val inverse = Matrix()
        if (!matrix.invert(inverse)) {
            return centerSquareCrop(bitmap)
        }

        val corners = floatArrayOf(
            0f, 0f,
            imageView.width.toFloat(), 0f,
            imageView.width.toFloat(), imageView.height.toFloat(),
            0f, imageView.height.toFloat()
        )
        inverse.mapPoints(corners)

        var left = corners[0]
        var top = corners[1]
        var right = corners[0]
        var bottom = corners[1]
        for (i in 0 until 4) {
            left = min(left, corners[i * 2])
            right = max(right, corners[i * 2])
            top = min(top, corners[i * 2 + 1])
            bottom = max(bottom, corners[i * 2 + 1])
        }

        val cropLeft = left.toInt().coerceIn(0, bitmap.width - 1)
        val cropTop = top.toInt().coerceIn(0, bitmap.height - 1)
        val cropRight = right.toInt().coerceIn(cropLeft + 1, bitmap.width)
        val cropBottom = bottom.toInt().coerceIn(cropTop + 1, bitmap.height)

        val width = cropRight - cropLeft
        val height = cropBottom - cropTop
        if (width <= 1 || height <= 1) {
            return centerSquareCrop(bitmap)
        }

        return Bitmap.createBitmap(bitmap, cropLeft, cropTop, width, height)
    }

    private fun centerSquareCrop(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        val copiedPath = PostImageHelper.copyPickedImage(activity, uri)
        if (copiedPath != null) {
            decodeBitmapFromPath(copiedPath)?.let { return it }
        }
        return decodeBitmapFromUri(uri)
    }

    private fun decodeBitmapFromPath(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val maxDim = 2048
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDim || bounds.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeFile(path, decodeOptions) ?: return null
        return applyExifRotationFromPath(path, decoded)
    }

    private fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        val resolver = activity.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val maxDim = 2048
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDim || bounds.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        return applyExifRotation(resolver, uri, decoded)
    }

    private fun applyExifRotationFromPath(path: String, bitmap: Bitmap): Bitmap {
        val rotation = try {
            val exif = ExifInterface(path)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (_: Exception) {
            0f
        }
        return rotateBitmap(bitmap, rotation)
    }

    private fun applyExifRotation(resolver: android.content.ContentResolver, uri: Uri, bitmap: Bitmap): Bitmap {
        val rotation = try {
            resolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (_: Exception) {
            0f
        }
        return rotateBitmap(bitmap, rotation)
    }

    private fun rotateBitmap(bitmap: Bitmap, rotation: Float): Bitmap {
        if (rotation == 0f) return bitmap
        val rotateMatrix = Matrix().apply { postRotate(rotation) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotateMatrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }
}
