package com.example.claims.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.example.claims.R
import com.example.claims.domain.ClaimStatus

/**
 * Widok XML rysowany na Canvas: trzy kroki likwidacji szkody połączone linią.
 * Używany w Compose przez AndroidView (ClaimListScreen) — pokazuje interop View ↔ Compose.
 */
class ClaimProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var activeColor = ContextCompat.getColor(context, R.color.claim_progress_active)
    private var inactiveColor = ContextCompat.getColor(context, R.color.claim_progress_inactive)
    private var rejectedColor = ContextCompat.getColor(context, R.color.claim_progress_rejected)
    private var radius = 6f * resources.displayMetrics.density
    private val lineWidth = 3f * resources.displayMetrics.density

    // Paint tworzony raz — nigdy w onDraw (alokacje w onDraw = jank).
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = lineWidth
    }

    var status: ClaimStatus = ClaimStatus.SUBMITTED
        set(value) {
            if (field == value) return
            field = value
            updateContentDescription()
            invalidate()
        }

    init {
        context.withStyledAttributes(attrs, R.styleable.ClaimProgressView, defStyleAttr) {
            activeColor = getColor(R.styleable.ClaimProgressView_activeColor, activeColor)
            inactiveColor = getColor(R.styleable.ClaimProgressView_inactiveColor, inactiveColor)
            rejectedColor = getColor(R.styleable.ClaimProgressView_rejectedColor, rejectedColor)
            radius = getDimension(R.styleable.ClaimProgressView_stepRadius, radius)
        }
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        updateContentDescription()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (radius * 2 + lineWidth).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val progress = ClaimProgress.of(status)
        val left = paddingLeft + radius
        val right = width - paddingRight - radius
        val cy = paddingTop + (height - paddingTop - paddingBottom) / 2f
        val step = (right - left) / (ClaimProgress.STEPS - 1)

        for (i in 0 until ClaimProgress.STEPS - 1) {
            linePaint.color = if (i < progress.reachedStep) activeColor else inactiveColor
            canvas.drawLine(left + i * step, cy, left + (i + 1) * step, cy, linePaint)
        }
        for (i in 0 until ClaimProgress.STEPS) {
            dotPaint.color = when {
                i > progress.reachedStep -> inactiveColor
                progress.rejected && i == progress.reachedStep -> rejectedColor
                else -> activeColor
            }
            canvas.drawCircle(left + i * step, cy, radius, dotPaint)
        }
    }

    private fun updateContentDescription() {
        contentDescription = context.getString(R.string.claim_progress_description, context.getString(status.label()))
    }
}
