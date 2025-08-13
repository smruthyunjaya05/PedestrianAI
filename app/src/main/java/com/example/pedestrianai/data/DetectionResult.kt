// File: app/src/main/java/com/example/pedestrianai/data/DetectionResult.kt
package com.example.pedestrianai.data

import android.graphics.RectF

/**
 * A data class to hold the results from the TFLite Task Library.
 *
 * @property boundingBox The bounding box of the detected object.
 * @property text The formatted text label including the confidence score (e.g., "Pedestrian 0.87").
 * @property confidence The raw confidence score of the detection.
 */
data class DetectionResult(
    val boundingBox: RectF,
    val text: String,
    val confidence: Float // Renamed from 'score' to 'confidence' to match your detector
)