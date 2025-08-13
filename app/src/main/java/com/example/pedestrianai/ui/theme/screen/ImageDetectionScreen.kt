// File: app/src/main/java/com/example/pedestrianai/ui/screen/ImageDetectionScreen.kt
package com.example.pedestrianai.ui.screen

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pedestrianai.data.DetectionResult
import com.example.pedestrianai.ml.TFLiteDetector
import com.example.pedestrianai.ui.theme.BackgroundGrey
import com.example.pedestrianai.ui.theme.BrightBlue
import com.example.pedestrianai.ui.theme.LightBlueBg
import com.example.pedestrianai.ui.theme.TextBlack
import com.example.pedestrianai.ui.theme.TextGrey
import java.text.DecimalFormat

val SuccessGreen = Color(0xFF28A745)

@Composable
fun ImageDetectionScreen(navController: NavController) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectionResults by remember { mutableStateOf<List<DetectionResult>?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            imageUri = uri
            detectionResults = null // Clear old results
            uri?.let {
                bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
        }
    )

    val detector = remember {
        TFLiteDetector(
            context = context,
            modelPath = "yolov8_pedestrian.tflite",
            detectorListener = object : TFLiteDetector.DetectorListener {
                override fun onError(error: String) {
                    isLoading = false
                    // TODO: Optionally show an error message
                }
                override fun onResults(
                    results: List<DetectionResult>?,
                    inferenceTime: Long,
                    imageHeight: Int,
                    imageWidth: Int
                ) {
                    detectionResults = results ?: emptyList()
                    isLoading = false
                }
            }
        )
    }

    LaunchedEffect(bitmap) {
        bitmap?.let {
            isLoading = true
            detector.detect(it, 0)
        }
    }

    fun resetState() {
        imageUri = null
        bitmap = null
        detectionResults = null
        isLoading = false
    }

    Scaffold(containerColor = BackgroundGrey) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Re-using the header from the Live Detection screen for consistency
            ScreenHeader(
                titlePart1 = "Image ",
                titlePart2 = "Analysis",
                onBackClicked = { navController.popBackStack() }
            )
            Spacer(modifier = Modifier.height(16.dp))

            when {
                detectionResults != null && bitmap != null -> {
                    ResultsScreen(
                        bitmap = bitmap!!,
                        results = detectionResults!!,
                        onAnalyzeAnother = { resetState() }
                    )
                }
                isLoading -> {
                    UploadScreen(
                        imageUri = imageUri,
                        isLoading = true,
                        onSelectClick = { imagePickerLauncher.launch("image/*") },
                        onClearClick = { resetState() }
                    )
                }
                else -> {
                    UploadScreen(
                        imageUri = imageUri,
                        isLoading = false,
                        onSelectClick = { imagePickerLauncher.launch("image/*") },
                        onClearClick = { resetState() }
                    )
                }
            }
        }
    }
}

