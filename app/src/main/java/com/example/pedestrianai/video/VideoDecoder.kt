// File: app/src/main/java/com/example/pedestrianai/video/VideoDecoder.kt
package com.example.pedestrianai.video

import android.content.Context
import android.graphics.ImageFormat // <<< THE FIX IS HERE
import android.media.*
import android.net.Uri

suspend fun decodeFrames(
    context: Context,
    videoUri: Uri,
    onFrame: suspend (image: Image, width: Int, height: Int, ptsUs: Long) -> Unit
) {
    val extractor = MediaExtractor().apply { setDataSource(context, videoUri, null) }
    var trackIndex = -1
    var format: MediaFormat? = null
    for (i in 0 until extractor.trackCount) {
        val f = extractor.getTrackFormat(i)
        if ((f.getString(MediaFormat.KEY_MIME) ?: "").startsWith("video/")) {
            trackIndex = i; format = f; break
        }
    }
    require(trackIndex >= 0 && format != null) { "No video track" }
    extractor.selectTrack(trackIndex)
    val width = format!!.getInteger(MediaFormat.KEY_WIDTH)
    val height = format.getInteger(MediaFormat.KEY_HEIGHT)
    val imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 3)
    val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
    decoder.configure(format, imageReader.surface, null, 0)
    decoder.start()
    val bufferInfo = MediaCodec.BufferInfo()
    var inputDone = false
    var outputDone = false
    while (!outputDone) {
        if (!inputDone) {
            val inIndex = decoder.dequeueInputBuffer(20_000)
            if (inIndex >= 0) {
                val inBuf = decoder.getInputBuffer(inIndex)!!
                val sampleSize = extractor.readSampleData(inBuf, 0)
                if (sampleSize < 0) {
                    decoder.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    inputDone = true
                } else {
                    val ptsUs = extractor.sampleTime
                    decoder.queueInputBuffer(inIndex, 0, sampleSize, ptsUs, 0)
                    extractor.advance()
                }
            }
        }
        val outIndex = decoder.dequeueOutputBuffer(bufferInfo, 20_000)
        when {
            outIndex >= 0 -> {
                val ptsUs = bufferInfo.presentationTimeUs
                decoder.releaseOutputBuffer(outIndex, true)
                val image = imageReader.acquireNextImage()
                if (image != null) {
                    try {
                        onFrame(image, width, height, ptsUs)
                    } finally {
                        image.close()
                    }
                }
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputDone = true
                }
            }
        }
    }
    decoder.stop(); decoder.release()
    imageReader.close()
    extractor.release()
}