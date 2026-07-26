package com.jksalcedo.passvault.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import java.util.Locale
import kotlin.math.abs

class MonogramDrawable(
    private val text: String,
    private val backgroundColor: Int,
    private val textColor: Int = Color.WHITE
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val textBounds = Rect()

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        val radius = width.coerceAtMost(height) / 2f

        // Draw background circle
        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(bounds.centerX().toFloat(), bounds.centerY().toFloat(), radius, paint)

        // Draw character
        if (text.isNotEmpty()) {
            paint.color = textColor
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            // Text size is dynamically scaled to match the circle size
            paint.textSize = radius * 1.0f

            val letter = text.trim().substring(0, 1).uppercase(Locale.getDefault())
            paint.getTextBounds(letter, 0, 1, textBounds)

            // vertical centering
            val textY = bounds.centerY().toFloat() + (textBounds.height() / 2f) - textBounds.bottom
            canvas.drawText(letter, bounds.centerX().toFloat(), textY, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        // High-contrast
        private val PALETTE = intArrayOf(
            0xFFD32F2F.toInt(), // Red
            0xFFC2185B.toInt(), // Pink
            0xFF7B1FA2.toInt(), // Purple
            0xFF512DA8.toInt(), // Deep Purple
            0xFF303F9F.toInt(), // Indigo
            0xFF1976D2.toInt(), // Blue
            0xFF0288D1.toInt(), // Light Blue
            0xFF0097A7.toInt(), // Cyan
            0xFF00796B.toInt(), // Teal
            0xFF388E3C.toInt(), // Green
            0xFF689F38.toInt(), // Light Green
            0xFFF57C00.toInt(), // Orange
            0xFFE64A19.toInt(), // Deep Orange
            0xFF5D4037.toInt(), // Brown
            0xFF455A64.toInt()  // Blue Grey
        )

        /**
         * Creates a MonogramDrawable with a background color deterministically generated
         * from the hash code of the input text.
         */
        fun createWithHash(text: String): MonogramDrawable {
            val trimmed = text.trim()
            val initial = if (trimmed.isNotEmpty()) trimmed.substring(0, 1) else "?"
            val index = abs(text.hashCode()) % PALETTE.size
            val color = PALETTE[index]
            return MonogramDrawable(initial, color)
        }
    }
}