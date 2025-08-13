// File: app/src/main/java/com/example/pedestrianai/util/YuvToRgbConverter.kt
package com.example.pedestrianai.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.renderscript.*
import java.nio.ByteBuffer

/**
 * Helper class using RenderScript to efficiently convert YUV Image frames to RGB Bitmaps.
 * This is required because ExoPlayer provides frames in YUV format via ImageReader.
 */
class YuvToRgbConverter(context: Context) {
    private val rs = RenderScript.create(context)
    private val script = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
    private var yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB? = null
    private var yuvAllocation: Allocation? = null
    private var rgbAllocation: Allocation? = null

    @Synchronized
    fun yuvToRgb(image: Image, output: Bitmap) {
        if (yuvToRgbIntrinsic == null) {
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
        }

        val yuvType = Type.Builder(rs, Element.U8(rs)).setX(image.width).setY(image.height).setYuvFormat(ImageFormat.YUV_420_888)
        yuvAllocation = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)
        rgbAllocation = Allocation.createFromBitmap(rs, output)

        yuvAllocation!!.copyFrom(imageToByteArray(image))
        yuvToRgbIntrinsic!!.setInput(yuvAllocation)
        yuvToRgbIntrinsic!!.forEach(rgbAllocation)
        rgbAllocation!!.copyTo(output)
    }

    private fun imageToByteArray(image: Image): ByteArray {
        val yBuffer: ByteBuffer = image.planes[0].buffer
        val uBuffer: ByteBuffer = image.planes[1].buffer
        val vBuffer: ByteBuffer = image.planes[2].buffer
        val ySize: Int = yBuffer.remaining()
        val uSize: Int = uBuffer.remaining()
        val vSize: Int = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        return nv21
    }
}