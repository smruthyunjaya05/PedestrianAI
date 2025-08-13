// File: app/src/main/java/com/example/pedestrianai/ui/view/OverlayView.kt
package com.example.pedestrianai.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.pedestrianai.data.DetectionResult
import kotlin.math.max

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var results: List<DetectionResult> = emptyList()
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    // Paint for the blue bounding box
    private val boxPaint = Paint().apply {
        color = Color.parseColor("#007AFF") // BrightBlue color
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    // Paint for the text label
    private val textPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = 45f
        textAlign = Paint.Align.LEFT
    }

    // Paint for the semi-transparent text background
    private val textBackgroundPaint = Paint().apply {
        color = Color.parseColor("#99007AFF") // Semi-transparent blue
        style = Paint.Style.FILL
    }

    fun setResults(
        detectionResults: List<DetectionResult>,
        imageHeight: Int,
        imageWidth: Int
    ) {
        this.results = detectionResults
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (results.isEmpty() || imageWidth == 0 || imageHeight == 0) {
            return
        }

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        for (result in results) {
            val boundingBox = result.boundingBox

            val scaledBoundingBox = RectF(
                boundingBox.left * scaleX,
                boundingBox.top * scaleY,
                boundingBox.right * scaleX,
                boundingBox.bottom * scaleY
            )

            // Draw the blue bounding box
            canvas.drawRect(scaledBoundingBox, boxPaint)

            // --- THIS IS THE FIX ---
            // We now get the pre-formatted text directly from the DetectionResult
            val label = result.text

            val textBounds = android.graphics.Rect()
            textPaint.getTextBounds(label, 0, label.length, textBounds)
            val textHeight = textBounds.height()

            val textBgLeft = scaledBoundingBox.left
            val textBgTop = max(0f, scaledBoundingBox.top - textHeight - 8f)
            val textBgRight = textBgLeft + textBounds.width() + 8f
            val textBgBottom = scaledBoundingBox.top
            canvas.drawRect(textBgLeft, textBgTop, textBgRight, textBgBottom, textBackgroundPaint)

            canvas.drawText(
                label,
                textBgLeft + 4f,
                textBgBottom - 4f,
                textPaint
            )
        }
    }
}