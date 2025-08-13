// File: app/src/main/java/com/example/pedestrianai/util/VideoYuvToRgbConverter.kt
package com.example.pedestrianai.util

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import kotlin.math.min

/**
 * Standalone YUV_420_888 -> ARGB converter for *video* frames.
 * Handles row/pixel strides correctly. No RenderScript.
 */
object VideoYuvToRgbConverter {
    fun toBitmap(image: Image, outBitmap: Bitmap) {
        require(image.format == ImageFormat.YUV_420_888)
        val width = image.width
        val height = image.height
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        yBuffer.rewind(); uBuffer.rewind(); vBuffer.rewind()
        val argb = IntArray(width * height)
        val yRow = ByteArray(yRowStride)
        val uRow = ByteArray(uvRowStride)
        val vRow = ByteArray(uvRowStride)
        var outIdx = 0
        for (row in 0 until height) {
            yBuffer.position(row * yRowStride)
            yBuffer.get(yRow, 0, min(yRowStride, yRow.size))
            val uvRowIndex = row / 2
            uBuffer.position(uvRowIndex * uvRowStride)
            vBuffer.position(uvRowIndex * uvRowStride)
            uBuffer.get(uRow, 0, min(uvRowStride, uRow.size))
            vBuffer.get(vRow, 0, min(uvRowStride, vRow.size))
            var col = 0
            while (col < width) {
                val y = (yRow[col].toInt() and 0xFF)
                val uvCol = (col / 2) * uvPixelStride
                val u = (uRow[uvCol].toInt() and 0xFF) - 128
                val v = (vRow[uvCol].toInt() and 0xFF) - 128
                val c = y - 16
                var r = (298 * c + 409 * v + 128) shr 8
                var g = (298 * c - 100 * u - 208 * v + 128) shr 8
                var b = (298 * c + 516 * u + 128) shr 8
                if (r < 0) r = 0 else if (r > 255) r = 255
                if (g < 0) g = 0 else if (g > 255) g = 255
                if (b < 0) b = 0 else if (b > 255) b = 255
                argb[outIdx++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                col++
            }
        }
        outBitmap.setPixels(argb, 0, width, 0, 0, width, height)
    }
}