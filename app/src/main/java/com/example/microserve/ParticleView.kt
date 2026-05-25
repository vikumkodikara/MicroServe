package com.example.microserve

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

class ParticleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particle(
        var x: Float, var y: Float,
        val radius: Float, val alpha: Int,
        val speedX: Float, val speedY: Float
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB39DDB.toInt()
        style = Paint.Style.FILL
    }

    private var animator: ValueAnimator? = null
    private var viewWidth = 0
    private var viewHeight = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        initParticles()
        startAnimation()
    }

    private fun initParticles() {
        particles.clear()
        val count = 35
        for (i in 0 until count) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * viewWidth,
                    y = Random.nextFloat() * viewHeight,
                    radius = Random.nextFloat() * 3f + 1f,
                    alpha = Random.nextInt(30, 90),
                    speedX = (Random.nextFloat() - 0.5f) * 0.8f,
                    speedY = -(Random.nextFloat() * 1.2f + 0.3f)
                )
            )
        }
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                updateParticles()
                invalidate()
            }
            start()
        }
    }

    private fun updateParticles() {
        for (p in particles) {
            p.x += p.speedX
            p.y += p.speedY
            if (p.y < -10f) {
                p.y = viewHeight.toFloat() + 10f
                p.x = Random.nextFloat() * viewWidth
            }
            if (p.x < -10f) p.x = viewWidth.toFloat() + 10f
            if (p.x > viewWidth + 10f) p.x = -10f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (p in particles) {
            paint.alpha = p.alpha
            canvas.drawCircle(p.x, p.y, p.radius, paint)
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