@Composable
fun UploadScreen(
    imageUri: Uri?,
    isLoading: Boolean,
    onSelectClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Upload & Analyze", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextBlack)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Drop your image file for instant pedestrian detection analysis.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightBlueBg, RoundedCornerShape(16.dp))
                .border(
                    width = 2.dp,
                    color = BrightBlue,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(32.dp)
                .clickable(enabled = !isLoading) { onSelectClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = BrightBlue)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.UploadFile, contentDescription = "Upload", tint = BrightBlue, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    if (imageUri != null) {
                        Text("Selected: ${imageUri.lastPathSegment?.split("/")?.last()}", fontWeight = FontWeight.Bold, color = TextBlack, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Supports JPG, PNG, and more", color = TextGrey)
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = onClearClick) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear")
                        }
                    } else {
                        Text("Click to select a file", fontWeight = FontWeight.Bold, color = TextBlack)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Supports JPG, PNG, and more", color = TextGrey)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSelectClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrightBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Select Image", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun ResultsScreen(
    bitmap: Bitmap,
    results: List<DetectionResult>,
    onAnalyzeAnother: () -> Unit
) {
    val isDetectionSuccessful = results.isNotEmpty()
    val averageConfidence = if (isDetectionSuccessful) results.map { it.confidence }.average() * 100 else 0.0
    val decimalFormat = DecimalFormat("0.0'%'")

    val titleIcon = if (isDetectionSuccessful) Icons.Outlined.CheckCircle else Icons.Outlined.Info
    val titleText = if (isDetectionSuccessful) "Detection Complete" else "Analysis Complete"
    val subtitleText = if (isDetectionSuccessful) "AI analysis has identified pedestrians." else "No pedestrians were detected in the image."
    val iconColor = if (isDetectionSuccessful) SuccessGreen else TextGrey

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = titleIcon, contentDescription = "Status", tint = iconColor, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(titleText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextBlack)
            }
            Text(subtitleText, color = TextGrey)
        }

        item {
            ImageWithOverlays(
                bitmap = bitmap,
                results = results,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ResultStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Groups,
                    title = "Pedestrians Detected",
                    value = results.size.toString(),
                    description = "Individuals identified."
                )
                ResultStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Speed,
                    title = "Avg. Confidence",
                    value = if (isDetectionSuccessful) decimalFormat.format(averageConfidence) else "N/A",
                    description = "AI model confidence."
                )
            }
        }

        item {
            if (isDetectionSuccessful) {
                DetectionDetailsCard(results = results, decimalFormat = decimalFormat)
            }
        }

        item {
            Button(
                onClick = onAnalyzeAnother,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrightBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(vertical = 8.dp)
            ) {
                Text("Analyze Another File", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

// --- THIS IS THE UPDATED COMPOSABLE ---
// It now draws numbered labels above each bounding box.
@Composable
fun ImageWithOverlays(bitmap: Bitmap, results: List<DetectionResult>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Analyzed Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Calculate scale factors to fit the image within the canvas while maintaining aspect ratio
                val scaleFactorX = size.width / bitmap.width
                val scaleFactorY = size.height / bitmap.height
                val scaleFactor = minOf(scaleFactorX, scaleFactorY)

                // Calculate offsets to center the scaled image
                val offsetX = (size.width - bitmap.width * scaleFactor) / 2
                val offsetY = (size.height - bitmap.height * scaleFactor) / 2

                // Prepare paints outside the loop for efficiency
                val boxPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#007AFF") // BrightBlue
                    strokeWidth = 5f
                    style = Paint.Style.STROKE
                }

                val textBackgroundPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#99007AFF") // Semi-transparent BrightBlue
                    style = Paint.Style.FILL
                }

                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 35f // You can adjust the text size here
                    textAlign = Paint.Align.LEFT
                }

                drawIntoCanvas { canvas ->
                    // Use forEachIndexed to get the index for numbering
                    results.forEachIndexed { index, result ->
                        val rect = result.boundingBox

                        // Scale the rectangle from original bitmap coordinates to canvas coordinates
                        val scaledRect = RectF(
                            rect.left * scaleFactor + offsetX,
                            rect.top * scaleFactor + offsetY,
                            rect.right * scaleFactor + offsetX,
                            rect.bottom * scaleFactor + offsetY
                        )

                        // 1. Draw the bounding box
                        canvas.nativeCanvas.drawRect(scaledRect, boxPaint)

                        // 2. Prepare and draw the numbered label
                        val label = "Pedestrian ${index + 1}"
                        val textBounds = android.graphics.Rect()
                        textPaint.getTextBounds(label, 0, label.length, textBounds)
                        val textHeight = textBounds.height()

                        // Position the text background just above the bounding box
                        val textBgTop = maxOf(0f, scaledRect.top - textHeight - 8f) // 8f padding from box
                        val textBgBottom = scaledRect.top
                        val textBgLeft = scaledRect.left
                        val textBgRight = scaledRect.left + textBounds.width() + 8f // 8f padding for width

                        // Draw the background rectangle for the text
                        canvas.nativeCanvas.drawRect(textBgLeft, textBgTop, textBgRight, textBgBottom, textBackgroundPaint)

                        // Draw the text on top of its background
                        canvas.nativeCanvas.drawText(
                            label,
                            textBgLeft + 4f,    // 4f horizontal padding
                            textBgBottom - 4f,  // 4f vertical padding
                            textPaint
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultStatCard(modifier: Modifier = Modifier, icon: ImageVector, title: String, value: String, description: String) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(LightBlueBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = BrightBlue)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextBlack)
            Text(title, color = TextGrey, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextGrey)
        }
    }
}

@Composable
fun DetectionDetailsCard(results: List<DetectionResult>, decimalFormat: DecimalFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Detection Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextBlack)
            Spacer(modifier = Modifier.height(8.dp))
            results.forEachIndexed { index, result ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pedestrian ${index + 1}", color = TextGrey)
                    Text(
                        text = decimalFormat.format(result.confidence * 100),
                        color = SuccessGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (index < results.size - 1) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}