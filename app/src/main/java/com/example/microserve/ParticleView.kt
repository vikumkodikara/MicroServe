package com.example.microserve

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class ParticleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particle(
        var x: Float, var y: Float,
        val radius: Float, val baseAlpha: Int,
        val speedX: Float, val speedY: Float,
        val pulseSpeed: Float, var pulsePhase: Float
    )

    private val particles = mutableListOf<Particle>()
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
    }

    private var animator: ValueAnimator? = null
    private var viewWidth = 0
    private var viewHeight = 0
    private val connectionDistance = 200f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        initParticles()
        startAnimation()
    }

    private fun initParticles() {
        particles.clear()
        for (i in 0 until 50) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * viewWidth,
                    y = Random.nextFloat() * viewHeight,
                    radius = Random.nextFloat() * 2.5f + 0.6f,
                    baseAlpha = Random.nextInt(30, 120),
                    speedX = (Random.nextFloat() - 0.5f) * 0.5f,
                    speedY = (Random.nextFloat() - 0.5f) * 0.5f,
                    pulseSpeed = Random.nextFloat() * 0.035f + 0.012f,
                    pulsePhase = Random.nextFloat() * Math.PI.toFloat() * 2f
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
                for (p in particles) {
                    p.x += p.speedX
                    p.y += p.speedY
                    p.pulsePhase += p.pulseSpeed
                    if (p.x < -20f) p.x = viewWidth + 20f
                    if (p.x > viewWidth + 20f) p.x = -20f
                    if (p.y < -20f) p.y = viewHeight + 20f
                    if (p.y > viewHeight + 20f) p.y = -20f
                }
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in particles.indices) {
            for (j in i + 1 until particles.size) {
                val a = particles[i]
                val b = particles[j]
                val dist = hypot(a.x - b.x, a.y - b.y)
                if (dist < connectionDistance) {
                    val alpha = ((1f - dist / connectionDistance) * 28).toInt()
                    linePaint.shader = LinearGradient(
                        a.x, a.y, b.x, b.y,
                        (alpha shl 24) or 0xA084E8,
                        (alpha shl 24) or 0x7A6BB3,
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawLine(a.x, a.y, b.x, b.y, linePaint)
                }
            }
        }
        linePaint.shader = null

        for (p in particles) {
            val pulse = (sin(p.pulsePhase.toDouble()) * 0.3f + 0.7f).toFloat()
            val a = (p.baseAlpha * pulse).toInt().coerceIn(0, 255)
            dotPaint.color = (a shl 24) or 0xB39DDB
            canvas.drawCircle(p.x, p.y, p.radius, dotPaint)
            if (p.radius > 1.8f) {
                dotPaint.color = ((a / 4) shl 24) or 0xB39DDB
                canvas.drawCircle(p.x, p.y, p.radius * 2.8f, dotPaint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
