// File: app/src/main/java/com/example/pedestrianai/video/GlBitmapEncoder.kt
package com.example.pedestrianai.video

import android.graphics.Bitmap
import android.media.*
import android.opengl.*
import android.util.Log
import android.view.Surface

class GlBitmapEncoder(width: Int, height: Int, fps: Int, bitRate: Int, outPath: String) {
    private val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }
    private val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
        configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }
    private val inputSurface: Surface = encoder.createInputSurface()
    private val muxer = MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private var track = -1
    private var muxerStarted = false
    private val bufferInfo = MediaCodec.BufferInfo()
    private val eglDisplay: EGLDisplay
    private val eglContext: EGLContext
    private val eglSurface: EGLSurface
    private val renderer = GlRenderer()

    init {
        encoder.start()
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val vers = IntArray(2)
        EGL14.eglInitialize(eglDisplay, vers, 0, vers, 1)
        val attribs = intArrayOf(EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE)
        val configs = arrayOfNulls<EGLConfig>(1)
        val num = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, attribs, 0, configs, 0, 1, num, 0)
        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
        val surfAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], inputSurface, surfAttribs, 0)
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
    }

    fun encodeBitmapFrame(bitmap: Bitmap, ptsUs: Long) {
        drain(false) // Drain any pending output before drawing
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        renderer.drawBitmap(bitmap)
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, ptsUs * 1000) // Convert micro to nano
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    private fun drain(endOfStream: Boolean) {
        if (endOfStream) {
            Log.d("GlBitmapEncoder", "Signaling end of input stream.")
            encoder.signalEndOfInputStream()
        }
        while (true) {
            val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return // We'll drain again on the next frame or at the end.
                    Log.d("GlBitmapEncoder", "No output buffer available, trying again.")
                }
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    Log.d("GlBitmapEncoder", "Encoder output format changed. Adding track to muxer.")
                    track = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
                outIndex >= 0 -> {
                    val buf = encoder.getOutputBuffer(outIndex) ?: continue
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0) {
                        check(muxerStarted) { "Muxer not started" }
                        buf.position(bufferInfo.offset)
                        buf.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(track, buf, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d("GlBitmapEncoder", "End of stream reached on drain.")
                        return
                    }
                }
                else -> {
                    Log.w("GlBitmapEncoder", "Unexpected result from dequeueOutputBuffer: $outIndex")
                }
            }
            // If we are draining at the end, we need to loop until the EOS is reached.
            // Otherwise, we can exit after one pass.
            if (endOfStream) continue else return
        }
    }

    fun finish() {
        drain(true)
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
        EGL14.eglDestroyContext(eglDisplay, eglContext)
        EGL14.eglTerminate(eglDisplay)
        encoder.stop()
        encoder.release()
        if (muxerStarted) {
            muxer.stop()
        }
        muxer.release()
        inputSurface.release()
        Log.d("GlBitmapEncoder", "Finished and released all resources.")
    }
}