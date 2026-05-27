package com.example.microserve

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import kotlin.math.max
import kotlin.math.min

/**
 * Inline square image adjust (pinch + drag) on the same screen — no separate crop activity.
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
    private val onImageSaved: (savedPath: String) -> Unit
) {
    private var sourceBitmap: Bitmap? = null
    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private var isAdjusting = false

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

    private val gestureDetector = GestureDetector(
        activity,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                matrix.postTranslate(-distanceX, -distanceY)
                imageView.imageMatrix = matrix
                return true
            }
        }
    )

    init {
        imageView.scaleType = ImageView.ScaleType.MATRIX
        imageView.setOnTouchListener { _, event ->
            if (!isAdjusting) return@setOnTouchListener false
            var handled = scaleDetector.onTouchEvent(event)
            handled = gestureDetector.onTouchEvent(event) || handled
            if (event.action == MotionEvent.ACTION_UP) {
                savedMatrix.set(matrix)
            }
            handled
        }
        confirmButton.setOnClickListener { confirmCrop() }
        cancelButton.setOnClickListener { cancelAdjust() }
    }

    fun startAdjust(uri: Uri) {
        val bitmap = activity.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return

        sourceBitmap?.recycle()
        sourceBitmap = bitmap
        isAdjusting = true

        placeholder.isVisible = false
        changeHint?.isVisible = false
        adjustControls.isVisible = true
        imageView.isVisible = true
        imageView.setImageBitmap(bitmap)

        imageView.post {
            if (imageView.width > 0 && imageView.height > 0) {
                fitImageToSquare()
            }
        }
    }

    fun showSavedPreview(imagePath: String) {
        isAdjusting = false
        adjustControls.isVisible = false
        placeholder.isVisible = false
        imageView.isVisible = true
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        PostImageHelper.loadPostImage(imageView, imagePath)
        adjustHint?.isVisible = false
        changeHint?.isVisible = true
    }

    fun reset() {
        isAdjusting = false
        sourceBitmap?.recycle()
        sourceBitmap = null
        matrix.reset()
        imageView.setImageDrawable(null)
        imageView.isVisible = false
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
        adjustControls.isVisible = false
        placeholder.isVisible = true
        adjustHint?.isVisible = false
        changeHint?.isVisible = false
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
        savedMatrix.set(matrix)
        imageView.imageMatrix = matrix
    }

    private fun confirmCrop() {
        val cropped = cropVisibleSquare() ?: return
        val savedPath = PostImageHelper.saveBitmap(activity, cropped)
        cropped.recycle()
        if (savedPath == null) return

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
        if (!matrix.invert(inverse)) return null

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
        if (width <= 0 || height <= 0) return null

        return Bitmap.createBitmap(bitmap, cropLeft, cropTop, width, height)
    }
}
