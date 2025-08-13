// File: app/src/main/java/com/example/pedestrianai/ml/TFLiteDetector.kt
package com.example.pedestrianai.ml

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.example.pedestrianai.data.DetectionResult
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class TFLiteDetector(
    val context: Context,
    val modelPath: String,
    var detectorListener: DetectorListener? = null
) {
    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
            results: List<DetectionResult>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }

    private var objectDetector: ObjectDetector? = null

    init {
        setupObjectDetector()
    }

    private fun setupObjectDetector() {
        val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
            .setScoreThreshold(0.5f)
            .setMaxResults(10)
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(4)
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())
        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(
                context,
                modelPath,
                optionsBuilder.build()
            )
        } catch (e: Exception) {
            detectorListener?.onError("TFLite failed to load the model: ${e.message}")
            Log.e("TFLite", "TFLite failed to load the model.", e)
        }
    }

    /**
     * This is the main detection function. It is now robust and handles bitmap conversion internally.
     */
    fun detect(image: Bitmap, imageRotation: Int): List<DetectionResult> {
        if (objectDetector == null) {
            setupObjectDetector()
        }

        // *** THE DEFINITIVE FIX IS HERE ***
        // 1. Check if the incoming bitmap needs conversion.
        val needsConversion = image.config != Bitmap.Config.ARGB_8888
        val bitmapToProcess = if (needsConversion) {
            // If it's not ARGB_8888, create a mutable copy in the correct format.
            Log.d("TFLiteDetector", "Bitmap config is ${image.config}, converting to ARGB_8888.")
            image.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            // Otherwise, use the original image.
            image
        }

        val inferenceTime = SystemClock.uptimeMillis()

        // 2. The rotation is for the live camera feed; for video frames it will be 0.
        val imageProcessor = ImageProcessor.Builder()
            .add(Rot90Op(-imageRotation / 90))
            .build()

        // 3. Now we are GUARANTEED to be passing an ARGB_8888 bitmap.
        val tensorImage = TensorImage.fromBitmap(bitmapToProcess)
        val finalTensorImage = imageProcessor.process(tensorImage)

        // 4. Run detection.
        val results = objectDetector?.detect(finalTensorImage)
        val finalInferenceTime = SystemClock.uptimeMillis() - inferenceTime

        // 5. If we created a temporary copy, recycle it to save memory.
        if (needsConversion) {
            bitmapToProcess.recycle()
        }

        val detectionResults = results?.mapNotNull { detection ->
            detection.categories.firstOrNull { it.label == "person" }?.let { category ->
                DetectionResult(
                    boundingBox = detection.boundingBox,
                    text = "Pedestrian ${"%.2f".format(category.score)}",
                    confidence = category.score
                )
            }
        } ?: emptyList()

        detectorListener?.onResults(
            detectionResults,
            finalInferenceTime,
            finalTensorImage.height,
            finalTensorImage.width
        )

        return detectionResults
    }

    fun close() {
        objectDetector?.close()
        objectDetector = null
    }
}